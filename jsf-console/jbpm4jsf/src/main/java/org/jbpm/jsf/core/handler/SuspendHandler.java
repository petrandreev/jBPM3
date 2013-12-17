package org.jbpm.jsf.core.handler;

import org.jboss.gravel.common.annotation.TldTag;
import org.jboss.gravel.common.annotation.TldAttribute;
import org.jbpm.jsf.JbpmActionListener;
import org.jbpm.jsf.core.action.SuspendActionListener;

import com.sun.facelets.tag.TagAttribute;
import com.sun.facelets.tag.TagConfig;
import com.sun.facelets.FaceletContext;

/**
 *
 */
@TldTag (
    name = "suspend",
    description = "Suspend a running task, token, or process instance.",
    attributes = {
        @TldAttribute (
            name = "value",
            description = "The item to suspend.",
            required = true,
            deferredType = Object.class
        )
    }
)
public final class SuspendHandler extends AbstractHandler {
    private final TagAttribute valueTagAttribute;

    public SuspendHandler(final TagConfig config) {
        super(config);
        valueTagAttribute = getRequiredAttribute("value");
    }

    protected JbpmActionListener getListener(final FaceletContext ctx) {
        return new SuspendActionListener(
            getValueExpression(valueTagAttribute, ctx, Object.class)
        );
    }
}
