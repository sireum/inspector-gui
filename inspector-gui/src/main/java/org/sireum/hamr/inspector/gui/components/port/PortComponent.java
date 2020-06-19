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

package org.sireum.hamr.inspector.gui.components.port;

import art.DataContent;
import art.PortMode;
import art.UPort;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sireum.Z;
import org.sireum.hamr.inspector.common.ArtUtils;
import org.sireum.hamr.inspector.gui.ViewController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

@Slf4j
@ViewController
@NoArgsConstructor
@SuppressWarnings("SpringJavaAutowiredFieldsWarningInspection") // <-- protection for generated classes
public class PortComponent {

    @Autowired
    @Qualifier("artUtils")
    private ArtUtils artUtils;

    @FXML
    public Circle circle;

//    @FXML
//    public TextFlow dataTextFlow;

    private final Tooltip tooltip = new Tooltip();

    private ObjectProperty<UPort> port = new SimpleObjectProperty<>();
    private ObjectProperty<DataContent> dataContent = new SimpleObjectProperty<>();
    private BooleanProperty isLocalPort = new SimpleBooleanProperty();

    public UPort getPort() {
        return port.get();
    }

    public ObjectProperty<UPort> portProperty() {
        return port;
    }

    public void setPort(UPort port) {
        this.port.set(port);
    }

    public DataContent getDataContent() {
        return dataContent.get();
    }

    public ObjectProperty<DataContent> dataContentProperty() {
        return dataContent;
    }

    public void setDataContent(DataContent dataContent) {
        this.dataContent.set(dataContent);
    }

    public boolean isLocalPort() {
        return isLocalPort.get();
    }

    public BooleanProperty isLocalPortProperty() {
        return isLocalPort;
    }

    public void setIsLocalPort(boolean isLocalPort) {
        this.isLocalPort.set(isLocalPort);
    }

    public void mouseClickAction(MouseEvent mouseEvent) {
        log.info("Port Component clicked");
    }

    public void mouseEnteredAction(MouseEvent mouseEvent) {

    }

    private static final Z dataIn = PortMode.DataIn$.MODULE$.ordinal();
    private static final Z dataOut = PortMode.DataOut$.MODULE$.ordinal();
    private static final Z eventIn = PortMode.EventIn$.MODULE$.ordinal();
    private static final Z eventOut = PortMode.EventOut$.MODULE$.ordinal();

    @FXML
    protected void initialize() {
        initTooltip();
        initColorBinding();

//       final var db = Bindings.createObjectBinding(() -> {
//            return TextFlowFactory.createDefaultParenMatchingTextFlow(dataContent.toString());
//        }, dataContent);


    }

    private void initTooltip() {
        final var sb = Bindings.createStringBinding(() -> {
            if (getPort() != null) {
                Tooltip.install(circle, tooltip);
                if (getDataContent() != null) {
                    return artUtils.informativePrettyPrint(getPort()) + " [ " + getPort().id().toString() + " ] \n\n" + getDataContent().toString();
                } else {
                    return artUtils.informativePrettyPrint(getPort()) + " [ " + getPort().id().toString() + " ] ";
                }
            }

            Tooltip.uninstall(circle, tooltip);
            return "";
        }, port, dataContent);

        tooltip.textProperty().bind(sb);
        tooltip.setShowDelay(Duration.ZERO);
        tooltip.setHideDelay(Duration.ZERO);
        tooltip.setWrapText(true);
        tooltip.setShowDuration(Duration.INDEFINITE);
    }

    private static final Color LOCAL_EVENT_IN_BG_COLOR = Color.PALEVIOLETRED.desaturate();
    private static final Color LOCAL_DATA_IN_BG_COLOR = Color.PALEGREEN.desaturate();
    private static final Color LOCAL_EVENT_OUT_BG_COLOR = Color.PALEVIOLETRED.desaturate();
    private static final Color LOCAL_DATA_OUT_BG_COLOR = Color.PALEGREEN.desaturate();

    private static final Color GLOBAL_EVENT_IN_BG_COLOR = LOCAL_EVENT_IN_BG_COLOR;
    private static final Color GLOBAL_DATA_IN_BG_COLOR = LOCAL_DATA_IN_BG_COLOR;
    private static final Color GLOBAL_EVENT_OUT_BG_COLOR = LOCAL_EVENT_OUT_BG_COLOR;
    private static final Color GLOBAL_DATA_OUT_BG_COLOR = LOCAL_DATA_OUT_BG_COLOR;

    private void initColorBinding() {
        final var ob = Bindings.createObjectBinding(() -> {
            if (getPort() != null) {
                final var portModeOrdinal = getPort().mode().ordinal();

                if (portModeOrdinal.equals(dataIn)) {
                    return isLocalPort.get() ? LOCAL_DATA_IN_BG_COLOR : GLOBAL_DATA_IN_BG_COLOR;
                } else if (portModeOrdinal.equals(eventIn)) {
                    return isLocalPort.get() ? LOCAL_EVENT_IN_BG_COLOR : GLOBAL_EVENT_IN_BG_COLOR;
                } else if (portModeOrdinal.equals(dataOut)) {
                    return isLocalPort.get() ? LOCAL_DATA_OUT_BG_COLOR : GLOBAL_DATA_OUT_BG_COLOR;
                } else if (portModeOrdinal.equals(eventOut)) {
                    return isLocalPort.get() ? LOCAL_EVENT_OUT_BG_COLOR : GLOBAL_EVENT_OUT_BG_COLOR;
                } else {
                    log.error("Unable to determine port type in port color property callback.");
                }
            }
            return Color.TRANSPARENT;
        }, port, isLocalPort);

        circle.fillProperty().bind(ob);
    }

}
