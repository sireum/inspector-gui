package org.sireum.hamr.inspector.gui.modules.rules;

import javafx.scene.control.TableCell;
import javafx.scene.text.TextAlignment;
import lombok.extern.slf4j.Slf4j;
import org.sireum.hamr.inspector.common.ArtUtils;

@Slf4j
class RuleTimeCell extends TableCell<SessionRule, Long> {

    RuleTimeCell() {
        setTextAlignment(TextAlignment.CENTER);
    }

    @Override
    protected void updateItem(Long item, boolean empty) {
        super.updateItem(item, empty);

        if (item == null) {
            setText(null);
        } else {
            setText(ArtUtils.formatTime(item));
        }
    }
}
