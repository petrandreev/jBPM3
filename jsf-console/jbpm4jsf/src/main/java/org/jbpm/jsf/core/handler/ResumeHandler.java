package org.jbpm.jsf.core.handler;

import org.jboss.gravel.common.annotation.TldTag;
import org.jboss.gravel.common.annotation.TldAttribute;
import org.jbpm.jsf.JbpmActionListener;
import org.jbpm.jsf.core.action.ResumeActionListener;

import com.sun.facelets.tag.TagAttribute;
import com.sun.facelets.tag.TagConfig;
import com.sun.facelets.FaceletContext;

/**
 *
 */
@TldTag (
    name = "resume",
    description = "Resume a running task, token, or process instance.",
    attributes = {
        @TldAttribute (
            name = "value",
            description = "The item to resume.",
            required = true,
            deferredType = Object.class
        )
    }
)
public final class ResumeHandler extends AbstractHandler {
    private final TagAttribute valueTagAttribute;

    public ResumeHandler(final TagConfig config) {
        super(config);
        valueTagAttribute = getRequiredAttribute("value");
    }

    protected JbpmActionListener getListener(final FaceletContext ctx) {
        return new ResumeActionListener(
            getValueExpression(valueTagAttribute, ctx, Object.class)
        );
    }
}
