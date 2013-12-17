package org.jbpm.jsf;

import org.jbpm.JbpmContext;
import org.jbpm.identity.hibernate.IdentitySession;

import javax.faces.application.FacesMessage;

/**
 *
 */
public interface JbpmJsfContext {
    JbpmContext getJbpmContext();

    IdentitySession getIdentitySession();

    void addSuccessMessage(String message);

    void addSuccessMessage(String message, String detail);

    void addSuccessMessage(FacesMessage.Severity severity, String message, String detail);

    /**
     * Choose an outcome.  The last outcome selected will be the one returned to
     * the navigation layer, unless an error occurs, in which case,
     * the outcome will be set to "error".
     *
     * @param outcomeName the outcome name
     */
    void selectOutcome(String outcomeName);

    String getOutcome();

    boolean isError();

    void setError(String message);

    void setError(String message, String detail);

    void setError(String message, Throwable cause);
}
