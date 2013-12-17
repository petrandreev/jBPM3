package org.jbpm.jsf.core.handler;

import org.jboss.gravel.common.annotation.TldTag;
import org.jboss.gravel.common.annotation.TldAttribute;
import org.jbpm.jsf.JbpmActionListener;
import org.jbpm.jsf.core.action.CancelActionListener;

import com.sun.facelets.tag.TagAttribute;
import com.sun.facelets.tag.TagConfig;
import com.sun.facelets.FaceletContext;

/**
 *
 */
@TldTag (
    name = "cancel",
    description = "Cancel a running task instance, token, or process instance.",
    attributes = {
        @TldAttribute (
            name = "value",
            description = "The item to cancel.",
            required = true,
            deferredType = Object.class
        )
    }
)
public final class CancelHandler extends AbstractHandler {
    private final TagAttribute taskTagAttribute;

    public CancelHandler(final TagConfig config) {
        super(config);
        taskTagAttribute = getRequiredAttribute("value");
    }

    protected JbpmActionListener getListener(final FaceletContext ctx) {
        return new CancelActionListener(
            getValueExpression(taskTagAttribute, ctx, Object.class)
        );
    }
}
