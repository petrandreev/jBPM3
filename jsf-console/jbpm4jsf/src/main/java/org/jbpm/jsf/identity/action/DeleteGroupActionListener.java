package org.jbpm.jsf.identity.action;

import org.jbpm.identity.Group;
import org.jbpm.jsf.JbpmActionListener;
import org.jbpm.jsf.JbpmJsfContext;

import javax.el.ELContext;
import javax.el.ValueExpression;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

/**
 *
 */
public final class DeleteGroupActionListener implements JbpmActionListener {
    private final ValueExpression groupExpression;

    public DeleteGroupActionListener(final ValueExpression groupExpression) {
        this.groupExpression = groupExpression;
    }

    public String getName() {
        return "deleteGroup";
    }

    public void handleAction(JbpmJsfContext context, ActionEvent event) {
        try {
            final FacesContext facesContext = FacesContext.getCurrentInstance();
            final ELContext elContext = facesContext.getELContext();
            final Group group = (Group) groupExpression.getValue(elContext);
            context.getJbpmContext().getSession().delete(group);
            context.addSuccessMessage("Successfully deleted group");
        } catch (Exception ex) {
            context.setError("Failed to delete group", ex);
        }
    }
}
