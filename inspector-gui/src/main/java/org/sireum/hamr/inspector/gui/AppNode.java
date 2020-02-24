package org.sireum.hamr.inspector.gui;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.NumberBinding;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TabPane;
import lombok.NoArgsConstructor;
import org.controlsfx.control.TaskProgressView;
import org.controlsfx.control.action.Action;
import org.controlsfx.control.action.ActionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

@ViewController
@NoArgsConstructor @SuppressWarnings("SpringJavaAutowiredFieldsWarningInspection") // <-- protection for generated classes
public class AppNode {

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
    public TabPane tabPane;

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

    @FXML
    protected void initialize() {
        ActionUtils.configureMenuItem(refreshSessionsAction, refreshSessionsMenuItem);
        ActionUtils.configureMenuItem(consoleTabAction, newTableTabMenuItem);
        ActionUtils.configureMenuItem(mscTabAction, newMscTabMenuItem);
        ActionUtils.configureMenuItem(rulesTabAction, newRulesTabMenuItem);
        ActionUtils.configureMenuItem(generateTestReportAction, generateReportMenuItem);
        ActionUtils.configureMenuItem(runInjectionAction, runInjectionMenuItem);
        initTaskProgressView();
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
