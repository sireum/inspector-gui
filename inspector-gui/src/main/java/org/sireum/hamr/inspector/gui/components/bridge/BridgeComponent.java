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

package org.sireum.hamr.inspector.gui.components.bridge;

import art.Bridge;
import art.PortMode;
import art.UPort;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sireum.Z;
import org.sireum.hamr.inspector.common.ArtUtils;
import org.sireum.hamr.inspector.gui.AppLoader;
import org.sireum.hamr.inspector.gui.ViewController;
import org.sireum.hamr.inspector.gui.components.port.PortComponent;
import org.sireum.hamr.inspector.gui.gfx.Coloring;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@ViewController
@NoArgsConstructor
@SuppressWarnings("SpringJavaAutowiredFieldsWarningInspection") // <-- protection for generated classes
public class BridgeComponent {

    @Getter
    @Autowired
    private AppLoader appLoader;

    @Getter
    @Autowired
    private ArtUtils artUtils;

    @Autowired
    @Qualifier("bridgeColoring")
    private Coloring<Bridge> bridgeColoring;

    @Getter
    private List<PortComponent> portComponentList = new ArrayList<>();

    @FXML
    public VBox globalIn;

    @FXML
    public VBox globalOut;

    @FXML
    public VBox localIn;

    @FXML
    public VBox localOut;

    @FXML
    public Label bridgeName;

    @FXML
    public BorderPane root;

    private ObjectProperty<Bridge> bridge = new SimpleObjectProperty<>();

    @Getter
    private Border outermostBorder = new Border(new BorderStroke(
            Color.BLACK,
            BorderStrokeStyle.SOLID,
            CornerRadii.EMPTY,
            BorderWidths.DEFAULT));
//            new BorderWidths(2.5, 2.5, 2.5, 2.5, true, true, true, true)));

    @Getter
    private Border innerBorder = new Border(new BorderStroke(
            Color.BLACK,
            BorderStrokeStyle.DASHED,
            CornerRadii.EMPTY,
            BorderWidths.DEFAULT));
//            new BorderWidths(1.5, 1.5, 1.5, 1.5, true, true, true, true)));

    public Bridge getBridge() {
        return bridge.get();
    }

    public ObjectProperty<Bridge> bridgeProperty() {
        return bridge;
    }

    public void setBridge(Bridge bridge) {
        this.bridge.set(bridge);
    }

    private static final Z dataIn = PortMode.DataIn$.MODULE$.ordinal();
    private static final Z dataOut = PortMode.DataOut$.MODULE$.ordinal();
    private static final Z eventIn = PortMode.EventIn$.MODULE$.ordinal();
    private static final Z eventOut = PortMode.EventOut$.MODULE$.ordinal();

    @FXML
    protected void initialize() {

        if (PortMode.numOfElements().toInt() != 4) {
            throw new IllegalStateException("BridgeComponent handles only 4 PortModes at the moment.");
        }

        bridgeProperty().addListener((observable, oldValue, newValue) -> {
            clearAll();
            root.setBackground(new Background(new BackgroundFill(
                    bridgeColoring.getColorOf(newValue).deriveColor(0.0, 1.0, 1.0, 0.3), null, null)));
//            root.setBorder();
//            root.setBackground(new Background(new BackgroundFill(bridgeColoring.getColorOf(newValue), null, null)));
            if (newValue != null) {
                bridgeName.setText(artUtils.prettyPrint(newValue));

                for (UPort port : artUtils.getPorts()) {
                    if (artUtils.getBridge(port).equals(newValue)) {
                        final var portModeOrdinal = port.mode().ordinal();

                        final var globalChild = appLoader.loadNodeReturnAll("port", PortComponent.class);
                        final var localChild = appLoader.loadNodeReturnAll("port", PortComponent.class);

                        final PortComponent global = (PortComponent) globalChild.getT3();
                        final PortComponent local = (PortComponent) localChild.getT3();

                        global.setPort(port);
                        global.setIsLocalPort(false);
                        portComponentList.add(global);

                        local.setPort(port);
                        local.setIsLocalPort(true);
                        portComponentList.add(local);

                        if (portModeOrdinal.equals(dataIn) || portModeOrdinal.equals(eventIn)) {
                            translatePortComponentBy(global, -1.0f);
                            translatePortComponentBy(local, -0.5f);
                            globalIn.getChildren().add(globalChild.getT1());
                            localIn.getChildren().add(localChild.getT1());
                        } else if (portModeOrdinal.equals(dataOut) || portModeOrdinal.equals(eventOut)) {
                            translatePortComponentBy(global, 1.0f);
                            translatePortComponentBy(local, 0.5f);
                            globalOut.getChildren().add(globalChild.getT1());
                            localOut.getChildren().add(localChild.getT1());
                        } else {
                            log.error("Unable to determine port type in initialize() method.");
                        }
                    }
                }

                // to ensure empty containers hold the same format, add a hidden port if needed
                addHiddenFillerPortComponent();
            }
        });
    }

    private void addHiddenFillerPortComponent() {
        if (localIn.getChildren().isEmpty()) {
            final var filler = appLoader.loadNodeReturnAll("port", PortComponent.class);
            translatePortComponentBy((PortComponent) filler.getT3(), -0.5f);
            filler.getT1().setVisible(false); // only exists for alignment of empty port holding components
            localIn.getChildren().add(filler.getT1());
        }

        if (localOut.getChildren().isEmpty()) {
            final var filler = appLoader.loadNodeReturnAll("port", PortComponent.class);
            translatePortComponentBy((PortComponent) filler.getT3(), 0.5f);
            filler.getT1().setVisible(false); // only exists for alignment of empty port holding components
            localOut.getChildren().add(filler.getT1());
        }

        if (globalIn.getChildren().isEmpty()) {
            final var filler = appLoader.loadNodeReturnAll("port", PortComponent.class);
            translatePortComponentBy((PortComponent) filler.getT3(), -1.0f);
            filler.getT1().setVisible(false); // only exists for alignment of empty port holding components
            globalIn.getChildren().add(filler.getT1());
        }

        if (globalOut.getChildren().isEmpty()) {
            final var filler = appLoader.loadNodeReturnAll("port", PortComponent.class);
            translatePortComponentBy((PortComponent) filler.getT3(), 1.0f);
            filler.getT1().setVisible(false); // only exists for alignment of empty port holding components
            globalOut.getChildren().add(filler.getT1());
        }
    }

    private void translatePortComponentBy(PortComponent portComponent, double factor) {
        final DoubleBinding translateHalfX = Bindings.multiply(portComponent.circle.radiusProperty(), factor);
        portComponent.circle.translateXProperty().bind(translateHalfX);
    }

    private void clearAll() {
        globalIn.getChildren().clear();
        localIn.getChildren().clear();
        globalOut.getChildren().clear();
        localOut.getChildren().clear();
        bridgeName.setText("");
    }

}
