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

package org.sireum.hamr.inspector.gui

import javafx.application.Application
import org.sireum.hamr.inspector.common.{Filter, Injection, InspectionBlueprint, Rule}

import scala.collection.JavaConverters

object InspectorGUILauncher {

  def run(inspectionBlueprint: InspectionBlueprint,
          filters: Set[Filter],
          rules: Set[Rule],
          injections: Set[Injection],
          args: Array[String]): Unit = {
    App.inspectionBlueprint = inspectionBlueprint
    App.filters = JavaConverters.setAsJavaSet(filters)
    App.rules = JavaConverters.setAsJavaSet(rules)
    App.injections = JavaConverters.setAsJavaSet(injections)
    App.args = args;
    Application.launch(classOf[App], args:_*)
  }

//  @Configuration
//  class InspectionBlueprintConfig(filters: Seq[Filter]) {
//
//
//
//  }

//  def run(inspectorBlueprintConfiguration: Class[_], args: Array[String]): Unit = {
//    App.inspectorBlueprintConfiguration = inspectorBlueprintConfiguration;
//    App.args = args;
//    Application.launch(classOf[App], args:_*)
//  }

}
