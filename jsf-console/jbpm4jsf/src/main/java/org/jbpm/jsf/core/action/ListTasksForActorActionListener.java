package org.jbpm.jsf.core.action;

import org.jbpm.jsf.JbpmActionListener;
import org.jbpm.jsf.JbpmJsfContext;
import org.jbpm.taskmgmt.exe.TaskInstance;

import javax.el.ValueExpression;
import javax.el.ELContext;
import javax.faces.event.ActionEvent;
import javax.faces.context.FacesContext;

import java.util.List;
import java.util.Collections;

/**
 *
 */
public final class ListTasksForActorActionListener implements JbpmActionListener {
    private final ValueExpression targetExpression;
    private final ValueExpression actorExpression;

    public ListTasksForActorActionListener(final ValueExpression actorExpression, final ValueExpression targetExpression) {
        this.actorExpression = actorExpression;
        this.targetExpression = targetExpression;
    }

    public String getName() {
        return "listTasksForActorActionListener";
    }

    @SuppressWarnings ({"unchecked"})
    public void handleAction(JbpmJsfContext context, ActionEvent event) {
        try {
            final FacesContext facesContext = FacesContext.getCurrentInstance();
            final ELContext elContext = facesContext.getELContext();
            final String actorId = (String) actorExpression.getValue(elContext);
            final List<TaskInstance> taskList = Collections.unmodifiableList(context.getJbpmContext().getTaskList(actorId));
            targetExpression.setValue(elContext, taskList);
            context.selectOutcome("success");
        } catch (Exception ex) {
            context.setError("Error loading task list", ex);
            return;
        }
    }
}
