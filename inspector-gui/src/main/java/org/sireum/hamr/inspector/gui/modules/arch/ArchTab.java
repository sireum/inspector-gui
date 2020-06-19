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

package org.sireum.hamr.inspector.gui.modules.arch;

import art.Bridge;
import art.UConnection;
import art.UPort;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.sireum.hamr.inspector.common.ArtUtils;
import org.sireum.hamr.inspector.common.Filter;
import org.sireum.hamr.inspector.gui.AppLoader;
import org.sireum.hamr.inspector.gui.ViewController;
import org.sireum.hamr.inspector.gui.components.bridge.BridgeComponent;
import org.sireum.hamr.inspector.gui.components.port.PortComponent;
import org.sireum.hamr.inspector.gui.modules.DisposableTabController;
import org.sireum.hamr.inspector.services.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

@Slf4j
@ViewController
@NoArgsConstructor @SuppressWarnings("SpringJavaAutowiredFieldsWarningInspection") // <-- protection for generated classes
public final class ArchTab implements DisposableTabController {

    @Getter
    @Autowired
    @Qualifier("filters")
    private ObservableList<Filter> filters;

    @Getter
    @Autowired
    @Qualifier("sessions")
    private ObservableList<Session> sessions;

    @Autowired
    @Qualifier("artUtils")
    private ArtUtils artUtils;

    @Getter
    @Autowired
    private AppLoader appLoader;

    @FXML
    public Pane content;

    @FXML
    public CheckBox showConnectionsCheckBox;

    @FXML
    private void initialize() {
        final Map<UPort, Circle> outerPortNodeMap = new HashMap<>();
        final Map<UPort, Circle> innerPortNodeMap = new HashMap<>();
        final Map<Bridge, BridgeComponent> bridgeNodeMap = new HashMap<>();

        for (Bridge bridge : artUtils.getBridges()) {
            loadBridge(bridge, outerPortNodeMap, innerPortNodeMap, bridgeNodeMap);
        }

        // lines
        for (UConnection connection : artUtils.getConnections()) {
            final Circle outerFromCircle = outerPortNodeMap.get(connection.from());
            final Circle outerToCircle = outerPortNodeMap.get(connection.to());
            final Circle innerFromCircle = innerPortNodeMap.get(connection.from());
            final Circle innerToCircle = innerPortNodeMap.get(connection.to());

            final BooleanBinding anyHovered = anyHovered(outerFromCircle, outerToCircle, innerFromCircle, innerToCircle);

            final BooleanBinding anyHoveredOrCheckbox = anyHovered.or(showConnectionsCheckBox.selectedProperty());

            createConnectionLine(bridgeNodeMap, outerFromCircle, outerToCircle, connection.from(), connection.to(), anyHoveredOrCheckbox);
            createConnectionLine(bridgeNodeMap, outerFromCircle, innerFromCircle, connection.from(), connection.from(), anyHovered);
            createConnectionLine(bridgeNodeMap, outerToCircle, innerToCircle, connection.to(), connection.to(), anyHovered);
        }
    }

    private static BooleanBinding anyHovered(Circle... circles) {
        // an array of each circle's hoverProperty, but with handling of null circles

        final ReadOnlyBooleanProperty[] bindings = Stream.of(circles).filter(Objects::nonNull)
                .map(Node::hoverProperty).toArray(ReadOnlyBooleanProperty[]::new);

        return Bindings.createBooleanBinding(() -> {
            for (ReadOnlyBooleanProperty binding : bindings) {
                if (binding.get()) {
                    return true;
                }
            }
            return false;
        }, bindings);
    }

    private void createConnectionLine(@NotNull Map<Bridge, BridgeComponent> bridgeNodeMap,
                                      @Nullable Node start, @Nullable Node end,
                                      @NotNull UPort from, @NotNull UPort to,
                                      @NotNull BooleanBinding visibilityProperty) {
        if (start != null && end != null) {
            final var sx = start.translateXProperty();
            final var spx = bridgeNodeMap.get(artUtils.getBridge(from)).root.translateXProperty();

            final var sy = start.translateYProperty();
            final var spy = bridgeNodeMap.get(artUtils.getBridge(from)).root.translateYProperty();

            final var ex = end.translateXProperty();
            final var epx = bridgeNodeMap.get(artUtils.getBridge(to)).root.translateXProperty();

            final var ey = end.translateYProperty();
            final var epy = bridgeNodeMap.get(artUtils.getBridge(to)).root.translateYProperty();

            final var sb = relativeCoords(start);
            final var eb = relativeCoords(end);

            final Line line = new Line(sb.getCenterX(), sb.getCenterY(), eb.getCenterX(), eb.getCenterY());

            line.startXProperty().bind(Bindings.createDoubleBinding(() -> relativeCoords(start).getCenterX(), sx, spx));
            line.startYProperty().bind(Bindings.createDoubleBinding(() -> relativeCoords(start).getCenterY(), sy, spy));
            line.endXProperty().bind(Bindings.createDoubleBinding(() -> relativeCoords(end).getCenterX(), ex, epx));
            line.endYProperty().bind(Bindings.createDoubleBinding(() -> relativeCoords(end).getCenterY(), ey, epy));

            line.visibleProperty().bind(visibilityProperty);

            // disable picking on lines
            line.setMouseTransparent(true);

            content.getChildren().add(line);
        }
    }

    private Bounds relativeCoords(Node node) {
        return content.sceneToLocal(node.localToScene(node.getBoundsInLocal()));
    }

    private void loadBridge(@NotNull Bridge bridge,
                            Map<UPort, Circle> outerPortNodeMap,
                            Map<UPort, Circle> innerPortNodeMap,
                            Map<Bridge, BridgeComponent> bridgeNodeMap) {
        final var comp = appLoader.loadNodeReturnAll("bridge", BridgeComponent.class);

        final var node = ((BridgeComponent)comp.getT3());
        node.setBridge(bridge);

        bridgeNodeMap.put(node.getBridge(), node);
        for (PortComponent portComponent : node.getPortComponentList()) {
            if (portComponent.isLocalPort()) {
                innerPortNodeMap.put(portComponent.getPort(), portComponent.circle);
            } else {
                outerPortNodeMap.put(portComponent.getPort(), portComponent.circle);
            }
        }

        content.getChildren().add(makeNodeDraggable(comp.getT1()));
    }

    @Override
    public void dispose() {

    }

    // drag function "makeNodeDraggable()" and class DragContext based on tutorial:
    // from: https://docs.oracle.com/javase/8/javafx/events-tutorial/draggablepanelsexamplejava.htm

    @Data
    private static final class DragContext {
        double mx, my; // mouse anchor x and y
        double tx, ty; // initial translate x and y positions from when mouse is pressed
    }

    private Node makeNodeDraggable(Node node) {
        final DragContext context = new DragContext();
        context.tx = node.getTranslateX(); // translateX
        context.ty = node.getTranslateY(); // translateY

        final Group draggableGroup = new Group(node);

        draggableGroup.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            context.mx = event.getX();
            context.my = event.getY();
            context.tx = node.getTranslateX();
            context.ty = node.getTranslateY();
        });

        draggableGroup.addEventFilter(MouseEvent.MOUSE_DRAGGED, event -> {
            // translate by initialTranslate + event pos - mouse's anchor point
            node.setTranslateX(context.tx + event.getX() - context.mx);
            node.setTranslateY(context.ty + event.getY() - context.my);
        });

        return draggableGroup;
    }

}
