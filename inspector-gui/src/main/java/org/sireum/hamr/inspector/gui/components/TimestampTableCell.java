package org.sireum.hamr.inspector.gui.components;

import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import org.sireum.hamr.inspector.common.Msg;
import org.sireum.hamr.inspector.gui.App;

/**
 * A cell which visualizes a Timestamp.
 */
public class TimestampTableCell extends TableCell<Msg, Long> {

    @Override
    protected void updateItem(Long item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setGraphic(null);
        } else {
            setGraphic(createTimestampLabel(item));
        }
    }

    /**
     * Override 3 protected cell methods to expose package access
     */
    @Override
    protected void layoutChildren() {
        super.layoutChildren();
    }

    @Override
    protected boolean isItemChanged(Long oldItem, Long newItem) {
        return super.isItemChanged(oldItem, newItem);
    }

    @Override
    protected Boolean getInitialFocusTraversable() {
        return super.getInitialFocusTraversable();
    }

    private static Label createTimestampLabel(Long timestamp) {
        return new Label(App.getArtUtils().formatTime(timestamp));
    }
}
