package org.jbpm.command;

import org.jbpm.graph.exe.Token;

/**
 * Resume the specified {@link Token}(s). See {@link AbstractTokenBaseCommand} to check
 * possibilities to specify {@link Token}(s).
 * 
 * @author bernd.ruecker@camunda.com
 */
public class ResumeTokenCommand extends AbstractTokenBaseCommand {

  private static final long serialVersionUID = 1L;

  public Object execute(Token token) {
    token.resume();
    return token;
  }

}
