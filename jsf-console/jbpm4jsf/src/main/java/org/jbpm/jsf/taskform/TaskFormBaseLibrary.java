package org.jbpm.jsf.taskform;

import org.jbpm.jsf.taskform.ui.UITaskFormCancelButton;
import org.jbpm.jsf.taskform.ui.UITaskFormSaveButton;
import org.jbpm.jsf.taskform.ui.UITaskFormTransitionButton;

import com.sun.facelets.tag.AbstractTagLibrary;

import javax.faces.context.FacesContext;
import javax.faces.application.Application;

/**
 *
 */
public abstract class TaskFormBaseLibrary extends AbstractTagLibrary {
    public TaskFormBaseLibrary(final String namespace) {
        super(namespace);

        final FacesContext facesContext = FacesContext.getCurrentInstance();
        final Application application = facesContext.getApplication();

        application.addComponent(UITaskFormCancelButton.COMPONENT_TYPE, UITaskFormCancelButton.class.getName());
        addComponent("cancelButton", UITaskFormCancelButton.COMPONENT_TYPE, "javax.faces.Button");
        application.addComponent(UITaskFormSaveButton.COMPONENT_TYPE, UITaskFormSaveButton.class.getName());
        addComponent("saveButton", UITaskFormSaveButton.COMPONENT_TYPE, "javax.faces.Button");
        application.addComponent(UITaskFormTransitionButton.COMPONENT_TYPE, UITaskFormTransitionButton.class.getName());
        addComponent("transitionButton", UITaskFormTransitionButton.COMPONENT_TYPE, "javax.faces.Button");
    }
}
