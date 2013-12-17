package org.jbpm.jsf.identity.action;

import org.jbpm.identity.Group;
import org.jbpm.identity.hibernate.IdentitySession;
import org.jbpm.jsf.JbpmActionListener;
import org.jbpm.jsf.JbpmJsfContext;

import javax.el.ELContext;
import javax.el.ValueExpression;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

/**
 *
 */
public final class LoadGroupByNameActionListener implements JbpmActionListener {

    private final ValueExpression groupNameExpression;
    private final ValueExpression targetExpression;

    public LoadGroupByNameActionListener(final ValueExpression groupNameExpression, final ValueExpression targetExpression) {
        this.groupNameExpression = groupNameExpression;
        this.targetExpression = targetExpression;
    }

    public String getName() {
        return "loadGroupByName";
    }

    public void handleAction(JbpmJsfContext context, ActionEvent event) {
        try {
            final FacesContext facesContext = FacesContext.getCurrentInstance();
            final ELContext elContext = facesContext.getELContext();
            final Object nameValue = groupNameExpression.getValue(elContext);
            final IdentitySession identitySession = new IdentitySession(context.getJbpmContext().getSession());
            if (nameValue == null) {
                context.setError("Error loading group", "The group name value is null");
                return;
            }
            final String groupName = nameValue.toString();
            final Group group = identitySession.getGroupByName(groupName);
            if (group == null) {
                context.setError("Error loading group", "No group was found named '" + groupName + "'");
                return;
            }
            targetExpression.setValue(elContext, group);
            context.selectOutcome("success");
        } catch (Exception ex) {
            context.setError("Error loading group", ex);
            return;
        }
    }
}
