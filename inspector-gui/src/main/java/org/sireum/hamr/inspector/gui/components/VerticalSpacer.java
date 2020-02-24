package org.sireum.hamr.inspector.gui.components;

import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class VerticalSpacer extends Pane {

    public VerticalSpacer() {
        VBox.setVgrow(this, Priority.SOMETIMES);
    }

}
