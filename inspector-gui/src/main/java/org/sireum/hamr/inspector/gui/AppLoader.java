package org.sireum.hamr.inspector.gui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Tab;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.sireum.hamr.inspector.gui.modules.DisposableTabController;
import org.springframework.context.ApplicationContext;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuples;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
public final class AppLoader {

    private final ApplicationContext context;

    public AppLoader(ApplicationContext applicationContext) {
        this.context = applicationContext;
    }

    private AtomicBoolean isRootLoaded = new AtomicBoolean();

    @ThreadedOn(threadName = "fx")
    private Parent rootNode = null;

    @ThreadedOn(threadName = "fx")
    private AppNode appNode = null;

    /**
     * Lazily called by appNode() or appRoot() beans when initialized.
     */
    private void loadRootIfNull() {
        final boolean isLoaded = isRootLoaded.getAcquire();
        if (!isLoaded) {
            final var tuple = loadNodeKeepFxmlLoader("appnode", AppNode.class);
            rootNode = (Parent) tuple.getT1();
            final var loader = tuple.getT2();
            appNode = loader.getController();
        }
        isRootLoaded.setRelease(true);
    }

    @NotNull
    public AppNode getAppNode() {
        loadRootIfNull();
        return appNode;
    }

    @NotNull
    public Parent getRootNode() {
        loadRootIfNull();
        return rootNode;
    }

    /**
     * todo add in method doc that loaders should NOT be cached, because this can cause issues for prototyped tabs
     * @param viewName
     * @param nodeClass
     * @param <T>
     * @return
     */
    @NotNull
    private <T> FXMLLoader createLoader(String viewName, Class<T> nodeClass) {
        final var qualifiedClassName = nodeClass.getName();
        final String packagePath;
        if (qualifiedClassName.contains(".")) {
            packagePath = qualifiedClassName.substring(0, qualifiedClassName.lastIndexOf('.'))
                    .replace('.', '/');
        } else {
            packagePath = "";
        }

        final var qualifiedName = String.format("/%s/%s.fxml", packagePath, viewName);

        final var resource = nodeClass.getResource(qualifiedName);
        final FXMLLoader loader = new FXMLLoader(resource);
        loader.setControllerFactory(context::getBean);
        return loader;
    }

    @NotNull
    private <T> Node loadNode(String viewName, Class<T> nodeClass) {
        return loadNodeKeepFxmlLoader(viewName, nodeClass).getT1();
    }

    @NotNull
    private <T> Tuple3<@NotNull Node, @NotNull FXMLLoader, @NotNull Object> loadNodeKeepFxmlLoader(String viewName, Class<T> nodeClass) {
        try {
            final FXMLLoader loader = createLoader(viewName, nodeClass);
            log.info("loading view {} for class {}", viewName, nodeClass.getSimpleName());
            Objects.requireNonNull(loader);

            final Object[] controller = new Object[1];
            loader.setControllerFactory(cls -> {
                controller[0] = context.getBean(cls);
                return controller[0];
            });

            final Node node = loader.load();

            if (controller[0] == null) {
                throw new IllegalStateException(
                        "AppLoader expected to receive a controller instance when loading fxml. " +
                        "This means either (1) an error occurred in the controller factory, or (2) the controller" +
                        "factory is loading on a separate thread. If (2), replace final array with CountDownLatch.");
            }

            return Tuples.of(node, loader, controller[0]);
        } catch (IOException e) {
            log.error("Unable to load view {} fxml for class {}. {}", viewName, nodeClass.getSimpleName(), e);
            // todo make this instead return special "unable to load" label node
            throw new RuntimeException("Unable to load view {} fxml.");
        }
    }

    @NotNull
    public <T> Tab loadAndInsertTab(String viewName, Class<T> nodeClass) {
        return loadTabAndInsertTabKeepFxmlLoader(viewName, nodeClass).getFirst();
    }

    @NotNull
    public <T> Pair<@NotNull Tab, @NotNull FXMLLoader> loadTabAndInsertTabKeepFxmlLoader(String viewName, Class<T> nodeClass) {
        final var tuple = loadNodeKeepFxmlLoader(viewName, nodeClass);
        final var node = tuple.getT1();
        final var loader = tuple.getT2();
        final var controller = tuple.getT3();

        final Tab tab = new Tab(viewName, node);
        tab.setOnClosed(event -> {
            if (controller instanceof DisposableTabController) {
                log.info("closing resources of disposable tab {}", viewName);
                ((DisposableTabController) controller).dispose();
            }
        });

        getAppNode().tabPane.getTabs().add(tab);
        return Pair.of(tab, loader);
    }

}
