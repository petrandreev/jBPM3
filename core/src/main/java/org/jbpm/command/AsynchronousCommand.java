package org.jbpm.command;

import org.jbpm.JbpmContext;

/**
 * provides extra configuration options for the execution of asynchronous
 * commands. Plain commands can also be executed asynchronously.
 * 
 * @deprecated asynchronous commands were never implemented
 */
public class AsynchronousCommand implements Command {

  private static final long serialVersionUID = 1L;

  Command command;
  int retryCount = 1;

  public AsynchronousCommand(Command command) {
    this.command = command;
  }

  public Object execute(JbpmContext jbpmContext) throws Exception {
    throw new UnsupportedOperationException(
      "sending commands over message service is not yet supported");
  }

}
