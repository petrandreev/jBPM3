package org.jbpm.command;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbpm.JbpmContext;
import org.jbpm.graph.exe.Token;
import org.jbpm.taskmgmt.exe.TaskInstance;

@SuppressWarnings({
  "rawtypes"
})
public abstract class AbstractCancelCommand extends AbstractBaseCommand {

  private static final long serialVersionUID = 1L;

  /**
   * Name of a standardized process variable written during cancellation in order to indicate
   * that this process has been 'canceled' and not just ended. The variable value is the
   * cancellation timestamp.
   */
  public final static String CANCELLATION_INDICATOR_VARIABLE_NAME = "canceled";

  protected transient JbpmContext jbpmContext;

  protected static final Log log = LogFactory.getLog(AbstractCancelCommand.class);

  protected void cancelTokens(Collection tokens) {
    if (tokens != null && !tokens.isEmpty()) {
      boolean debug = log.isDebugEnabled();
      if (debug) log.debug("cancelling " + tokens.size() + " tokens");

      for (Iterator itr = tokens.iterator(); itr.hasNext();) {
        Token token = (Token) itr.next();
        if (debug) log.debug("cancelling " + token);
        cancelToken(token);
      }
    }
  }

  protected void cancelToken(Token token) {
    // recursively cancel children
    cancelTokens(token.getChildren().values());

    // cancel tasks
    cancelTasks(getTasksForToken(token));

    if (!token.hasEnded()) {
      // end token but do not propagate to parent
      // to prevent inadvertent termination
      token.end(false);
    }
    if (log.isDebugEnabled()) log.debug("cancelled " + token);
  }

  protected List getTasksForToken(Token token) {
    return jbpmContext.getSession()
      .getNamedQuery("TaskMgmtSession.findTaskInstancesByTokenId")
      .setLong("tokenId", token.getId())
      .list();
  }

  protected void cancelTasks(List tasks) {
    if (tasks != null && !tasks.isEmpty()) {
      boolean debug = log.isDebugEnabled();
      if (debug) log.debug("cancelling " + tasks.size() + " tasks");

      for (Iterator it = tasks.iterator(); it.hasNext();) {
        TaskInstance ti = (TaskInstance) it.next();
        if (debug) log.debug("cancelling " + ti);
        // manually turn off signaling for task instance,
        // otherwise it may signal its associated token
        ti.setSignalling(false);
        ti.cancel();
      }
    }
  }

}