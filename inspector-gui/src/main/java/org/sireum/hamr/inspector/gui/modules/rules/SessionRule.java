package org.sireum.hamr.inspector.gui.modules.rules;

import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.sireum.hamr.inspector.common.Rule;
import org.sireum.hamr.inspector.engine.RuleProcessorService;
import org.sireum.hamr.inspector.services.RuleStatus;
import org.sireum.hamr.inspector.services.Session;

/**
 * Represents a {@link Rule} and {@link Session} pair, which are the two components to get a
 * {@link RuleStatus} from {@link RuleProcessorService}.
 *
 * The {@link Session} field is {@link Nullable} because no {@link Rule} is initially selected when a {@link RulesTab}
 * is first opened.
 */
@Value(staticConstructor = "of")
public final class SessionRule {
    @Nullable final Session session;
    @NotNull final Rule rule;
}
