package org.jbpm.jsf.core.handler;

import org.jboss.gravel.common.annotation.TldAttribute;
import org.jboss.gravel.common.annotation.TldTag;
import org.jbpm.jsf.JbpmActionListener;
import org.jbpm.jsf.core.action.DeleteActionListener;

import com.sun.facelets.FaceletContext;
import com.sun.facelets.tag.TagAttribute;
import com.sun.facelets.tag.TagConfig;

/**
 *
 */
@TldTag (
    name = "delete",
    description = "Delete a process, process instance, or job.",
    attributes = {
        @TldAttribute (
            name = "value",
            description = "The item to delete.",
            required = true,
            deferredType = Object.class
        )
    }
)
public final class DeleteHandler extends AbstractHandler {
    private final TagAttribute processTagAttribute;

    public DeleteHandler(final TagConfig config) {
        super(config);
        processTagAttribute = getRequiredAttribute("value");
    }

    protected JbpmActionListener getListener(final FaceletContext ctx) {
        return new DeleteActionListener(
            getValueExpression(processTagAttribute, ctx, Object.class)
        );
    }
}
