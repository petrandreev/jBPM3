package org.jbpm.command;

import org.jbpm.JbpmContext;
import org.jbpm.graph.exe.Token;
import org.jbpm.util.Clock;

/**
 * Cancel a {@link Token}
 * 
 * @author Bernd Ruecker (bernd.ruecker@camunda.com)
 */
public class CancelTokenCommand extends AbstractCancelCommand {

  private static final long serialVersionUID = 7145293049356621597L;

  private long tokenId;

  public CancelTokenCommand() {
  }

  public CancelTokenCommand(long tokenId) {
    this.tokenId = tokenId;
  }

  public Object execute(JbpmContext jbpmContext) throws Exception {
    this.jbpmContext = jbpmContext;
    Token token = jbpmContext.getGraphSession().loadToken(tokenId);

    // create a token local process variable to indicate this token was canceled
    token.getProcessInstance().getContextInstance().createVariable(
        CANCELLATION_INDICATOR_VARIABLE_NAME, Clock.getCurrentTime(), token);

    cancelToken(token);
    this.jbpmContext = null;
    return null;
  }

  public long getTokenId() {
    return tokenId;
  }

  public void setTokenId(long tokenId) {
    this.tokenId = tokenId;
  }

  public String getAdditionalToStringInformation() {
    return "tokenId=" + tokenId;
  }

  // methods for fluent programming

  public CancelTokenCommand tokenId(long tokenId) {
    setTokenId(tokenId);
    return this;
  }
}