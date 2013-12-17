package org.jbpm.jsf.identity.handler;

import java.util.List;
import org.jboss.gravel.common.annotation.TldAttribute;
import org.jboss.gravel.common.annotation.TldTag;
import org.jbpm.jsf.JbpmActionListener;
import org.jbpm.jsf.core.handler.AbstractHandler;
import org.jbpm.jsf.identity.action.ListGroupsActionListener;

import com.sun.facelets.FaceletContext;
import com.sun.facelets.tag.TagAttribute;
import com.sun.facelets.tag.TagConfig;

/**
 *
 */
@TldTag (
    name = "listGroups",
    description = "Read the list of groups from the database.",
    attributes = {
        @TldAttribute (
            name = "target",
            description = "An EL expression into which the group list should be stored.",
            required = true,
            deferredType = List.class
        )
    }
)
public final class ListGroupsHandler extends AbstractHandler {
    private final TagAttribute targetTagAttribute;

    public ListGroupsHandler(final TagConfig config) {
        super(config);
        targetTagAttribute = getRequiredAttribute("target");
    }

    protected JbpmActionListener getListener(final FaceletContext ctx) {
        return new ListGroupsActionListener(
            getValueExpression(targetTagAttribute, ctx, List.class)
        );
    }
}
