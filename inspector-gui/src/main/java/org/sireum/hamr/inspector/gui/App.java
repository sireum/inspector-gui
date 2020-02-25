package org.sireum.hamr.inspector.gui;

import art.Bridge;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sireum.hamr.inspector.common.ArchDiscovery;
import org.sireum.hamr.inspector.common.ArtUtils;
import org.sireum.hamr.inspector.engine.ServiceBeans;
import org.sireum.hamr.inspector.gui.gfx.Coloring;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@Slf4j
@NoArgsConstructor
@SpringBootApplication
// can filter out AppDiscovery and ServiceBeans because they are imported by AppActions
@ComponentScan(basePackages = { "org.sireum.hamr.inspector" },
        excludeFilters = { @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = { AppDiscovery.class, ServiceBeans.class}) })
public class App extends Application {

    public static final float COLOR_SCHEME_HUE_OFFSET = 0.05f;
    public static final float COLOR_SCHEME_SATURATION = 0.85f;
    public static final float COLOR_SCHEME_BRIGHTNESS = 0.65f;

    private ConfigurableApplicationContext applicationContext;

    @Getter
    private static ArtUtils artUtils = null;

    @Getter
    private static Coloring<Bridge> bridgeColoring = null;

    @Override
    public void init() throws Exception {
        log.info("Initializing spring context...");
        log.info("javafx.runtime.version: {}", System.getProperties().get("javafx.runtime.version"));
//        applicationContext = SpringApplication.run(AppDiscovery.class);
        applicationContext = SpringApplication.run(App.class);
        applicationContext.registerShutdownHook();

        final var arch = applicationContext.getBean(ArchDiscovery.class);
        artUtils = new ArtUtils(arch);
        bridgeColoring = Coloring.ofUniformlyDistantColors(
                artUtils.getBridges(),
                COLOR_SCHEME_HUE_OFFSET,
                COLOR_SCHEME_SATURATION,
                COLOR_SCHEME_BRIGHTNESS
        );
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