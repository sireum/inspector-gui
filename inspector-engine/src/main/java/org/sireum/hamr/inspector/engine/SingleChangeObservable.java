package org.sireum.hamr.inspector.engine;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * todo put in engine of ArtGui
 * @param <T>
 */
public class SingleChangeObservable<T> {

    private static final Logger log = LoggerFactory.getLogger(SingleChangeObservable.class);

    private final AtomicReference<AtomicState> state =
            new AtomicReference<>(new AtomicState(result -> { }, null));

    // immutable, no compare, hashcode, etc. because private and these functions are never used.
    private final class AtomicState {
        // will have a value until consumed, then will equal null
        @Nullable final Consumer<T> listeners;

        @Nullable final T terminalValue;

        private AtomicState(@Nullable Consumer<T> listeners, @Nullable T terminalValue) {
            if (listeners == null && terminalValue == null) {
                throw new IllegalStateException("AtomicState should never have null values for both fields");
            } else if (listeners != null && terminalValue != null) {
                throw new IllegalStateException("AtomicState should never have non-null values for both fields");
            }
            this.listeners = listeners;
            this.terminalValue = terminalValue;
        }
    }

    @NotNull
    private final T initial;

    @NotNull
    @SuppressWarnings("FieldCanBeLocal")
    private final Disposable disposable;

    private SingleChangeObservable(@NotNull T initial, @NotNull Mono<T> terminal, @Nullable Consumer<T> listener) {
        this.initial = initial;

        if (listener != null) {
            addListener(listener);
        }

        disposable = terminal.subscribe(result -> {
            var lastNonTerminalState = state.getAndUpdate(it -> {
                // set to final state (null consumer chain and result is set)
                return new AtomicState(null, result);
            });

            if (lastNonTerminalState.listeners == null) {
                throw new IllegalStateException("it should be impossible for lastNonTerminalState to have null listeners");
            }

            lastNonTerminalState.listeners.accept(state.get().terminalValue);
        });
    }

    @NotNull
    public T get() {
        return Objects.requireNonNullElse(state.get().terminalValue, initial);
    }

    public void addListener(@NotNull Consumer<T> listener) {

        final CountDownLatch terminalListenerGate = new CountDownLatch(1);

        var prevState = state.getAndUpdate(it -> {
            // if listeners has a value then the state has not changed and the new listener can be added to the chain
            if (it.listeners != null) {

                final var gateEnforcer = (Consumer<T>) result -> {
                    try {
                        final boolean successfulAwait = terminalListenerGate.await(250, TimeUnit.MILLISECONDS);
                        if (!successfulAwait) {
                            log.warn("Consumer was blocked from running on the terminalValue by " +
                                    "its initialValue call. Will retry.");

                            final boolean secondAttempt = terminalListenerGate.await(2, TimeUnit.SECONDS);
                            if (!secondAttempt) {
                                log.error("Consumer was still blocked upon reattempt. " +
                                        "Allowing out-of-order execution (may cause race condition).");
                            } else {
                                log.warn("Consumer block was resolved. Note listeners should be non-blocking.");
                            }
                        }
                    } catch (InterruptedException e) {
                        log.warn("An InterruptedException occurred when blocking to enforce " +
                                "initial callback -> terminal callback ordering of listeners", e);
                    }
                };

                final var newListenerChain = it.listeners.andThen(gateEnforcer).andThen(listener);

                // pass along its terminalValue (just in case) but technically should always be null if listeners exist.
                return new AtomicState(newListenerChain, it.terminalValue);
            }
            // if listeners is null then do nothing because listener will be invoked after the end of this
            // side-effect free updating function
            return it;
        });

        // if prevState's listeners are null then this SingleChangeObservable has already changed, the getAndUpdate()
        // call above was a no-op, and the listener passed to this method can be applied instantly
        if (prevState.listeners == null) {
            final T terminalValue = prevState.terminalValue;
            if (terminalValue == null) {
                throw new IllegalStateException("AtomicState should never have null values for both fields");
            }
            listener.accept(terminalValue);
        } else {
            // if prevState's listeners are NOT null, then apply the listener for currentValue, knowing that
            // it will be again applied when the transition occurs
            //
            // without some ordering, a race condition could occur where:
            //  1. listener is attached
            //  2. the value changes (async) and listeners(terminalValue) begins executing
            //          (most of which is async command queueing)
            //  3. the listener runs, consuming terminalValue
            //  4. the line below runs, consuming initialValue
            //
            //  if the consumer has a side effect then the result of the final value is overridden by the initialValue
            //  and it will appear as if SingleChangeObservable's transition never occurred.
            //
            //  A CountDownLatch is used to manage this. If the call to initial blocks terminal for more than 2.25
            //  seconds then the listener will be called (with the terminalValue), potentially causing the race condition

            final var gateOpener = (Consumer<T>) initialValue -> terminalListenerGate.countDown();
            listener.andThen(gateOpener).accept(initial);
        }
    }

    @NotNull
    public static <T> SingleChangeObservable<T> create(@NotNull T initial, @NotNull Mono<T> terminal) {
        return create(initial, terminal, null);
    }

    @NotNull
    public static <T> SingleChangeObservable<T> create(@NotNull T initial, @NotNull Mono<T> terminal, @Nullable Consumer<T> listener) {
        return new SingleChangeObservable<>(initial, terminal, listener);
    }

}
