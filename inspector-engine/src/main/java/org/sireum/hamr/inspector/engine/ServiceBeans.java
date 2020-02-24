package org.sireum.hamr.inspector.engine;

import com.sun.glass.ui.Application;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.sireum.hamr.inspector.gui.ThreadedOn;
import org.sireum.hamr.inspector.services.Session;
import org.sireum.hamr.inspector.services.SessionService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.Disposable;

import javax.annotation.PostConstruct;

import static javafx.collections.FXCollections.*;

/**
 * Beans which are managed behind the scenes by Services.
 */
@Slf4j
@Configuration
public class ServiceBeans {

    private final SessionService sessionService;

    @ThreadedOn(threadName = "fx")
    private final ObservableSet<Session> sessionsSet = observableSet();

    @SuppressWarnings("FieldCanBeLocal")
    private Disposable subscribe = null;

    @PostConstruct
    private void postConstruct() {
        subscribe = sessionService.sessions()
                .doOnNext(session -> Application.invokeLater(() -> sessionsSet.add(session)))
                .thenMany(sessionService.liveStatusUpdates()
                        .doOnNext(flux -> Application.invokeLater(() -> sessionsSet.add(flux.key()))))
                .subscribe();
    }

    public ServiceBeans(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Bean(name = "sessions")
    @ThreadedOn(threadName = "fx")
    public ObservableList<Session> sessions() {
        return unmodifiableObservableList(observableArrayList(sessionsSet)).sorted();
    }

    public void refreshSessionsList() {
        sessionService.sessions().subscribe(session -> Application.invokeLater(() -> sessionsSet.add(session)));
    }

    @Bean(name = "sessionNames")
    @ThreadedOn(threadName = "fx")
    public ObservableList<String> sessionNames(@Qualifier("sessions") ObservableList<Session> sessions) {
        return EasyBind.map(sessions, Session::getName);
    }
}
