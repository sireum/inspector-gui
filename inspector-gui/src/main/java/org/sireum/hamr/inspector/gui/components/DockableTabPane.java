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

package org.sireum.hamr.inspector.gui.components;

import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.controlsfx.glyphfont.FontAwesome;
import org.controlsfx.glyphfont.Glyph;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

interface DockableTabPaneApi {
    @NotNull Tab addTab(@NotNull String text, @NotNull Node content);
    void removeTab(@NotNull Tab tab);
    void splitTab(@NotNull Tab tab, @NotNull Orientation orientation);
}

@Slf4j
class DockableTabPaneSkin implements Skin<DockableTabPane>, DockableTabPaneApi {

    @Getter
    private final DockableTabPane control;

    private final StackPane rootNode = new StackPane();

    private TabPane rootTabPane = createTabPane();
    final Tab[] targetTab = new Tab[1];

    private final SplitPane splitPane = new SplitPane();

    public DockableTabPaneSkin(DockableTabPane control) {
        this.control = control;
        rootNode.getChildren().addAll(splitPane);
        splitPane.getItems().add(rootTabPane);
    }

    @Override
    public DockableTabPane getSkinnable() {
        return control;
    }

    @Override
    public Node getNode() {
        return rootNode;
    }

    @Override
    public void dispose() {

    }

    @NotNull
    @Override
    public Tab addTab(@NotNull String name, @NotNull Node content) {
        final Label label = new Label(name);
        final Tab tab = new Tab();

        label.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            targetTab[0] = tab;
        });

        tab.setGraphic(label);
        tab.setContent(content);

        final Glyph columnsImg = new Glyph("FontAwesome", FontAwesome.Glyph.COLUMNS);
        final MenuItem splitVerticallyMenuItem = new MenuItem("Split Vertically", columnsImg);
        splitVerticallyMenuItem.setOnAction(event -> {
            splitTab(tab, Orientation.VERTICAL);
        });

        final Glyph barsImg = new Glyph("FontAwesome", FontAwesome.Glyph.BARS);
        final MenuItem splitHorizontallyMenuItem = new MenuItem("Split Horizontally", barsImg);
        splitHorizontallyMenuItem.setOnAction(event -> {
            splitTab(tab, Orientation.HORIZONTAL);
        });

        final var ctxMenu = new ContextMenu(splitVerticallyMenuItem, splitHorizontallyMenuItem);
        tab.setContextMenu(ctxMenu);

        rootTabPane.getTabs().add(tab); // tabs are always added to the first tabPane

        return tab;
    }

    @Override
    public void removeTab(@NotNull Tab tab) {
        removeRecursive(tab);
    }

    private boolean removeRecursive(@NotNull Tab tab) {
        return removeRecursive(tab, splitPane);
    }

    private static boolean removeRecursive(@NotNull Tab tab, SplitPane container) {
        // container's children are SplitPane or TabPane
        // children that are SplitPane also guarantee this contract
        for (Node node : container.getItems()) {

            if (node instanceof TabPane) {
                final ObservableList<Tab> tabs = ((TabPane) node).getTabs();

                // check if tab exists at this level
                for (Tab potentialTab : tabs) {
                    if (tab == potentialTab) {
                        tabs.remove(tab);
                        return true;
                    }
                }
            } else if (node instanceof SplitPane) {
                // check recursively if tab holds SplitPane
                final SplitPane splitPane = (SplitPane) node;
                final boolean success = removeRecursive(tab, splitPane);

                if (success) {
                    return true;
                }
            }
        }

        return false;
    }

    @Nullable
    private TabPane findUppermostTabPane() {
        return findUppermostTabPane(splitPane);
    }

    @Nullable
    private static TabPane findUppermostTabPane(SplitPane container) {

        // first check all items
        for (Node node : container.getItems()) {
            if (node instanceof TabPane) {
                return (TabPane) node;
            }
        }

        // then go deeper down tree if needed
        for (Node node : container.getItems()) {
            if (node instanceof SplitPane) {
                // check recursively if tab holds SplitPane
                final SplitPane splitPane = (SplitPane) node;
                final TabPane tabPane = findUppermostTabPane(splitPane);

                if (tabPane != null) {
                    return tabPane;
                }
            }
        }

        return null;
    }

    private SplitPane findMostDirectContainer(Node child) {
        return findMostDirectContainer(splitPane, child);
    }

    private static SplitPane findMostDirectContainer(SplitPane splitPane, Node child) {
        for (Node node : splitPane.getItems()) {
            if (node == child) { // implicitly means node must also be instance of TabPane
                return splitPane;
            }
        }

        for (Node node : splitPane.getItems()) {
            if (node instanceof SplitPane) {
                final SplitPane result = findMostDirectContainer((SplitPane) node, child);
                if (result != null) {
                    return result;
                }
            }
        }

        return null;
    }

    @Override
    public void splitTab(@NotNull Tab tab, @NotNull Orientation orientation) {
        final TabPane tabPane = tab.getTabPane();
        if (tabPane != null) {
            final SplitPane parent = findMostDirectContainer(tabPane);
            if (parent != null) {
                // if 1 item, just add tabPane directly
                if (parent.getItems().size() < 2) {
                    parent.setOrientation(oppositeOrientation(orientation));
                    final TabPane newTabPane = createTabPane();
                    tabPane.getTabs().remove(tab);
                    newTabPane.getTabs().add(tab);
                    parent.getItems().add(newTabPane);
                } else { // if more than one, put tabPane into a splitPane
                    tabPane.getTabs().remove(tab);

                    final TabPane newTabPane = createTabPane();
                    newTabPane.getTabs().add(tab);

                    parent.getItems().remove(tabPane);

                    final SplitPane newSplitPane = createSplitPane(tabPane, newTabPane);
                    newSplitPane.setOrientation(oppositeOrientation(orientation));
                    parent.getItems().add(newSplitPane);
                }
            }
        }
    }

    private static final Background bg = new Background(new BackgroundFill(Color.YELLOW, null, null));

    private static Orientation oppositeOrientation(Orientation orientation) {
        if (orientation == Orientation.HORIZONTAL) {
            return Orientation.VERTICAL;
        } else if (orientation == Orientation.VERTICAL) {
            return Orientation.HORIZONTAL;
        } else {
            // impossible unless a new value is added down the road or something
            throw new IllegalStateException("Orientation must equal HORIZONTAL or VERTICAL.");
        }
    }

    private SplitPane createSplitPane(Node ... items) {
        final SplitPane splitPane = new SplitPane(items);

        splitPane.getItems().addListener((ListChangeListener<? super Node>) c -> {
            if (splitPane.getItems().isEmpty()) {
                final SplitPane parent = findMostDirectContainer(splitPane);
                if (parent != null) {
                    parent.getItems().remove(splitPane);
                }
            }
        });

        return splitPane;
    }

    private TabPane createTabPane() {
        final TabPane tabPane = new TabPane();

        tabPane.addEventFilter(MouseEvent.MOUSE_RELEASED, event -> {
            if (targetTab[0] != null) {
                final PickResult pickResult = event.getPickResult();
                Node node = pickResult.getIntersectedNode();

                while (node != null && !(node instanceof TabPane)) {
                    node = node.getParent();
                }

                final TabPane targetTabPane = (TabPane) node;
                final Tab tab = targetTab[0];

                if (targetTabPane != null && tab != null && tabPane != targetTabPane) {
                    tabPane.getTabs().remove(tab);
                    targetTabPane.getTabs().add(tab);
                }

                targetTab[0] = null;
            }
        });

        // remove if 0 items
        tabPane.getTabs().addListener((ListChangeListener<? super Tab>) c -> {
           if (tabPane.getTabs().isEmpty()) {
               final SplitPane parent = findMostDirectContainer(tabPane);
               if (parent != null) {
                   parent.getItems().remove(tabPane);

                   // if the closing tabPane was the root then set rootTabPane to
                   // another tabPane.
                   if (tabPane == rootTabPane) {
                       final TabPane nullableTabPane = findUppermostTabPane();
                       if (nullableTabPane != null) {
                           rootTabPane = nullableTabPane;
                       } else {
                           rootTabPane = createTabPane();
                           // since a new TabPane was created it needs to be added to root
                           splitPane.getItems().add(rootTabPane);
                       }
                   }
               }
           }
        });

        tabPane.setTabDragPolicy(TabPane.TabDragPolicy.REORDER);
        return tabPane;
    }
}

public class DockableTabPane extends Control implements DockableTabPaneApi {

    public DockableTabPane() {
        setSkin(new DockableTabPaneSkin(this));
    }

    @NotNull
    @Override
    public Tab addTab(@NotNull String name, @NotNull Node content) {
        final DockableTabPaneSkin skin = (DockableTabPaneSkin) getSkin();
        return skin.addTab(name, content);
    }

    @Override
    public void removeTab(@NotNull Tab tab) {
        final DockableTabPaneSkin skin = (DockableTabPaneSkin) getSkin();
        skin.removeTab(tab);
    }

    @Override
    public void splitTab(@NotNull Tab tab, @NotNull Orientation orientation) {
        final DockableTabPaneSkin skin = (DockableTabPaneSkin) getSkin();
        skin.splitTab(tab, orientation);
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        resize(getWidth(), getHeight());
    }

}
