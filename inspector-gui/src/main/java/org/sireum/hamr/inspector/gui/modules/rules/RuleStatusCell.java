package org.sireum.hamr.inspector.gui.modules.rules;

import javafx.scene.control.TableCell;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.sireum.hamr.inspector.services.RuleStatus;

@Slf4j
class RuleStatusCell extends TableCell<SessionRule, RuleStatus> {

    private static final double GRAPHIC_RADIUS = 14.0;

    @Override
    protected void updateItem(RuleStatus item, boolean empty) {
        super.updateItem(item, empty);

        if (item == null) {
            setGraphic(null);
        } else {
            setGraphic(createGraphic(item));
        }
    }

    @NotNull
    private static Circle createGraphic(@NotNull RuleStatus status) {
        switch (status) {
            case RUNNING: return new Circle(GRAPHIC_RADIUS, Color.GRAY);
            case SUCCESS: return new Circle(GRAPHIC_RADIUS, Color.GREEN);
            case FAILURE: return new Circle(GRAPHIC_RADIUS, Color.RED);
            default: throw new IllegalStateException("RuleCell encountered unhandled RuleStatus enum case");
        }
    }
}
