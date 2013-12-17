package org.jbpm.command;

import org.jbpm.JbpmContext;
import org.jbpm.graph.exe.Token;

/**
 * Return the token with the specified tokenId
 * 
 * @author Bernd Ruecker (bernd.ruecker@camunda.com)
 */
public class GetTokenCommand extends AbstractGetObjectBaseCommand
{

  private static final long serialVersionUID = 1L;

  private long tokenId;

  public GetTokenCommand()
  {
  }

  public Object execute(JbpmContext jbpmContext) throws Exception
  {
    setJbpmContext(jbpmContext);
    Token token = jbpmContext.getToken(tokenId);
    retrieveToken(token);
    return token;
  }

  public long getTokenId()
  {
    return tokenId;
  }

  public void setTokenId(long tokenId)
  {
    this.tokenId = tokenId;
  }

  @Override
  public String getAdditionalToStringInformation()
  {
    return "tokenId=" + tokenId;
  }

  // methods for fluent programming

  public GetTokenCommand tokenId(long tokenId)
  {
    setTokenId(tokenId);
    return this;
  }
}
