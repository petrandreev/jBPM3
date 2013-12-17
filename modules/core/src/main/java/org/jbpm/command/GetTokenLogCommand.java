package org.jbpm.command;

import java.util.Iterator;
import java.util.List;

import org.jbpm.JbpmContext;
import org.jbpm.logging.log.ProcessLog;

/**
 * Retrieve the {@link ProcessLog} for the token with the given tokenId
 * 
 * returns a {@link List} with {@link ProcessLog}s.
 * 
 * @author Bernd Ruecker (bernd.ruecker@camunda.com)
 */
public class GetTokenLogCommand extends AbstractBaseCommand
{

  private static final long serialVersionUID = 1L;

  private long tokenId;

  public GetTokenLogCommand()
  {
  }

  public GetTokenLogCommand(long tokenId)
  {
    this.tokenId = tokenId;
  }

  public Object execute(JbpmContext jbpmContext) throws Exception
  {
    List logList = jbpmContext.getLoggingSession().findLogsByToken(tokenId);
    return loadLogFromList(logList);
  }

  /**
   * access everything on all ProcessLog objects, which is not in 
   * the default fetch group from hibernate, but needs to
   * be accessible from the client
   * 
   * overwrite this method, if you need more details in your client
   */
  protected List loadLogFromList(List logList)
  {
    Iterator iter = logList.iterator();
    while (iter.hasNext())
    {
        ProcessLog pl = (ProcessLog)iter.next();        
        retrieveProcessLog(pl);
    }
    return logList;
  }

  public void retrieveProcessLog(ProcessLog pl)
  {
    pl.toString(); // to string accesses important fields
    pl.getToken().getId();
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
  
  public GetTokenLogCommand tokenId(long tokenId)
  {
    setTokenId(tokenId);
    return this;
  }
}
