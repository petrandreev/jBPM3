package org.jbpm.context.exe.variableinstance;

import org.jbpm.context.exe.VariableInstance;

public class NullInstance extends VariableInstance {

  private static final long serialVersionUID = 1L;

  public boolean isStorable(Object value) {
    return value == null;
  }

  protected Object getObject() {
    return null;
  }

  protected void setObject(Object value) {
  }
}
