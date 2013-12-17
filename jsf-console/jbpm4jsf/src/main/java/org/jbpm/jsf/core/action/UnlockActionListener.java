package org.jbpm.jsf.core.action;

import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.jsf.JbpmActionListener;
import org.jbpm.jsf.JbpmJsfContext;
import org.jbpm.taskmgmt.exe.TaskInstance;

import javax.el.ELContext;
import javax.el.ValueExpression;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

/**
 *
 */
public final class UnlockActionListener implements JbpmActionListener {

    private final ValueExpression valueExpression;

    public UnlockActionListener(final ValueExpression valueExpression) {
        this.valueExpression = valueExpression;
    }

    public String getName() {
        return "unlock";
    }

    public void handleAction(JbpmJsfContext context, ActionEvent event) {
        try {
            final FacesContext facesContext = FacesContext.getCurrentInstance();
            final ELContext elContext = facesContext.getELContext();
            final Object value = valueExpression.getValue(elContext);
            if (value == null) {
                context.setError("Unlock failed", "The value is null");
                return;
            }
            if (value instanceof TaskInstance) {
                ((TaskInstance)value).setSignalling(false);
                Token token = ((TaskInstance)value).getToken();
                if( token.isLocked() ) { 
                  String lockOwner = token.getLockOwner();
                  token.unlock(lockOwner);
                }
                else { 
                  context.setError("Unlock failed", "The token is not locked.");
                  return;
                }
                context.addSuccessMessage("Task instance unlocked");
            } else if (value instanceof Token) {
                final Token token = ((Token) value);
                if( token.isLocked() ) { 
                  String lockOwner = token.getLockOwner();
                  token.unlock(lockOwner);
                }
                else { 
                  context.setError("Unlock failed", "The token is not locked.");
                  return;
                }
                context.addSuccessMessage("Token unlocked");
            } else if (value instanceof ProcessInstance) {
                final ProcessInstance processInstance = ((ProcessInstance) value);
                Token token = processInstance.getRootToken();
                if( token.isLocked() ) { 
                  String lockOwner = token.getLockOwner();
                  token.unlock(lockOwner);
                }
                else { 
                  context.setError("Unlock failed", "The token is not locked.");
                  return;
                }
                context.addSuccessMessage("Process instance unlocked");
            } else {
                context.setError("Unlock failed", "The value is not a recognized type");
                return;
            }
            context.getJbpmContext().getSession().flush();
            context.selectOutcome("success");
        } catch (Exception ex) {
            context.setError("Unlock failed", ex);
            return;
        }
    }
}
