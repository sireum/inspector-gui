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

package org.sireum.hamr.inspector.gui.tasks.reports;

import io.qameta.allure.SeverityLevel;
import lombok.Data;
import org.jetbrains.annotations.Nullable;

// https://docs.qameta.io/allure/#_features_2

@Data
public class RuleMetaInfo {

    // todo add categories for Passed, Failed, Unresolved

    @Nullable final SeverityLevel severityLevel;

    // see: https://docs.qameta.io/allure/#_behaviours_mapping
    @Nullable final String epic;    // epic = "Rules"
    @Nullable final String feature; // feature = name of capdef if one is used?
    @Nullable final String story;   // specific rule name?

    @Nullable final String displayName; // allure checks for junit5 displayName
    @Nullable final String description;

    // todo add annotated Steps (@Step) to each operator in org.sireum.hamr.inspector.stream.Flux and resolve for report

    @Nullable final String attachment; // simply any extra info I want attached
    @Nullable final String link; // useful for linking to msc chart!!!

}
