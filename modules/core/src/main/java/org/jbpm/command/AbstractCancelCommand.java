package org.jbpm.command;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Query;
import org.jbpm.JbpmContext;
import org.jbpm.graph.exe.Token;
import org.jbpm.taskmgmt.exe.TaskInstance;

public abstract class AbstractCancelCommand extends AbstractBaseCommand
{

  private static final long serialVersionUID = 1L;

  /**
   * Name of a standardized process variable which is written during cancellation
   * in order to indicate that this process has been 'canceled' and not just ended.
   * Value of the variable is the timestamp of cancellation.
   */
  public static final String CANCELLATION_INDICATOR_VARIABLE_NAME = "canceled";

  protected transient JbpmContext jbpmContext = null;

  protected static final Log log = LogFactory.getLog(AbstractCancelCommand.class);

  protected void cancelTokens(Collection tokens)
  {
    if (tokens != null && tokens.size() > 0)
    {
      log.info("cancel " + tokens.size() + " tokens");
      for (Iterator itr = tokens.iterator(); itr.hasNext();)
      {
        cancelToken((Token)itr.next());
      }
    }
  }

  protected void cancelToken(Token token)
  {
    token.end(false); // end the token but dont verify
    // ParentTermination
    // if set to token.end() == token.end(true) the parent token is
    // terminated if there are no children
    // If we then use that in a "SignalingJoin" the main path of execution
    // is triggered, but we dont want that!

    // Recursively cancel children
    cancelTokens(token.getChildren().values());
    
    // cancel tasks
    cancelTasks(getTasksForToken(token));

    log.info("token " + token.getId() + " canceled");
  }

  protected List getTasksForToken(Token token)
  {
    Query hqlQuery = jbpmContext.getSession().getNamedQuery("TaskMgmtSession.findTaskInstancesByTokenId");
    hqlQuery.setLong("tokenId", token.getId());
    return hqlQuery.list();
  }

  protected void cancelTasks(List tasks)
  {
    if (tasks != null && tasks.size() > 0)
    {
      log.info("cancel " + tasks.size() + " tasks");
      for (Iterator it = tasks.iterator(); it.hasNext();)
      {
        TaskInstance ti = (TaskInstance)it.next();

        // if the process def doesn't set signal="never", we have to
        // manually turn off signaling for all tasks;
        // otherwise, the token will be triggered instead of being
        // ended.
        // Do this until http://jira.jboss.com/jira/browse/JBPM-392 is
        // resolved

        log.info("cancel task " + ti.getId());
        ti.setSignalling(false);
        ti.cancel();
      }
    }
  }

}