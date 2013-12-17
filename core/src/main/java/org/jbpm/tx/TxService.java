package org.jbpm.tx;

import org.jbpm.svc.Service;

public class TxService implements Service {

  private static final long serialVersionUID = 1L;
  
  boolean isRollbackOnly = false;

  public void close() {
  }

  public boolean isRollbackOnly() {
    return isRollbackOnly;
  }
  public void setRollbackOnly() {
    isRollbackOnly = true;
  }
}
