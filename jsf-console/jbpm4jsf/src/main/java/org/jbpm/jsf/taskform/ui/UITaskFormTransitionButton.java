package org.jbpm.jsf.taskform.ui;

import java.io.Serializable;

import javax.el.ValueExpression;
import javax.faces.context.FacesContext;

import org.jbpm.jsf.taskform.ui.UITaskFormButtonBase;

/**
 *
 */
public final class UITaskFormTransitionButton extends UITaskFormButtonBase {
    public static final String COMPONENT_TYPE = "jbpm4jsf.tf.TransitionButton";

    private String transition;

    public String getTransition() {
        if (transition != null) {
            return transition;
        } else {
            final ValueExpression transitionExpression = getValueExpression("transition");
            if (transitionExpression == null) {
                return null;
            }
            return (String)transitionExpression.getValue(getFacesContext().getELContext());
        }
    }

    public void setTransition(final String transition) {
        this.transition = transition;
    }
    

    private State state;

    public Object saveState(FacesContext context) {
        if (state == null) {
            state = new State();
        }
        state.superState = super.saveState(context);
        state.transition = transition;
        return state;
    }

    public void restoreState(FacesContext context, Object object) {
        state = (State) object;
        super.restoreState(context, state.superState);
        transition = state.transition;
    }

    static final class State implements Serializable {
        private static final long serialVersionUID = 1L;

        Object superState;
        String transition;
    }

}
