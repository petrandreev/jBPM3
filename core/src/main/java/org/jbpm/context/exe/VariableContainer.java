package org.jbpm.context.exe;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;

import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.jbpm.context.log.VariableDeleteLog;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.logging.db.DbLoggingService;

public abstract class VariableContainer implements Serializable {

  protected Map variableInstances;

  private static final long serialVersionUID = 520258491083406913L;

  protected abstract VariableContainer getParentVariableContainer();

  public abstract Token getToken();

  // variables ////////////////////////////////////////////////////////////////

  public Object getVariable(String name) {
    Object value = null;
    if (hasVariableLocally(name)) {
      value = getVariableLocally(name);
    }
    else {
      VariableContainer parent = getParentVariableContainer();
      if (parent != null) {
        // check upwards in the token hierarchy
        value = parent.getVariable(name);
      }
    }
    return value;
  }

  public void setVariable(String name, Object value) {
    VariableContainer parent = getParentVariableContainer();
    if (hasVariableLocally(name) || parent == null) {
      setVariableLocally(name, value);
    }
    else {
      // propagate to parent variable container
      parent.setVariable(name, value);
    }
  }

  public boolean hasVariable(String name) {
    // if the variable is present in the variable instances
    if (hasVariableLocally(name)) return true;

    // search in parent variable container
    VariableContainer parent = getParentVariableContainer();
    if (parent != null) return parent.hasVariable(name);

    return false;
  }

  public void deleteVariable(String name) {
    if (name == null) throw new JbpmException("variable name is null");
    if (hasVariableLocally(name)) deleteVariableLocally(name);
  }

  /**
   * adds all the given variables to this variable container. The method
   * {@link #setVariables(Map)} is the same as this method, but it was added for naming
   * consistency.
   */
  public void addVariables(Map variables) {
    setVariables(variables);
  }

  /**
   * adds all the given variables to this variable container. It doesn't remove any existing
   * variables unless they are overwritten by the given variables. This method is the same as
   * {@link #addVariables(Map)} and this method was added for naming consistency.
   */
  public void setVariables(Map variables) {
    if (variables != null) {
      for (Iterator iter = variables.entrySet().iterator(); iter.hasNext();) {
        Map.Entry entry = (Map.Entry) iter.next();
        setVariable((String) entry.getKey(), entry.getValue());
      }
    }
  }

  public Map getVariables() {
    Map variables = getVariablesLocally();
    VariableContainer parent = getParentVariableContainer();
    if (parent != null) {
      Map parentVariables = parent.getVariablesLocally();
      parentVariables.putAll(variables);
      variables = parentVariables;
    }
    return variables;
  }

  public Map getVariablesLocally() {
    Map variables = new HashMap();
    if (variableInstances != null) {
      for (Iterator iter = variableInstances.entrySet().iterator(); iter.hasNext();) {
        Map.Entry entry = (Map.Entry) iter.next();
        String name = (String) entry.getKey();
        VariableInstance variableInstance = (VariableInstance) entry.getValue();
        if (!variables.containsKey(name)) {
          variables.put(name, variableInstance.getValue());
        }
      }
    }
    return variables;
  }

  // local variable methods ///////////////////////////////////////////////////

  public boolean hasVariableLocally(String name) {
    return variableInstances != null && variableInstances.containsKey(name);
  }

  public Object getVariableLocally(String name) {
    Object value = null;

    // if the variable is present in the variable instances
    if (hasVariableLocally(name)) {
      value = getVariableInstance(name).getValue();
    }

    return value;
  }

  public void deleteVariableLocally(String name) {
    deleteVariableInstance(name);
  }

  public void setVariableLocally(String name, Object value) {
    if (name == null) {
      throw new IllegalArgumentException("variable name is null");
    }

    VariableInstance variableInstance = getVariableInstance(name);
    // if variable instance already exists and it does not support the new value
    if (variableInstance != null && !variableInstance.supports(value)) {
      // delete the old variable instance
      if (log.isDebugEnabled()) {
        log.debug(variableInstance.getToken() + " unsets '" + name + "' due to type change");
      }
      deleteVariableInstance(name);
      variableInstance = null;
    }

    if (variableInstance != null) {
      if (log.isDebugEnabled()) {
        log.debug(variableInstance.getToken() + " sets '" + name + "' to " + value);
      }
      variableInstance.setValue(value);
    }
    else {
      Token token = getToken();
      if (log.isDebugEnabled()) {
        log.debug(token + " initializes '" + name + "' to " + value);
      }
      addVariableInstance(VariableInstance.create(token, name, value));
    }
  }

  // local variable instances /////////////////////////////////////////////////

  public VariableInstance getVariableInstance(String name) {
    return variableInstances != null ? (VariableInstance) variableInstances.get(name) : null;
  }

  public Map getVariableInstances() {
    return variableInstances;
  }

  public void addVariableInstance(VariableInstance variableInstance) {
    if (variableInstances == null) variableInstances = new HashMap();
    variableInstances.put(variableInstance.getName(), variableInstance);
    // only register additions in the updated variable containers
    // because the registry is only used to check for non-persistable variables
    ContextInstance contextInstance = getContextInstance();
    if (contextInstance != null) contextInstance.addUpdatedVariableContainer(this);
  }

  public void deleteVariableInstance(String name) {
    if (variableInstances != null) {
      VariableInstance variableInstance = (VariableInstance) variableInstances.remove(name);
      if (variableInstance != null) {
        // unlink variable
        variableInstance.removeReferences();
        // log variable deletion
        getToken().addLog(new VariableDeleteLog(variableInstance));

        // if a context is present and its logging service is not connected to the database 
        JbpmContext jbpmContext = JbpmContext.getCurrentJbpmContext();
        if (jbpmContext != null
          && !(jbpmContext.getServices().getLoggingService() instanceof DbLoggingService)) {
          // delete variable instance here before all references to it are lost
          Session session = jbpmContext.getSession();
          if (session != null) session.delete(variableInstance);
        }
      }
    }
  }

  public ContextInstance getContextInstance() {
    Token token = getToken();
    return token != null ? token.getProcessInstance().getContextInstance() : null;
  }

  /** @deprecated call {@link ContextInstance#getUpdatedVariableContainers()} instead */
  public static Collection getUpdatedVariableContainers(ProcessInstance processInstance) {
    return processInstance.getContextInstance().updatedVariableContainers;
  }

  private static final Log log = LogFactory.getLog(VariableContainer.class);
}
