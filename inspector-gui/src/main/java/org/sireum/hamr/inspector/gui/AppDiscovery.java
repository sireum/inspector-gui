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

import javafx.collections.ObservableList;
import lombok.extern.slf4j.Slf4j;
import org.sireum.hamr.inspector.common.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;

import static javafx.collections.FXCollections.observableList;
import static javafx.collections.FXCollections.unmodifiableObservableList;

@Slf4j
//@SpringBootApplication
@SpringBootConfiguration
//@EnableAutoConfiguration(exclude = {
//        TaskExecutionAutoConfiguration.class,
//        CacheAutoConfiguration.class,
////        LocalDevToolsAutoConfiguration.class,
//        ProjectInfoAutoConfiguration.class,
//        AopAutoConfiguration.class,
//        TaskExecutionAutoConfiguration.class,
////        DevToolsDataSourceAutoConfiguration.class,
//        PropertyPlaceholderAutoConfiguration.class,
//        TaskSchedulingAutoConfiguration.class,
//        MessageSourceAutoConfiguration.class,
//        JacksonAutoConfiguration.class,
//        ConfigurationPropertiesAutoConfiguration.class, // todo exclude?
//        CacheAutoConfiguration.class,
//        FreeMarkerAutoConfiguration.class,
//})
@EnableAutoConfiguration // todo want to have this for deps?
@ComponentScan(basePackages = {
        // at this point the InspectorBlueprint which provides the target hamr project has already been discovered
        "org.sireum.hamr.inspector.common",         // then apply to common for ArtUtils
        "org.sireum.hamr.inspector.services",       // then services
        "org.sireum.hamr.inspector.engine",         // then engine which uses services
        "org.sireum.hamr.inspector.gui",            // the gui which uses engine
        "org.sireum.hamr.inspector"                 // then anything else
})
@Configuration
public class AppDiscovery {

    private final ApplicationContext applicationContext;

    public AppDiscovery(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Bean(name = "inspectionBlueprint")
    public InspectionBlueprint inspectionBlueprint() {
        return Objects.requireNonNull(App.inspectionBlueprint);
    }

    @Bean(name = "artUtils")
    public ArtUtils artUtils(@Qualifier("inspectionBlueprint") InspectionBlueprint inspectionBlueprint) {
        return ArtUtils.create(inspectionBlueprint);
    }

    @Bean(name = "filters")
    public ObservableList<Filter> filters() {
//        final Filter[] filters = applicationContext.getBeansOfType(Filter.class).values().toArray(new Filter[0]);
//        return unmodifiableObservableList(observableList(List.of(filters)));

        final Set<Filter> filters = Objects.requireNonNull(Objects.requireNonNull(App.filters));
        return unmodifiableObservableList(observableList(new ArrayList<>(filters)));
    }

    @Bean(name = "rules")
    public ObservableList<Rule> rules() {
//        final Rule[] rules = applicationContext.getBeansOfType(Rule.class).values().toArray(new Rule[0]);
//        return unmodifiableObservableList(observableList(List.of(rules)));

        final Set<Rule> rules = Objects.requireNonNull(Objects.requireNonNull(App.rules));
        return unmodifiableObservableList(observableList(new ArrayList<>(rules)));
    }

    @Bean(name = "injections")
    public ObservableList<Injection> injections() {
//        final Injection[] injections = applicationContext.getBeansOfType(Injection.class).values().toArray(new Injection[0]);
//        return unmodifiableObservableList(observableList(List.of(injections)));

        final Set<Injection> injections = Objects.requireNonNull(Objects.requireNonNull(App.injections));
        return unmodifiableObservableList(observableList(new ArrayList<>(injections)));
    }

}
