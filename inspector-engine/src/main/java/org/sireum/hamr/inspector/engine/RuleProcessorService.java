package org.sireum.hamr.inspector.engine;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import javafx.application.Platform;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableLongValue;
import javafx.beans.value.ObservableObjectValue;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.sireum.hamr.inspector.common.ArtUtils;
import org.sireum.hamr.inspector.common.Msg;
import org.sireum.hamr.inspector.common.Rule;
import org.sireum.hamr.inspector.gui.ThreadedOn;
import org.sireum.hamr.inspector.services.MsgService;
import org.sireum.hamr.inspector.services.RuleStatus;
import org.sireum.hamr.inspector.services.Session;
import org.springframework.stereotype.Controller;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Signal;
import reactor.util.function.Tuple2;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

@Slf4j
@Controller
public class RuleProcessorService {

    private final MsgService msgService;
    private final ArtUtils artUtils;

    public RuleProcessorService(MsgService msgService, ArtUtils artUtils) {
        this.msgService = msgService;
        this.artUtils = artUtils;
    }

    private LoadingCache<Tuple2<Session, Rule>, ObservableObjectValue<RuleStatus>> CACHE;

    @ThreadedOn(threadName = "fx")
    private TimeCache TIME_CACHE = new TimeCache();
    private LastMsgCache LAST_MSG_CACHE = new LastMsgCache();
    private ThrowableCache ERROR_CACHE = new ThrowableCache();

    @PostConstruct
    private void postConstruct() {
        CACHE = Caffeine.newBuilder().build((Tuple2<Session, Rule> sessionRule) -> {
            final Session session = sessionRule.getT1();
            final Rule rule = sessionRule.getT2();

            final Function<Flux<? extends Signal<?>>, Mono<RuleStatus>> HANDLE_LAST_SIGNAL =
                    (Flux<? extends Signal<?>> flux) -> flux
                            .takeLast(1)
                            .single()
                            .flatMap(signal -> {
                                switch (signal.getType()) {
                                    case ON_COMPLETE:
                                        return Mono.just(RuleStatus.SUCCESS);
                                    case ON_ERROR:
                                        ERROR_CACHE.setIfAbsent(sessionRule, Objects.requireNonNull(signal.getThrowable()));
                                        return Mono.just(RuleStatus.FAILURE);
                                    default:
                                        return Mono.error(new IllegalStateException("expected last signal complete or failure"));
                                }
                            });

            final Mono<RuleStatus> resultMono = msgService.replayThenLive(session)
                    .publish(msgFlux -> {
                        final AtomicInteger x = new AtomicInteger(0);
                        final AtomicInteger y = new AtomicInteger(0);
                        final Mono<RuleStatus> ruleStatusMono = msgFlux
                                .doOnNext(it -> x.incrementAndGet())
                                .transformDeferred(flux -> rule.rule(org.sireum.hamr.inspector.stream.Flux.from(flux), artUtils))
                                .materialize()
                                .transform(HANDLE_LAST_SIGNAL)
                                .takeLast(1)
                                .single();

                        // the publish method forces both fluxes to run in lockstep todo fix
                        final Mono<List<Msg>> lastMsgMono = msgFlux
                                .doOnNext(it -> y.incrementAndGet())
                                .window(10, 1)
                                .takeUntilOther(ruleStatusMono)
                                .takeLast(1)
                                .single()
                                .flatMap(Flux::collectList);

                        var combine = Flux.zip(ruleStatusMono, lastMsgMono);

                        if (x.get() != y.get()) {
                            log.error("evaluated msg counts {} != {} when evaluating two co-publishing fluxes", x.get(), y.get());
                        }

                        return combine;

                    })
                    .single()
                    .doOnNext(t -> Platform.runLater(() -> {
                        final List<Msg> msgs = t.getT2();
                        LAST_MSG_CACHE.setCachedTime(sessionRule, msgs);
                        if (!msgs.isEmpty()) {
                            TIME_CACHE.setCachedTime(sessionRule, msgs.get(msgs.size() - 1).timestamp());
                        }
                    }))
                    .map(Tuple2::getT1);

            /*
             * Local variable is necessary because compiler cannot find symbol resultMono if directly returned.
             */
            @SuppressWarnings("UnnecessaryLocalVariable")
            final var resultProperty = new SimpleObjectProperty<>(RuleStatus.RUNNING) {
                @SuppressWarnings("unused")
                final Disposable d = resultMono.subscribe(result -> {
                    log.info("rule {} session {} completed with status {}, invoking propertyUpdate later...",
                            rule.name(), session.getName(), result);
                    Platform.runLater(() -> {
                        log.info("javafx property has been updated to reflect rule {} session {} status of {}",
                                rule.name(), session.getName(), result);
                        this.setValue(result);
                    });
                });
            };

            return resultProperty;
        });
    }

    @NotNull
    public ObservableObjectValue<RuleStatus> getRuleStatusObservable(@NotNull Tuple2<Session, Rule> sessionRule) {
        return Objects.requireNonNull(CACHE.get(sessionRule));
    }

    /**
     *
     * @param sessionRule the session and rule which should be applied to find the stop time
     * @return An {@link ObservableLongValue} initially containing null and then updated to match the time
     */
    @NotNull
    public ObservableLongValue getRuleStopTimeObservable(@NotNull Tuple2<Session, Rule> sessionRule) {
        return TIME_CACHE.getCachedTime(sessionRule);
    }

    @NotNull
    public ObservableObjectValue<List<Msg>> getRuleLastMsgObservable(@NotNull Tuple2<Session, Rule> sessionRule) {
        return LAST_MSG_CACHE.getCachedLastMsg(sessionRule);
    }

    @NotNull
    public ObservableObjectValue<Throwable> getErrorCause(@NotNull Tuple2<Session, Rule> sessionRule) {
        return ERROR_CACHE.get(sessionRule);
    }

    // todo generify caches
    static class TimeCache {
        private Cache<Tuple2<Session, Rule>, SimpleLongProperty> TIME_CACHE = Caffeine.newBuilder().build();

        @NotNull
        synchronized ObservableLongValue getCachedTime(Tuple2<Session, Rule> tuple) {
            final ObservableLongValue time = TIME_CACHE.getIfPresent(tuple);

            if (time == null) {
                final SimpleLongProperty nullTime = new SimpleLongProperty();
                TIME_CACHE.put(tuple, nullTime);
                return nullTime;
            }

            return time;
        }

        synchronized void setCachedTime(Tuple2<Session, Rule> tuple, long time) {
            final SimpleLongProperty timeObs = TIME_CACHE.getIfPresent(tuple);

            if (timeObs != null) {
                timeObs.set(time);
            } else {
                TIME_CACHE.put(tuple, new SimpleLongProperty(time));
            }
        }
    }

    static class LastMsgCache {
        private Cache<Tuple2<Session, Rule>, SimpleObjectProperty<List<Msg>>> LAST_MSG_CACHE = Caffeine.newBuilder().build();

        @NotNull
        synchronized ObservableObjectValue<List<Msg>> getCachedLastMsg(Tuple2<Session, Rule> tuple) {
            final ObservableObjectValue<List<Msg>> msg = LAST_MSG_CACHE.getIfPresent(tuple);

            if (msg == null) {
                final SimpleObjectProperty<List<Msg>> nullMsg = new SimpleObjectProperty<>();
                LAST_MSG_CACHE.put(tuple, nullMsg);
                return nullMsg;
            }

            return msg;
        }

        synchronized void setCachedTime(Tuple2<Session, Rule> tuple, List<Msg> msg) {
            final SimpleObjectProperty<List<Msg>> msgObs = LAST_MSG_CACHE.getIfPresent(tuple);

            if (msgObs != null) {
                msgObs.set(msg);
            } else {
                LAST_MSG_CACHE.put(tuple, new SimpleObjectProperty<>(msg));
            }
        }
    }

    static class ThrowableCache {
        private Cache<Tuple2<Session, Rule>, SimpleObjectProperty<Throwable>> THROWABLE_CACHE = Caffeine.newBuilder().build();

        @NotNull
        synchronized ObservableObjectValue<Throwable> get(Tuple2<Session, Rule> tuple) {
            final ObservableObjectValue<Throwable> throwable = THROWABLE_CACHE.getIfPresent(tuple);

            if (throwable == null) {
                final SimpleObjectProperty<Throwable> nullThrowable = new SimpleObjectProperty<>();
                THROWABLE_CACHE.put(tuple, nullThrowable);
                return nullThrowable;
            }

            return throwable;
        }

        synchronized void setCachedItem(Tuple2<Session, Rule> tuple, Throwable throwable) {
            final SimpleObjectProperty<Throwable> throwableObs = THROWABLE_CACHE.getIfPresent(tuple);

            if (throwableObs != null) {
                throwableObs.set(throwable);
            } else {
                THROWABLE_CACHE.put(tuple, new SimpleObjectProperty<>(throwable));
            }
        }

        synchronized void setIfAbsent(Tuple2<Session, Rule> tuple, Throwable throwable) {
            final SimpleObjectProperty<Throwable> throwableObs = THROWABLE_CACHE.getIfPresent(tuple);

            if (throwableObs == null) {
                THROWABLE_CACHE.put(tuple, new SimpleObjectProperty<>(throwable));
            }
        }
    }
}
