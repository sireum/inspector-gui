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

import art.Bridge;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.NumberBinding;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.controlsfx.control.TaskProgressView;
import org.controlsfx.control.action.Action;
import org.controlsfx.control.action.ActionUtils;
import org.jetbrains.annotations.NotNull;
import org.sireum.docktabfx.DockablePane;
import org.sireum.hamr.inspector.common.ArtUtils;
import org.sireum.hamr.inspector.gui.gfx.Coloring;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

@Slf4j
@ViewController
@NoArgsConstructor @SuppressWarnings("SpringJavaAutowiredFieldsWarningInspection") // <-- protection for generated classes
public class AppNode {

    @Getter
    @Autowired
    private ArtUtils artUtils;

    @Getter
    @Autowired
    private Coloring<Bridge> bridgeColoring;

    @Autowired
    @Qualifier("archTabAction")
    private Action archTabAction;

    @Autowired
    @Qualifier("consoleTabAction")
    private Action consoleTabAction;

    @Autowired
    @Qualifier("mscTabAction")
    private Action mscTabAction;

    @Autowired
    @Qualifier("rulesTabAction")
    private Action rulesTabAction;

    @Autowired
    @Qualifier("refreshSessionsAction")
    private Action refreshSessionsAction;

    @Autowired
    @Qualifier("generateTestReportAction")
    private Action generateTestReportAction;

    @Autowired
    @Qualifier("runInjectionAction")
    private Action runInjectionAction;

    @FXML
    public DockablePane rootTabPane;

    @FXML
    public MenuItem newArchTabMenuItem;

    @FXML
    public MenuItem newTableTabMenuItem;

    @FXML
    public MenuItem newMscTabMenuItem;

    @FXML
    public MenuItem newRulesTabMenuItem;

    @FXML
    public MenuItem refreshSessionsMenuItem;

    @FXML
    public MenuItem generateReportMenuItem;

    @FXML
    public MenuItem runInjectionMenuItem;

    @FXML
    public TaskProgressView<Task<?>> taskProgressView;

    @NotNull
    public Tab addNodeAsNewTab(@NotNull String viewName, @NotNull Node node, @NotNull Object controller) {
        return rootTabPane.addTab(viewName, node);
    }

    @FXML
    protected void initialize() {
        configureMenuItems();
        initTaskProgressView();
    }

    private void configureMenuItems() {
        ActionUtils.configureMenuItem(refreshSessionsAction, refreshSessionsMenuItem);
        ActionUtils.configureMenuItem(archTabAction, newArchTabMenuItem);
        ActionUtils.configureMenuItem(consoleTabAction, newTableTabMenuItem);
        ActionUtils.configureMenuItem(mscTabAction, newMscTabMenuItem);
        ActionUtils.configureMenuItem(rulesTabAction, newRulesTabMenuItem);
        // todo hide add unstable / "beta" warning?
        ActionUtils.configureMenuItem(generateTestReportAction, generateReportMenuItem);
        ActionUtils.configureMenuItem(runInjectionAction, runInjectionMenuItem);
    }

    private void initTaskProgressView() {
        // bind taskProgressView's visibility to whether or not its taskList is empty
        taskProgressView.visibleProperty().bind(Bindings.isNotEmpty(taskProgressView.getTasks()));

        final NumberBinding zeroIfNoTasks = Bindings.when(taskProgressView.visibleProperty())
                .then(taskProgressView.getPrefHeight())
                .otherwise(0);

        taskProgressView.prefHeightProperty().bind(zeroIfNoTasks);
    }
}
