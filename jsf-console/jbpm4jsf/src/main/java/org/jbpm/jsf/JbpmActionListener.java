package org.jbpm.jsf;

import javax.faces.event.ActionEvent;

/**
 *
 */
public interface JbpmActionListener {
    /**
     * Get the name of this action.  This name may be used
     * by the navigation handler to choose an appropriate navigation
     * outcome.
     *
     * @return the name
     */
    String getName();

    void handleAction(JbpmJsfContext context, ActionEvent event);
}
