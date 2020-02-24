package org.sireum.hamr.inspector.gui;

import javafx.collections.ObservableList;
import lombok.extern.slf4j.Slf4j;
import org.sireum.hamr.inspector.common.ArchDiscovery;
import org.sireum.hamr.inspector.common.Filter;
import org.sireum.hamr.inspector.common.Injection;
import org.sireum.hamr.inspector.common.Rule;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import java.util.List;

import static javafx.collections.FXCollections.observableList;
import static javafx.collections.FXCollections.unmodifiableObservableList;

@Slf4j
@SpringBootApplication
@ComponentScan(basePackages = {
        "org.sireum.hamr.inspector.common",     // first discover the Arch
        "org.sireum.hamr.inspector.services",   // then services
        "org.sireum.hamr.inspector.engine",     // then engine which uses services
        "org.sireum.hamr.inspector.gui",        // the gui which uses engine
        "org.sireum.hamr.inspector",             // then anything else
})
public class AppDiscovery {

    private final ApplicationContext applicationContext;

    public AppDiscovery(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Bean(name = "filters")
    public ObservableList<Filter> capDefs() {
        final Filter[] filters = applicationContext.getBeansOfType(Filter.class).values().toArray(new Filter[0]);
        return unmodifiableObservableList(observableList(List.of(filters)));
    }

    @Bean(name = "rules")
    public ObservableList<Rule> rules() {
        final Rule[] rules = applicationContext.getBeansOfType(Rule.class).values().toArray(new Rule[0]);
        return unmodifiableObservableList(observableList(List.of(rules)));
    }

    @Bean(name = "injections")
    public ObservableList<Injection> injections() {
        final Injection[] injections = applicationContext.getBeansOfType(Injection.class).values().toArray(new Injection[0]);
        return unmodifiableObservableList(observableList(List.of(injections)));
    }

    @Bean(name = "archDiscovery")
    public ArchDiscovery archDiscovery() {
        final var beans = applicationContext.getBeansOfType(ArchDiscovery.class).values().toArray(new ArchDiscovery[0]);

        if (beans.length == 0) {
            final String errorString = "Unable to discover art architecture. Please ensure an ArchitectureDescription is on the classpath.";
            log.error(errorString);
            System.exit(-1);
        } else if (beans.length > 1) {
            log.warn("Multiple ArchitectureDescriptions discovered on classpath! There should be one. Using {}", beans[0].ad().toString());
        }

        return beans[0];
    }

}
