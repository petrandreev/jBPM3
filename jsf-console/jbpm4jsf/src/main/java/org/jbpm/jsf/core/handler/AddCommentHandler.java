package org.jbpm.jsf.core.handler;

import org.jboss.gravel.common.annotation.TldAttribute;
import org.jboss.gravel.common.annotation.TldTag;
import org.jbpm.graph.def.GraphElement;
import org.jbpm.jsf.JbpmActionListener;
import org.jbpm.jsf.core.action.AddCommentActionListener;

import com.sun.facelets.FaceletContext;
import com.sun.facelets.tag.TagAttribute;
import com.sun.facelets.tag.TagConfig;

/**
 *
 */
@TldTag(
    name ="addComment",
    description = "Add a comment to a task or token.",
    attributes = {
    @TldAttribute (
        name = "comment",
        description = "An EL expression that returns the comment text to add.",
        deferredType = String.class,
        required = true
    ),
    @TldAttribute (
        name = "target",
        description = "An EL expression that returns the node to add a comment to.",
        deferredType = GraphElement.class,
        required = true
    )
})
public final class AddCommentHandler extends AbstractHandler {
    private final TagAttribute commentAttribute;
    private final TagAttribute targetAttribute;

    public AddCommentHandler(final TagConfig config) {
        super(config);
        commentAttribute = getRequiredAttribute("comment");
        targetAttribute = getRequiredAttribute("target");
    }

    protected JbpmActionListener getListener(final FaceletContext ctx) {
        return new AddCommentActionListener(commentAttribute.getValueExpression(ctx, String.class), targetAttribute.getValueExpression(ctx, Object.class));
    }
}
