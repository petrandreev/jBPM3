package org.jbpm.command;

import org.jbpm.JbpmContext;

/**
 * provides extra configuration options for the execution of asynchronous commands. Plain commands can also be executed
 * asynchronously.
 */
public class AsynchronousCommand implements Command
{

  private static final long serialVersionUID = 1L;

  int retryCount = 1;

  Command command;

  public AsynchronousCommand(Command command)
  {
    this.command = command;
  }

  public Object execute(JbpmContext jbpmContext) throws Exception
  {
    throw new UnsupportedOperationException("sending any command over the message service is not yet supported");
  }

}
