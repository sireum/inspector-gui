package org.sireum.hamr.inspector.gui

import javafx.application.Application

object InspectorGUILauncher {

  def run(inspectorBlueprintConfiguration: Class[_], args: Array[String]): Unit = {
    App.inspectorBlueprintConfiguration = inspectorBlueprintConfiguration;
    App.args = args;
    Application.launch(classOf[App], args:_*)
  }

}
