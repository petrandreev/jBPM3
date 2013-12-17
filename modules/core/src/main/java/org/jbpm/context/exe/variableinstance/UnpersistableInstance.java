package org.jbpm.context.exe.variableinstance;

import org.jbpm.context.exe.VariableInstance;

/**
 * uses the cache in variable instance to store any object
 * without persisting it.
 */
public class UnpersistableInstance extends VariableInstance {
  
  private static final long serialVersionUID = 1L;

  public boolean isStorable(Object value) {
    return true;
  }

  protected Object getObject() {
    return null;
  }

  protected void setObject(Object value) {
  }
}
