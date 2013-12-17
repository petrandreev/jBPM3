package org.jbpm.jsf.identity.handler;

import org.jbpm.jsf.core.handler.AbstractHandler;
import org.jbpm.jsf.JbpmActionListener;
import org.jbpm.jsf.identity.action.VerifyUserActionListener;

import com.sun.facelets.FaceletContext;
import com.sun.facelets.tag.TagConfig;
import com.sun.facelets.tag.TagAttribute;

/**
 *
 */
public final class VerifyUserHandler extends AbstractHandler {
    private final TagAttribute userNameTagAttribute;
    private final TagAttribute passwordTagAttribute;
    private final TagAttribute userIdTargetTagAttribute;

    public VerifyUserHandler(final TagConfig config) {
        super(config);
        userNameTagAttribute = getRequiredAttribute("userName");
        passwordTagAttribute = getRequiredAttribute("password");
        userIdTargetTagAttribute = getRequiredAttribute("userIdTarget");
    }

    protected JbpmActionListener getListener(final FaceletContext ctx) {
        return new VerifyUserActionListener(
            getValueExpression(userNameTagAttribute, ctx, String.class),
            getValueExpression(passwordTagAttribute, ctx, String.class),
            getValueExpression(userIdTargetTagAttribute, ctx, Long.class)
        );
    }
}
