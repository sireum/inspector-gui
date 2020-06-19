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

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableLongValue;
import javafx.beans.value.ObservableObjectValue;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.util.Callback;
import javafx.util.StringConverter;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.controlsfx.control.MasterDetailPane;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.monadic.MonadicBinding;
import org.sireum.hamr.inspector.common.Msg;
import org.sireum.hamr.inspector.common.Rule;
import org.sireum.hamr.inspector.engine.RuleProcessorService;
import org.sireum.hamr.inspector.gui.ViewController;
import org.sireum.hamr.inspector.gui.collections.FxCollectors;
import org.sireum.hamr.inspector.gui.components.msc.Msc;
import org.sireum.hamr.inspector.services.RuleStatus;
import org.sireum.hamr.inspector.services.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import reactor.util.function.Tuples;

import java.util.Arrays;
import java.util.List;

@Slf4j
@ViewController
@NoArgsConstructor
@SuppressWarnings("SpringJavaAutowiredFieldsWarningInspection") // <-- protection for generated classes
public class RulesTab {

    @Autowired
    @Qualifier("rules")
    private ObservableList<Rule> rules;

    @Getter
    @Autowired
    @Qualifier("sessions")
    private ObservableList<Session> sessions;

    @Autowired
    private RuleProcessorService ruleProcessorService;

    @FXML
    public ComboBox<Session> sessionComboBox;

    @FXML
    public TableView<SessionRule> rulesView;

    @FXML
    public Text detailText;

    @FXML
    public MasterDetailPane masterDetailPane;

    @FXML
    public VBox detailNode;

    /*
     * JavaFx forces the word "Controller" to follow for an fx:id of a fx:included component.
     * So this fx:id is "mscView" in rules.fxml
     */
    @FXML
    public Msc mscViewController;

    @Getter
    private final Callback<TableView<SessionRule>, TableRow<SessionRule>> rowFactory = tableView -> new TableRow<>();

    // status cell factory
    @Getter
    private final Callback<TableColumn<SessionRule, RuleStatus>, TableCell<SessionRule, RuleStatus>>
            ruleStatusCellFactory = it -> new RuleStatusCell();

    // status cell value factory
    @Getter
    private final Callback<TableColumn.CellDataFeatures<SessionRule, RuleStatus>, ObservableValue<RuleStatus>> ruleStatusCellValueFactory = it -> {
        final SessionRule sessionRule = it.getValue();
        if (sessionRule.getSession() == null) {
            return new SimpleObjectProperty<>(RuleStatus.RUNNING);
        } else {
            return ruleProcessorService.getRuleStatusObservable(Tuples.of(sessionRule.getSession(), sessionRule.getRule()));
        }
    };

    // name cell factory
    @Getter
    private final Callback<TableColumn<SessionRule, String>, TableCell<SessionRule, String>>
            ruleNameCellFactory = it -> new RuleNameCell();

    // name cell value factory
    @Getter
    private final Callback<TableColumn.CellDataFeatures<SessionRule, String>, ObservableValue<String>>
            ruleNameCellValueFactory = it -> new SimpleStringProperty(it.getValue().getRule().name());

    // time cell factory
    @Getter
    private final Callback<TableColumn<SessionRule, Long>, TableCell<SessionRule, Long>>
            ruleTimeCellFactory = it -> new RuleTimeCell();

    // time cell value factory
    @Getter
    private final Callback<TableColumn.CellDataFeatures<SessionRule, Long>, ObservableLongValue> ruleTimeCellValueFactory = it -> {
        final SessionRule sessionRule = it.getValue();
        if (sessionRule.getSession() == null) {
            return new SimpleLongProperty();
        } else {
            return ruleProcessorService.getRuleStopTimeObservable(Tuples.of(sessionRule.getSession(), sessionRule.getRule()));
        }
    };

    @Getter
    private StringConverter<Session> sessionStringConverter = new StringConverter<>() {
        @Override
        public String toString(Session session) {
            return session.getName();
        }

        @Override
        public Session fromString(String string) {
            return sessions.stream().filter(s -> s.getName().equals(string)).findFirst().orElseGet(() -> {
                log.error("unable to find Session matching string from dropDownBox");
                return null;
            });
        }
    };

    @FXML
    protected void initialize() {
        detailText.wrappingWidthProperty().bind(Bindings.max(0, Bindings.subtract(detailNode.widthProperty(), 10)));

        final var ruleStatus = EasyBind.map(rulesView.getSelectionModel().selectedItemProperty(), sessionRule -> {
            if (sessionRule != null && sessionRule.getSession() != null) {
                final var sessionRuleTuple = Tuples.of(sessionRule.getSession(), sessionRule.getRule());
                final var lastMsg = ruleProcessorService.getRuleLastMsgObservable(sessionRuleTuple);
                final var cause = ruleProcessorService.getErrorCause(sessionRuleTuple);

                return Tuples.of(lastMsg, cause);
            }
            return Tuples.of(new SimpleObjectProperty<List<Msg>>(null), new SimpleObjectProperty<Throwable>(null));
        });

        final MonadicBinding<ObservableList<Msg>> lastMsgList = EasyBind.map(ruleStatus, tuple -> {
            final ObservableObjectValue<List<Msg>> lastMsgs = tuple.getT1();
            final ObservableObjectValue<Throwable> throwable = tuple.getT2();

            // todo find less hacky way to update this property
            detailText.textProperty().unbind();
            if (throwable.get() != null) {
                detailText.textProperty().bind(Bindings.when(Bindings.isNotNull(throwable))
                        .then(throwable.get().toString() + "\n\n\n" +
                                Arrays.toString(throwable.get().getStackTrace()) + "\n\n\n" +
                                Arrays.toString(throwable.get().getSuppressed()))
                        .otherwise(""));
            } else {
                detailText.setText("");
            }

            if (lastMsgs.get() != null) {
                return FXCollections.observableList(lastMsgs.get());
            } else {
                return FXCollections.emptyObservableList();
            }
        });

        mscViewController.itemsProperty().bind(lastMsgList);
    }

    /*
     * Occurs whenever the value of sessionChangeAction is changed.
     * Will not occur if the user re-selects the current selection.
     */
    @FXML
    private void sessionChangeAction() {
        final var sessionRules = rules.stream()
                .map(rule -> SessionRule.of(sessionComboBox.getValue(), rule))
                .collect(FxCollectors.toObservableList());
        rulesView.setItems(sessionRules);
        rulesView.refresh();
    }
}
