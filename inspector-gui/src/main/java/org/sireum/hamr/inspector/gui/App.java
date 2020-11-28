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

package org.sireum.hamr.inspector.gui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sireum.hamr.inspector.common.Filter;
import org.sireum.hamr.inspector.common.Injection;
import org.sireum.hamr.inspector.common.InspectionBlueprint;
import org.sireum.hamr.inspector.common.Rule;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Objects;
import java.util.Set;

@Slf4j
@NoArgsConstructor
public class App extends Application {

    public static final float COLOR_SCHEME_HUE_OFFSET = 0.05f;
    public static final float COLOR_SCHEME_SATURATION = 0.85f;
    public static final float COLOR_SCHEME_BRIGHTNESS = 0.65f;

    // set by InspectorApplication before init()
    static volatile InspectionBlueprint inspectionBlueprint = null;
    static volatile Set<Filter> filters = null;
    static volatile Set<Rule> rules = null;
    static volatile Set<Injection> injections = null;
    static volatile String[] args = null;

    // null during init, then has value on start
    private volatile ConfigurableApplicationContext applicationContext;

    @Override
    public void init() throws Exception {
        Objects.requireNonNull(inspectionBlueprint, "inspectionBlueprint must be set before launching");
        Objects.requireNonNull(filters, "filtersSeq must be set before launching");
        Objects.requireNonNull(rules, "rulesSeq must be set before launching");
        Objects.requireNonNull(injections, "injectionsSeq must be set before launching");
        Objects.requireNonNull(args, "Args must be set before launching");
        log.info("Initializing spring context...");
        log.info("javafx.runtime.version: {}", System.getProperties().get("javafx.runtime.version"));

        final var configurationClasses = new Class<?>[] { AppDiscovery.class };
        applicationContext = new SpringApplicationBuilder(configurationClasses)
                .headless(false)
                .web(WebApplicationType.NONE)
                .registerShutdownHook(true)
                .run(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        log.info("Starting app...");

        final var appLoader = applicationContext.getBean(AppLoader.class);
        final var root = appLoader.getRootNode();
        final var scene = new Scene(root);

        primaryStage.setTitle("Inspector Gui");
        primaryStage.setScene(scene);
        primaryStage.setWidth(1080);
        primaryStage.setHeight(720);
        primaryStage.centerOnScreen();
        primaryStage.show();
    }

    @Override
    public void stop() throws Exception {
        log.info("Stopping app...");
        applicationContext.close();
        Platform.exit();
    }
}