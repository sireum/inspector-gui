/*
 * Copyright (c) 2020, Matthew Weis, Kansas State University
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

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
        sessionService.sessions()
                .collectList()
                .doOnNext(list -> Platform.runLater(() -> {
                    sessionsSet.clear();
                    sessionsSet.addAll(list);
                }))
                .subscribe();
    }

}
