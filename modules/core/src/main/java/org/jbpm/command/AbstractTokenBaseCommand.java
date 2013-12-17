package org.jbpm.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.criterion.Restrictions;
import org.jbpm.JbpmContext;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.Token;
import org.jbpm.util.CollectionUtil;

/**
 * Abstract base class for commands working on Tokens. The {@link Token} can either be specified by
 * id or multiple ids. The alternative is to specify a {@link ProcessDefinition} name, a required
 * node name and version. In this case <b>all</b> found {@link Token}s are processed. If no version
 * is specified, <b>all</b> versions are taken into account.
 * 
 * <b>Attention:</b> The result of the command is either a List of {@link Token}s
 * or one single {@link Token}, depending if this command operates on one single instance
 * (indicated by "operateOnSingleObject").
 * 
 * @author bernd.ruecker@camunda.com
 */
public abstract class AbstractTokenBaseCommand implements Command
{
  protected Log log = LogFactory.getLog(this.getClass());

  private long[] tokenIds = null;
  private String processName = null;
  private String stateName = null;
  private int processVersion = 0;
  
  /**
   * indicator if exactly one token is targeted with the command
   */
  private boolean operateOnSingleObject;

  private transient JbpmContext jbpmContext;

  private static final long serialVersionUID = 1L;

  public AbstractTokenBaseCommand()
  {
    super();
  }

  protected JbpmContext getJbpmContext()
  {
    return jbpmContext;
  }

  public Object execute(JbpmContext jbpmContext) throws Exception
  {
    this.jbpmContext = jbpmContext;
    try
    {
      ArrayList result = new ArrayList();
      log.debug("executing " + this);

      // batch tokens
      if (tokenIds != null && tokenIds.length > 0)
      {
        for (int i = 0; i < tokenIds.length; i++)
        {
          Token token = jbpmContext.loadTokenForUpdate(tokenIds[i]);
          result.add(execute(token));
        }
      }
      else 
      {
        // search for tokens depending on query parameters
        this.operateOnSingleObject = false;
        
        Criteria criteria = jbpmContext.getSession().createCriteria(Token.class);
  
        if (processName != null)
        {
          criteria.createCriteria("processInstance")
            .createCriteria("processDefinition")
            .add(Restrictions.eq("name", processName));
        }
        if (processVersion>0)
        {
          criteria.createCriteria("processInstance")
            .createCriteria("processDefinition")
            .add(Restrictions.eq("version", processVersion));
        }
        if (stateName != null)
        {
          criteria.createCriteria("node")
            .add(Restrictions.eq("name", stateName));
        }
        
        List<?> queryResult = criteria.list();
        CollectionUtil.checkList(queryResult, Token.class);
        
        for (Token token : (List<Token>)queryResult)
        {
          result.add(execute(token));
        }
      }

      if (operateOnSingleObject)
      {
        if (result.size() < 1)
          return null;
        else
          return result.get(0);
      }
      else
      {
        return result;
      }
    }
    finally
    {
      this.jbpmContext = null;
    }
  }

  public abstract Object execute(Token token);

  public void setTokenIds(long[] tokenIds)
  {
    this.operateOnSingleObject = false;
    this.tokenIds = tokenIds;
  }

  public void setTokenId(long tokenId)
  {
    this.operateOnSingleObject = true;
    this.tokenIds = new long[1];
    this.tokenIds[0] = tokenId;
  }

  public String getAdditionalToStringInformation()
  {
    return "";
  }

  public String getProcessName()
  {
    return processName;
  }

  public void setProcessName(String processName)
  {
    this.processName = processName;
  }

  public int getProcessVersion()
  {
    return processVersion;
  }

  public void setProcessVersion(int processVersion)
  {
    this.processVersion = processVersion;
  }

  public String getStateName()
  {
    return stateName;
  }

  public void setStateName(String stateName)
  {
    this.stateName = stateName;
  }

  public long[] getTokenIds()
  {
    return tokenIds;
  }

  /**
   * return the process instance id in case only one process instance id is set. Otherwise an
   * {@link IllegalStateException} is thrown
   */
  public long getTokenId()
  {
    if (tokenIds == null || tokenIds.length != 1)
    {
      throw new IllegalStateException("exactly one token id must be set on "
          + this
          + " to get tokenId property; found "
          + Arrays.toString(tokenIds));
    }
    return tokenIds[0];
  }

  public String toString()
  {
    if (processName != null && stateName != null)
    {
      return this.getClass().getName()
          + " [tokenIds="
          + Arrays.toString(tokenIds)
          + ";processName="
          + processName
          + ";processVersion="
          + (processVersion > 0 ? processVersion : "NA")
          + ";stateName="
          + stateName
          + getAdditionalToStringInformation()
          + "]";
    }
    else
    {
      return this.getClass().getName()
          + " [tokenIds="
          + Arrays.toString(tokenIds)
          + ";operateOnSingleObject="
          + operateOnSingleObject
          + getAdditionalToStringInformation()
          + "]";
    }
  }

  // methods for fluent programming

  public AbstractTokenBaseCommand tokenIds(long[] tokenIds)
  {
    setTokenIds(tokenIds);
    return this;
  }

  public AbstractTokenBaseCommand tokenId(long tokenId)
  {
    setTokenId(tokenId);
    return this;
  }

  public AbstractTokenBaseCommand processName(String processName)
  {
    setProcessName(processName);
    return this;
  }

  public AbstractTokenBaseCommand processVersion(int processVersion)
  {
    setProcessVersion(processVersion);
    return this;
  }

  public AbstractTokenBaseCommand stateName(String stateName)
  {
    setStateName(stateName);
    return this;
  }

  public boolean isOperateOnSingleObject()
  {
    return operateOnSingleObject;
  }  
}