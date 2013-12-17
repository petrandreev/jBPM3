package org.jbpm.command;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.hibernate.Query;

import org.jbpm.JbpmContext;
import org.jbpm.graph.exe.ProcessInstance;

/**
 * This command can retrieve all process instances (e.g. for admin client). You have the possibility
 * to filter the command, therefor use the available attributes
 * 
 * @author Bernd Ruecker (bernd.ruecker@camunda.com)
 */
public class GetProcessInstancesCommand extends AbstractGetObjectBaseCommand {

  private static final long serialVersionUID = -5601050489405283851L;

  /*
   * is true, only the running process instances are retrieved (ended and canceled ones are skipped)
   */
  private boolean onlyRunning = true;

  /*
   * if given, only processes with start date >= given date are shown
   */
  private Date fromStartDate;

  /*
   * if given, only processes with start date <= given date are shown
   */
  private Date untilStartDate;

  /*
   * if given, only processes with this name are retrieved
   */
  private String processDefinitionName;

  private long processInstanceId = -1;
  /*
   * if given, only processes with this name are retrieved
   */
  private String stateName;

  private String version = null;

  private transient boolean firstExpression = true;

  private String getConcatExpression() {
    if (firstExpression) {
      firstExpression = false;
      return " where ";
    }
    return " and ";
  }

  public Object execute(JbpmContext jbpmContext) throws Exception {
    setJbpmContext(jbpmContext);
    firstExpression = true;
    StringBuffer queryText = new StringBuffer("select pi"
        + " from org.jbpm.graph.exe.ProcessInstance as pi ");

    if (onlyRunning) {
      queryText.append(getConcatExpression()).append(" pi.end = null");
    }

    if (fromStartDate != null) {
      queryText.append(getConcatExpression()).append(" pi.start >= :from ");
    }
    if (untilStartDate != null) {
      queryText.append(getConcatExpression()).append(" pi.start <= :until ");
    }
    if (version != null) {
      queryText.append(getConcatExpression()).append(" pi.version = :version ");
    }

    // name
    if (processInstanceId != -1) {
      queryText.append(getConcatExpression()).append(" pi.processDefinition.id = :processId  ");
    }
    else if (processDefinitionName != null && processDefinitionName.length() > 0) {
      queryText.append(getConcatExpression()).append(
          " pi.processDefinition.name = :processDefinitionName  ");
    }

    // TODO: this code only fecthes root tokens, child-tokens has to be
    // considered too!
    if (stateName != null && stateName.length() > 0) {
      queryText.append(getConcatExpression()).append(" pi.rootToken.node.name = :nodeName ");
    }

    queryText.append(" order by pi.start desc");

    Query query = jbpmContext.getSession().createQuery(queryText.toString());

    if (fromStartDate != null) {
      query.setTimestamp("from", fromStartDate);
    }
    if (untilStartDate != null) {
      query.setTimestamp("until", untilStartDate);
    }

    if (processInstanceId != -1) {
      query.setLong("processId", processInstanceId);
    }
    if (processDefinitionName != null && processDefinitionName.length() > 0) {
      query.setString("processDefinitionName", processDefinitionName);
    }

    if (stateName != null && stateName.length() > 0) {
      query.setString("nodeName", stateName);
    }

    if (version != null) {
      query.setString("version", version);
    }

    return retrieveProcessInstanceDetails(query.list());
  }

  /**
   * access everything on all processInstance objects, which is not in the default fetch group from
   * hibernate, but needs to be accesible from the client overwrite this, if you need more details
   * in your client
   */
  public List retrieveProcessInstanceDetails(List processInstanceList) {
    Iterator it = processInstanceList.iterator();
    while (it.hasNext()) {
      retrieveProcessInstance((ProcessInstance) it.next());
    }
    return processInstanceList;
  }

  public Date getFromStartDate() {
    return fromStartDate;
  }

  public void setFromStartDate(Date fromStartDate) {
    this.fromStartDate = fromStartDate;
  }

  public boolean isOnlyRunning() {
    return onlyRunning;
  }

  public void setOnlyRunning(boolean onlyRunning) {
    this.onlyRunning = onlyRunning;
  }

  /**
   * @deprecated
   */
  public String getProcessName() {
    return processDefinitionName;
  }

  /**
   * @deprecated
   */
  public void setProcessName(String processName) {
    this.processDefinitionName = processName;
  }

  public String getProcessDefinitionName() {
    return processDefinitionName;
  }

  public void setProcessDefinitionName(String processName) {
    this.processDefinitionName = processName;
  }

  public String getStateName() {
    return stateName;
  }

  public void setStateName(String stateName) {
    this.stateName = stateName;
  }

  public Date getUntilStartDate() {
    return untilStartDate;
  }

  public void setUntilStartDate(Date untilStartDate) {
    this.untilStartDate = untilStartDate;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public long getProcessInstanceId() {
    return processInstanceId;
  }

  public void setProcessInstanceId(long processId) {
    this.processInstanceId = processId;
  }

  /**
   * @deprecated
   */
  public long getProcessId() {
    return processInstanceId;
  }

  /**
   * @deprecated
   */
  public void setProcessId(long processId) {
    this.processInstanceId = processId;
  }

  public String getAdditionalToStringInformation() {
    return "processInstanceId="
        + processInstanceId
        + ";processDefinitionName="
        + processDefinitionName
        + ";version="
        + version
        + ";stateName="
        + stateName
        + ";fromStartDate="
        + fromStartDate
        + ";untilStartDate="
        + untilStartDate
        + ";onlyRunning="
        + onlyRunning;
  }

  // methods for fluent programming

  public GetProcessInstancesCommand fromStartDate(Date fromStartDate) {
    setFromStartDate(fromStartDate);
    return this;
  }

  public GetProcessInstancesCommand onlyRunning(boolean onlyRunning) {
    setOnlyRunning(onlyRunning);
    return this;
  }

  public GetProcessInstancesCommand processDefinitionName(String processName) {
    setProcessDefinitionName(processName);
    return this;
  }

  public GetProcessInstancesCommand stateName(String stateName) {
    setStateName(stateName);
    return this;
  }

  public GetProcessInstancesCommand untilStartDate(Date untilStartDate) {
    setUntilStartDate(untilStartDate);
    return this;
  }

  public GetProcessInstancesCommand version(String version) {
    setVersion(version);
    return this;
  }

  public GetProcessInstancesCommand processInstanceId(long processId) {
    setProcessInstanceId(processId);
    return this;
  }
}
