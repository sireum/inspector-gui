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

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Tab;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import reactor.util.function.Tuple2;
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

    private final AtomicBoolean isRootLoaded = new AtomicBoolean();

    @ThreadedOn(threadName = "fx")
    private Parent rootNode = null;

    @ThreadedOn(threadName = "fx")
    private AppNode appNode = null;

    /**
     * Lazily called by appNode() or appRoot() beans when initialized.
     */
    private void loadRootIfNull() {
        final boolean isNotLoaded = isRootLoaded.compareAndSet(false, true);
        if (isNotLoaded) {
            final var tuple = loadNodeReturnAll("appnode", AppNode.class);
            rootNode = (Parent) tuple.getT1();
            final var loader = tuple.getT2();
            appNode = loader.getController();
        }
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
    public <T> Node loadNode(@NotNull String viewName, @NotNull Class<T> nodeClass) {
        return loadNodeReturnAll(viewName, nodeClass).getT1();
    }

    @NotNull
    public <T> Tuple3<@NotNull Node, @NotNull FXMLLoader, @NotNull Object> loadNodeReturnAll(@NotNull String viewName, @NotNull Class<T> nodeClass) {
        try {
            final FXMLLoader loader = createLoader(viewName, nodeClass);
            log.debug("loading view {} for class {}", viewName, nodeClass.getSimpleName());
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
            // todo make this instead return special "unable to load" label node instead of throwing error
            throw new RuntimeException("Unable to load view {} fxml.");
        }
    }

    @NotNull
    public <T> Tab loadAndInsertTab(String viewName, Class<T> nodeClass) {
        return loadTabAndInsertTabKeepFxmlLoader(viewName, nodeClass).getT1();
    }

    @NotNull
    public <T> Tuple2<@NotNull Tab, @NotNull FXMLLoader> loadTabAndInsertTabKeepFxmlLoader(String viewName, Class<T> nodeClass) {
        final var tuple = loadNodeReturnAll(viewName, nodeClass);
        final var node = tuple.getT1();
        final var loader = tuple.getT2();
        final var controller = tuple.getT3();

        final Tab tab = getAppNode().addNodeAsNewTab(viewName, node, controller);
        return Tuples.of(tab, loader);
    }

}
