package org.jbpm.command;

import java.util.Date;

import org.jbpm.graph.exe.Token;

/**
 * a bunch of processes is signaled with this command. you can specify the tokens either <li>by
 * a array of token ids <li>or by processName, processVersion (optional, without all versions),
 * stateName transitionName specifies the transition to take (if null, the default transition is
 * taken).
 * 
 * @author Bernd Ruecker (bernd.ruecker@camunda.com)
 */
public class BatchSignalCommand extends AbstractTokenBaseCommand {

  private static final long serialVersionUID = -4330623193546102772L;

  /**
   * if set, only tokens which are started after this date are signaled (interesting to
   * implement some timeout for example)
   */
  private Date inStateAtLeastSince;

  private String transitionName;

  public Object execute(Token token) {
    if (inStateAtLeastSince == null || token.getNodeEnter().before(inStateAtLeastSince)) {

      if (log.isDebugEnabled()) log.debug("signalling " + token);
      if (transitionName == null) {
        token.signal();
      }
      else {
        token.signal(transitionName);
      }
    }
    return token;
  }

  public String getTransitionName() {
    return transitionName;
  }

  public void setTransitionName(String transitionName) {
    this.transitionName = transitionName;
  }

  public Date getInStateAtLeastSince() {
    return inStateAtLeastSince;
  }

  public void setInStateAtLeastSince(Date inStateAtLeastSince) {
    this.inStateAtLeastSince = inStateAtLeastSince;
  }

  public String getAdditionalToStringInformation() {
    return ";transitionName=" + transitionName + ";inStateAtLeastSince=" + inStateAtLeastSince;
  }

  // methods for fluent programming

  public BatchSignalCommand transitionName(String transitionName) {
    setTransitionName(transitionName);
    return this;
  }

  public BatchSignalCommand inStateAtLeastSince(Date inStateAtLeastSince) {
    setInStateAtLeastSince(inStateAtLeastSince);
    return this;
  }
}
