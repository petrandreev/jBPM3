package org.jbpm.svc;

import org.jbpm.JbpmException;

public class JbpmServiceException extends JbpmException {

  private static final long serialVersionUID = 1L;

  public JbpmServiceException() {
    super();
  }
  public JbpmServiceException(String message, Throwable cause) {
    super(message, cause);
  }
  public JbpmServiceException(String message) {
    super(message);
  }
  public JbpmServiceException(Throwable cause) {
    super(cause);
  }
}
