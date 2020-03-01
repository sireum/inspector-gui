package org.sireum.hamr.inspector.engine;

import art.Bridge;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.sireum.hamr.inspector.common.ArtUtils;
import org.sireum.hamr.inspector.gui.ThreadedOn;
import org.sireum.hamr.inspector.gui.gfx.Coloring;
import org.sireum.hamr.inspector.services.Session;
import org.sireum.hamr.inspector.services.SessionService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.Disposable;

import javax.annotation.PostConstruct;

import static javafx.collections.FXCollections.observableSet;
import static javafx.collections.FXCollections.unmodifiableObservableList;
import static org.sireum.hamr.inspector.gui.App.*;

/**
 * Beans which are managed behind the scenes by Services.
 */
@Slf4j
@Configuration
public class ServiceBeans {

    private final SessionService sessionService;
    private final ArtUtils artUtils;

    @ThreadedOn(threadName = "fx")
    private final ObservableSet<Session> sessionsSet = observableSet();

    @ThreadedOn(threadName = "fx")
    private final ObservableList<Session> backingSessionsList = FXCollections.observableArrayList();

    private final SetChangeListener<Session> sessionSetChangeListener = change -> {
        if (change.wasAdded()) {
            backingSessionsList.add(change.getElementAdded());
        }
        if (change.wasRemoved()) {
            backingSessionsList.remove(change.getElementRemoved());
        }
    };

    @SuppressWarnings("FieldCanBeLocal")
    private Disposable subscribe = null;

    @PostConstruct
    private void postConstruct() {
        sessionsSet.addListener(sessionSetChangeListener);

        subscribe = sessionService.sessions()
                .collectList()
                .doOnNext(list -> Platform.runLater(() -> sessionsSet.addAll(list)))
                .thenMany(sessionService.liveStatusUpdates().doOnNext(flux -> Platform.runLater(() -> sessionsSet.add(flux.key()))))
                .subscribe();
    }

    public ServiceBeans(SessionService sessionService, ArtUtils artUtils) {
        this.sessionService = sessionService;
        this.artUtils = artUtils;
    }

    @Bean(name = "sessions")
    @ThreadedOn(threadName = "fx")
    public ObservableList<Session> sessions() {
        return unmodifiableObservableList(backingSessionsList).sorted();
    }

    @Bean(name = "sessionNames")
    @ThreadedOn(threadName = "fx")
    public ObservableList<String> sessionNames(@Qualifier("sessions") ObservableList<Session> sessions) {
        return EasyBind.map(sessions, Session::getName);
    }

    @Bean(name = "bridgeColoring")
    public Coloring<Bridge> bridgeColoring() {
        return Coloring.ofUniformlyDistantColors(
                artUtils.getBridges(),
                COLOR_SCHEME_HUE_OFFSET,
                COLOR_SCHEME_SATURATION,
                COLOR_SCHEME_BRIGHTNESS);
    }

    public void refreshSessionsList() {
//        sessionService.sessions().subscribe(session -> Platform.runLater(() -> sessionsSet.add(session)));

        sessionService.sessions()
                .collectList()
                .doOnNext(list -> Platform.runLater(() -> {
                    sessionsSet.clear();
                    sessionsSet.addAll(list);
                }))
                .subscribe();
    }

}
