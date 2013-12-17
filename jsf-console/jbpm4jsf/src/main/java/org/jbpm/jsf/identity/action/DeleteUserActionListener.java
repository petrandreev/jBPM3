package org.jbpm.jsf.identity.action;

import org.hibernate.Session;
import org.jbpm.identity.User;
import org.jbpm.jsf.JbpmActionListener;
import org.jbpm.jsf.JbpmJsfContext;

import javax.el.ELContext;
import javax.el.ValueExpression;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

/**
 *
 */
public final class DeleteUserActionListener implements JbpmActionListener {
    private final ValueExpression userExpression;

    public DeleteUserActionListener(final ValueExpression userExpression) {
        this.userExpression = userExpression;
    }

    public String getName() {
        return "deleteUser";
    }

    public void handleAction(JbpmJsfContext context, ActionEvent event) {
        try {
            final FacesContext facesContext = FacesContext.getCurrentInstance();
            final ELContext elContext = facesContext.getELContext();
            final User user = (User) userExpression.getValue(elContext);
            final Session session = context.getJbpmContext().getSession();
            session.delete(user);
            context.addSuccessMessage("Successfully deleted user");
        } catch (Exception ex) {
            context.setError("Failed to delete user", ex);
        }
    }
}
