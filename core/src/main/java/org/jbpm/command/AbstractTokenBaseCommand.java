package org.jbpm.command;

import java.util.ArrayList;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Query;

import org.jbpm.JbpmContext;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.Token;
import org.jbpm.util.ArrayUtil;

/**
 * Abstract base class for commands working on Tokens. The {@link Token} can either be specified
 * by id or multiple ids. The alternative is to specify a {@link ProcessDefinition} name, a
 * required node name and version. In this case <b>all</b> found {@link Token}s are processed.
 * If no version is specified, <b>all</b> versions are taken into account.
 * 
 * @author bernd.ruecker@camunda.com
 */
public abstract class AbstractTokenBaseCommand implements Command {

  protected final Log log = LogFactory.getLog(getClass());

  private long[] tokenIds;
  private String processName;
  private String stateName;
  private int processVersion;

  private boolean operateOnSingleObject;

  private transient JbpmContext jbpmContext;

  private static final long serialVersionUID = 1L;

  protected JbpmContext getJbpmContext() {
    return jbpmContext;
  }

  public Object execute(JbpmContext jbpmContext) throws Exception {
    this.jbpmContext = jbpmContext;
    try {
      ArrayList result = new ArrayList();
      // batch tokens
      if (tokenIds != null && tokenIds.length > 0) {
        for (int i = 0; i < tokenIds.length; i++) {
          Token token = jbpmContext.loadTokenForUpdate(tokenIds[i]);
          result.add(execute(token));
        }
      }

      // search for tokens in process/state
      if (processName != null && stateName != null) {
        operateOnSingleObject = false;

        Query query;
        if (processVersion > 0) {
          query = jbpmContext.getSession()
            .getNamedQuery("GraphSession.findTokensForProcessVersionInNode");
          query.setInteger("processDefinitionVersion", processVersion);
        }
        else {
          query = jbpmContext.getSession()
            .getNamedQuery("GraphSession.findTokensForProcessInNode");
        }
        query.setString("processDefinitionName", processName);
        query.setString("nodeName", stateName);

        for (Iterator iter = query.list().iterator(); iter.hasNext();) {
          Token token = (Token) iter.next();
          result.add(execute(token));
        }
      }

      if (operateOnSingleObject) {
        if (result.size() < 1)
          return null;
        else
          return result.get(0);
      }
      else {
        return result;
      }
    }
    finally {
      this.jbpmContext = null;
    }
  }

  public abstract Object execute(Token token);

  public void setTokenIds(long[] tokenIds) {
    operateOnSingleObject = false;
    this.tokenIds = tokenIds;
  }

  public void setTokenId(long tokenId) {
    operateOnSingleObject = true;
    tokenIds = new long[1];
    tokenIds[0] = tokenId;
  }

  public String getAdditionalToStringInformation() {
    return "";
  }

  public String getProcessName() {
    return processName;
  }

  public void setProcessName(String processName) {
    this.processName = processName;
  }

  public int getProcessVersion() {
    return processVersion;
  }

  public void setProcessVersion(int processVersion) {
    this.processVersion = processVersion;
  }

  public String getStateName() {
    return stateName;
  }

  public void setStateName(String stateName) {
    this.stateName = stateName;
  }

  public long[] getTokenIds() {
    return tokenIds;
  }

  /**
   * return the token id in case only one token id is set. Otherwise an
   * {@link IllegalStateException} is thrown
   */
  public long getTokenId() {
    if (tokenIds == null || tokenIds.length != 1) {
      throw new IllegalStateException("multiple token ids set: " + ArrayUtil.toString(tokenIds));
    }
    return tokenIds[0];
  }

  public String toString() {
    if (processName != null && stateName != null) {
      return getClass().getName() + " [tokenIds=" + ArrayUtil.toString(tokenIds)
        + ";processName=" + processName + ";processVersion="
        + (processVersion > 0 ? Integer.toString(processVersion) : "NA") + ";stateName="
        + stateName + getAdditionalToStringInformation() + "]";
    }
    else {
      return getClass().getName() + " [tokenIds=" + ArrayUtil.toString(tokenIds)
        + ";operateOnSingleObject=" + operateOnSingleObject
        + getAdditionalToStringInformation() + "]";
    }
  }

  // methods for fluent programming

  public AbstractTokenBaseCommand tokenIds(long[] tokenIds) {
    setTokenIds(tokenIds);
    return this;
  }

  public AbstractTokenBaseCommand tokenId(long tokenId) {
    setTokenId(tokenId);
    return this;
  }

  public AbstractTokenBaseCommand processName(String processName) {
    setProcessName(processName);
    return this;
  }

  public AbstractTokenBaseCommand processVersion(int processVersion) {
    setProcessVersion(processVersion);
    return this;
  }

  public AbstractTokenBaseCommand stateName(String stateName) {
    setStateName(stateName);
    return this;
  }
}