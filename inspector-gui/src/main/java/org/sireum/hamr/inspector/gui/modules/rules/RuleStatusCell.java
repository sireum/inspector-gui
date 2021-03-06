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
