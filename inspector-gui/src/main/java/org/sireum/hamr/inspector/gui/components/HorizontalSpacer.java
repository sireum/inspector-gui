package org.sireum.hamr.inspector.gui.components;

import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;

public class HorizontalSpacer extends Pane {

    public HorizontalSpacer() {
        HBox.setHgrow(this, Priority.SOMETIMES);
    }

}
