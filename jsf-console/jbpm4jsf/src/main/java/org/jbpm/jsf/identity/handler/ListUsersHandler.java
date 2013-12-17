package org.jbpm.jsf.identity.handler;

import java.util.List;
import org.jboss.gravel.common.annotation.TldAttribute;
import org.jboss.gravel.common.annotation.TldTag;
import org.jbpm.jsf.JbpmActionListener;
import org.jbpm.jsf.core.handler.AbstractHandler;
import org.jbpm.jsf.identity.action.ListUsersActionListener;

import com.sun.facelets.FaceletContext;
import com.sun.facelets.tag.TagAttribute;
import com.sun.facelets.tag.TagConfig;

/**
 *
 */
@TldTag (
    name = "listUsers",
    description = "Read the list of users from the database.",
    attributes = {
        @TldAttribute (
            name = "target",
            description = "An EL expression into which the user list should be stored.",
            required = true,
            deferredType = List.class
        )
    }
)
public final class ListUsersHandler extends AbstractHandler {
    private final TagAttribute targetTagAttribute;

    public ListUsersHandler(final TagConfig config) {
        super(config);
        targetTagAttribute = getRequiredAttribute("target");
    }

    protected JbpmActionListener getListener(final FaceletContext ctx) {
        return new ListUsersActionListener(
            getValueExpression(targetTagAttribute, ctx, List.class)
        );
    }
}
