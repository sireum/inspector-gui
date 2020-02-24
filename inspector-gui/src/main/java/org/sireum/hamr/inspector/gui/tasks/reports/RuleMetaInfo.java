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
