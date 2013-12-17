package org.jbpm.jsf.core.action;

import org.jbpm.jsf.JbpmActionListener;
import org.jbpm.jsf.JbpmJsfContext;
import org.jbpm.graph.exe.ProcessInstance;

import javax.el.ValueExpression;
import javax.el.ELContext;
import javax.faces.event.ActionEvent;
import javax.faces.context.FacesContext;

import java.util.List;

public final class ListProcessInstancesActionListener implements JbpmActionListener {
    private final ValueExpression targetExpression;
    private final ValueExpression processIdExpression;

    public ListProcessInstancesActionListener(final ValueExpression processIdExpression, final ValueExpression targetExpression) {
        this.processIdExpression = processIdExpression;
        this.targetExpression = targetExpression;
    }

    public String getName() {
        return "listProcessInstances";
    }

    @SuppressWarnings ({"unchecked"})
    public void handleAction(JbpmJsfContext context, ActionEvent event) {
        try {
            final FacesContext facesContext = FacesContext.getCurrentInstance();
            final ELContext elContext = facesContext.getELContext();
            final Object idValue = processIdExpression.getValue(elContext);
            final long id;
            if (idValue instanceof Number) {
                id = ((Number)idValue).longValue();
            } else if (idValue instanceof String) {
                id = Long.parseLong((String)idValue);
            } else if (idValue != null) {
                id = Long.parseLong(idValue.toString());
            } else {
                context.setError("Error loading process instance list", "The process ID value is null");
                return;
            }
            final List<ProcessInstance> processList = context.getJbpmContext().getGraphSession().findProcessInstances(id);
            targetExpression.setValue(elContext, processList);
            context.selectOutcome("success");
        } catch (Exception ex) {
            context.setError("Error loading process instance list", ex);
            return;
        }
    }
}
