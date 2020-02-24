package org.sireum.hamr.inspector.gui.tasks.reports;

import lombok.Data;
import org.sireum.hamr.inspector.common.Rule;
import org.sireum.hamr.inspector.services.Session;

@Data
public class RuleTestJob {
    final Rule rule;
    final Session session;

    final String getName() {
        return rule.name() + " @ " + session;
    }
}
