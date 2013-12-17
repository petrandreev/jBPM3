package org.jbpm.context.exe;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbpm.JbpmException;
import org.jbpm.context.log.VariableDeleteLog;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;

public abstract class VariableContainer implements Serializable {
  
  private static final long serialVersionUID = 520258491083406913L;
  protected Map<String, VariableInstance> variableInstances = null;

  protected abstract VariableContainer getParentVariableContainer();
  public abstract Token getToken();
  
  // variables ////////////////////////////////////////////////////////////////
  
  public Object getVariable(String name) {
    Object value = null;
    if (hasVariableLocally(name)) {
      value = getVariableLocally(name);
    } else {
      VariableContainer parent = getParentVariableContainer();
      if (parent!=null) {
        // check upwards in the token hierarchy
        value = parent.getVariable(name);
      }
    } 
    return value;
  }
  
  public void setVariable(String name, Object value) {
    VariableContainer parent = getParentVariableContainer();
    if ( hasVariableLocally(name)
         || parent==null 
       ) {
      setVariableLocally(name, value);

    } else { 
      // so let's action to the parent token's TokenVariableMap
      parent.setVariable(name, value);
    }
  }

  public boolean hasVariable(String name) {
    boolean hasVariable = false;

    // if the variable is present in the variable instances
    if (hasVariableLocally(name)) {
      hasVariable = true;

    } else {
      VariableContainer parent = getParentVariableContainer();
      if (parent!=null) {
        hasVariable = parent.hasVariable(name);
      }
    }

    return hasVariable;
  }

  public void deleteVariable(String name) {
    if (name==null) {
      throw new JbpmException("name is null");
    }
    if (hasVariableLocally(name)) {
      deleteVariableLocally(name);
    }
  }

  /**
   * adds all the given variables to this variable container.
   * The method {@link #setVariables(Map)} is the same as this method, but 
   * it was added for naming consitency.
   */
  public void addVariables(Map<String, Object> variables) {
    setVariables(variables);
  }

  /**
   * adds all the given variables to this variable container.  It doesn't 
   * remove any existing variables unless they are overwritten by the given 
   * variables.
   * This method is the same as {@link #addVariables(Map)} and this method 
   * was added for naming consistency. 
   */
  public void setVariables(Map<String, Object> variables) {
    if (variables!=null) {
      for (Map.Entry<String, Object> entry : variables.entrySet()) {
        setVariable(entry.getKey(), entry.getValue());        
      }
    }
  }

  public Map<String, Object> getVariables() {
    Map<String, Object> variables = getVariablesLocally();
    VariableContainer parent = getParentVariableContainer();
    if (parent!=null) {
      Map<String, Object> parentVariables = parent.getVariablesLocally();
      parentVariables.putAll(variables);
      variables = parentVariables;
    }
    return variables;
  }

  public Map<String, Object> getVariablesLocally() {
    Map<String, Object> variables = new HashMap<String, Object>();
    if (variableInstances!=null) {
      for (Map.Entry<String, VariableInstance> entry : variableInstances.entrySet()) {
        String name = entry.getKey();
        VariableInstance variableInstance = entry.getValue();
        if (!variables.containsKey(name)) {
          variables.put(name, variableInstance.getValue());
        }        
      }
    }
    return variables;
  }

  // local variable methods ///////////////////////////////////////////////////

  public boolean hasVariableLocally(String name) {
    return ( (variableInstances!=null)
             && (variableInstances.containsKey(name))
           );
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
    if (name==null) {
      throw new JbpmException("name is null");
    }
    
    VariableInstance variableInstance = getVariableInstance(name);
    // if there is already a variable instance and it doesn't support the current type...
    if ( (variableInstance!=null) 
         && (!variableInstance.supports(value))
       ) {
      // delete the old variable instance
      log.debug("variable type change. deleting '"+name+"' from '"+this+"'");
      deleteVariableInstance(name);
      variableInstance = null;
    }

    if (variableInstance==null) {
      log.debug("create variable '"+name+"' in '"+this+"' with value '"+value+"'");
      variableInstance = VariableInstance.create(getToken(), name, value);
      addVariableInstance(variableInstance);
    } else {
      log.debug("update variable '"+name+"' in '"+this+"' to value '"+value+"'");
      variableInstance.setValue(value);
    }
  }

  // local variable instances /////////////////////////////////////////////////

  public VariableInstance getVariableInstance(String name) {
    return (variableInstances!=null ? (VariableInstance) variableInstances.get(name) : null);
  }
  
  public Map<String, VariableInstance> getVariableInstances() {
    return variableInstances;
  }

  public void addVariableInstance(VariableInstance variableInstance) {
    if (variableInstances==null) {
      variableInstances = new HashMap<String, VariableInstance>();
    }
    variableInstances.put(variableInstance.getName(), variableInstance);
    // only additions are registered in the updated variable containers 
    // because it is only used in the save operation to check wether there 
    // are unpersistable variables added
    addUpdatedVariableContainer();
  }

  public void deleteVariableInstance(String name) {
    if (variableInstances!=null) {
      VariableInstance variableInstance = variableInstances.remove(name);
      if (variableInstance!=null) {
        getToken().addLog(new VariableDeleteLog(variableInstance));
        variableInstance.removeReferences();
      }
    }
  }

  void addUpdatedVariableContainer(){
    ContextInstance contextInstance = getContextInstance();
    if (contextInstance!=null) {
      if (contextInstance.updatedVariableContainers==null) {
        contextInstance.updatedVariableContainers = new ArrayList<VariableContainer>();
      }
      contextInstance.updatedVariableContainers.add(this);
    }
  }
  
  public ContextInstance getContextInstance() {
    Token token = getToken();
    ProcessInstance processInstance = (token!=null ? token.getProcessInstance() : null);
    return (processInstance!=null ? processInstance.getContextInstance() : null);
  }
  
  public static Collection<VariableContainer> getUpdatedVariableContainers(ProcessInstance processInstance) {
    return processInstance.getContextInstance().updatedVariableContainers;
  }
  
  private static Log log = LogFactory.getLog(VariableContainer.class);
}
