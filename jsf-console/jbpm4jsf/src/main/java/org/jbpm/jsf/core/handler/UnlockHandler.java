package org.jbpm.jsf.core.handler;

import org.jboss.gravel.common.annotation.TldAttribute;
import org.jboss.gravel.common.annotation.TldTag;
import org.jbpm.jsf.JbpmActionListener;
import org.jbpm.jsf.core.action.UnlockActionListener;

import com.sun.facelets.FaceletContext;
import com.sun.facelets.tag.TagAttribute;
import com.sun.facelets.tag.TagConfig;

/**
 *
 */
@TldTag (
    name = "unlock",
    description = "Unlock a running token.",
    attributes = {
        @TldAttribute (
            name = "value",
            description = "The item to unlock.",
            required = true,
            deferredType = Object.class
        )
    }
)
public final class UnlockHandler extends AbstractHandler {
    private final TagAttribute taskTagAttribute;

    public UnlockHandler(final TagConfig config) {
        super(config);
        taskTagAttribute = getRequiredAttribute("value");
    }

    protected JbpmActionListener getListener(final FaceletContext ctx) {
        return new UnlockActionListener(
            getValueExpression(taskTagAttribute, ctx, Object.class)
        );
    }
}
