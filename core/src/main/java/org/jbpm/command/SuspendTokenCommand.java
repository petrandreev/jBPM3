package org.jbpm.command;

import org.jbpm.graph.exe.Token;

/**
 * Suspend the specified {@link Token}(s). See {@link AbstractTokenBaseCommand} to check
 * possibilities to specify {@link Token}(s).
 * 
 * @author bernd.ruecker@camunda.com
 */
public class SuspendTokenCommand extends AbstractTokenBaseCommand {

  private static final long serialVersionUID = 1L;

  public Object execute(Token token) {
    token.suspend();
    return token;
  }

}
