package org.jbpm.sim.bam;

import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;

import org.hibernate.Query;
import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.jbpm.command.GetProcessDefinitionCommand;
import org.jbpm.graph.def.ProcessDefinition;

/**
 * This command can read jbpm log data and propose
 * simulation inputs based on this data.
 * 
 * The following informations are gathered:
 * <ul>
 *   <li>process starting event distribution</li>
 *   <li>outgoing probabilities (decision, state & task instance)</li>
 *   <li>time distribution for states and task instances</li>
 * </ul>
 * 
 * <b>Open Todos at the moment</b>
 * <ul>
 *   <li>activate time limits again (problem with HSQL at the moment)</li>
 *   <li>support more time frames (e.g. to easily support excluding lunch)</li>
 *   <li>support super states</li>
 *   <li>support sub processes</li>
 *   <li>fix query for process start event intervals: There are still some problems with that query: <a href="http://jira.jboss.com/jira/browse/JBPM-1082">JIRA 1082</a></li>
 * </ul>
 * 
 * @author bernd.ruecker@camunda.com
 */
public class GetSimulationInputCommand extends GetProcessDefinitionCommand {

  private static final long serialVersionUID = 1L;

  /**
   * if set, only information <b>later</b> than the given time 
   * of day is used. Only the time part is used!
   * 
   * This is useful, if you want to look only at one shift
   * (for example from 08:00 till 17:00) and is used normally
   * together with field <code>tillTimeOfDay</code>
   * 
   * Be patient, this may be not really implemented for your special database. Look at 
   * <a href="http://jira.jboss.com/jira/browse/JBPM-1080">JIRA JBPM-1080</a> for more information
   */
  private Date fromTimeOfDay;
  
  /**
   * see <code>fromTimeOfDay</code>
   */
  private Date tillTimeOfDay;
  
  public GetSimulationInputCommand() {     
    super();
  }

  public GetSimulationInputCommand(String name) {
    super(name);
  }
  
  public GetSimulationInputCommand(String name, int version) {
    super(name, version);
  }
  
  public Object execute(JbpmContext jbpmContext) throws Exception {   
    ProcessDefinition pd = (ProcessDefinition) super.execute(jbpmContext);
    BamSimulationProposal result = new BamSimulationProposal(getName(), getVersion());
    
    result.setSwimlanes(
        (String[])pd.getTaskMgmtDefinition().getSwimlanes().keySet().toArray(new String[0]));
        
    queryProcessStatistics(jbpmContext, pd, result);

    queryStateStatistics(jbpmContext, pd, result);
    queryDecisionStatistics(jbpmContext, pd, result);
    queryTaskInstanceStatistics(jbpmContext, pd, result);
    
    queryDecisionStatistics(jbpmContext, pd, result);
    
//    long[] timeBetweenStartEvents;

    return result;
  }
  

  private void queryProcessStatistics(JbpmContext jbpmContext, ProcessDefinition pd, BamSimulationProposal result) {
    Query decisionQuery = jbpmContext.getSession().getNamedQuery("Simulation.calculateProcessInstanceStartInterval");
  decisionQuery.setTime("fromTime", fromTimeOfDay);
  decisionQuery.setTime("tillTime", tillTimeOfDay);
    decisionQuery.setLong("processDefinitionId", pd.getId());
    
    Object[] o = (Object[]) decisionQuery.uniqueResult();

    if (o!=null) {
      ElementStatistics statistics = new ElementStatistics(pd.getName());
      
      statistics.setSampleCount( ((Long)o[0]).longValue() );
      statistics.setDurationAverage( ((Double)o[1]).doubleValue() );
      statistics.setDurationMin( ((Double)o[2]).longValue() );
      statistics.setDurationMax( ((Double)o[3]).longValue() );
      statistics.setDurationStddev( ((Double)o[4]).doubleValue() );
      
      result.setProcessStatistics(statistics);
    }
    else 
      throw new JbpmException("no logs found for process " + pd);
  }

  private void queryDecisionStatistics(JbpmContext jbpmContext, ProcessDefinition pd, BamSimulationProposal result) {
    Query decisionQuery = jbpmContext.getSession().getNamedQuery("Simulation.calculateAverageTimeForDecisions");
    decisionQuery.setTime("fromTime", fromTimeOfDay);
    decisionQuery.setTime("tillTime", tillTimeOfDay);
    decisionQuery.setEntity("processDefinition", pd);
    
    Iterator decisionStatisticsIterator = decisionQuery.list().iterator();
    while (decisionStatisticsIterator.hasNext()) {
      Object[] o = (Object[]) decisionStatisticsIterator.next();
      
      long decisionId = ((Long) o[0]).longValue();    
      String decisionName = (String) o[1];    
      ElementStatistics statistics = new ElementStatistics(decisionName);
      
      statistics.setSampleCount( ((Long)o[2]).longValue() );
      statistics.setDurationAverage( ((Double)o[3]).doubleValue() );
      statistics.setDurationMin( ((Long)o[4]).longValue() );
      statistics.setDurationMax( ((Long)o[5]).longValue() );
      statistics.setDurationStddev( ((Double)o[6]).doubleValue() );
      
      queryTransitionProbabilities(jbpmContext, decisionId, statistics);
      
      result.addDecisionProposal(statistics);
    }
  }  

  private void queryStateStatistics(JbpmContext jbpmContext, ProcessDefinition pd, BamSimulationProposal result) {
    Query nodeQuery = null;
    
    nodeQuery = jbpmContext.getSession().getNamedQuery("Simulation.calculateAverageTimeForStates");
    
    nodeQuery.setTime("fromTime", fromTimeOfDay);
    nodeQuery.setTime("tillTime", tillTimeOfDay);
    nodeQuery.setEntity("processDefinition", pd);
    
    Iterator nodeStatisticsIterator = nodeQuery.list().iterator();
    while (nodeStatisticsIterator.hasNext()) {
      Object[] o = (Object[]) nodeStatisticsIterator.next();
      
      long stateId = ((Long) o[0]).longValue();    
      String nodeName = (String) o[1];    
      ElementStatistics statistics = new ElementStatistics(nodeName);

      statistics.setSampleCount( ((Long)o[2]).longValue() );
      statistics.setDurationAverage( ((Double)o[3]).doubleValue() );
      statistics.setDurationMin( ((Long)o[4]).longValue() );
      statistics.setDurationMax( ((Long)o[5]).longValue() );
      statistics.setDurationStddev( ((Double)o[6]).doubleValue() );
            
      queryTransitionProbabilities(jbpmContext, stateId, statistics);
      
      result.addStateProposal(statistics);
    }
  }
  
  private void queryTaskInstanceStatistics(JbpmContext jbpmContext, ProcessDefinition pd, BamSimulationProposal result) {
    Query nodeQuery = null;
    
    nodeQuery = jbpmContext.getSession().getNamedQuery("Simulation.calculateAverageTimeForTaskInstances");

    nodeQuery.setTime("fromTime", fromTimeOfDay);
    nodeQuery.setTime("tillTime", tillTimeOfDay);
    nodeQuery.setLong("processDefinitinId", pd.getId());
    
    Iterator nodeStatisticsIterator = nodeQuery.list().iterator();
    while (nodeStatisticsIterator.hasNext()) {
      Object[] o = (Object[]) nodeStatisticsIterator.next();
      
      long taskNodeId = ((Long) o[0]).longValue();    
      String taskName = (String) o[2];    
      ElementStatistics statistics = new ElementStatistics(taskName);

      statistics.setSampleCount( ((Long)o[3]).longValue() );
      statistics.setDurationAverage( ((Double)o[4]).doubleValue() );
      statistics.setDurationMin( ((Double)o[5]).doubleValue() );
      statistics.setDurationMax( ((Double)o[6]).doubleValue() );
      statistics.setDurationStddev( ((Double)o[7]).doubleValue() );
            
      queryTransitionProbabilities(jbpmContext, taskNodeId, statistics);
      
      result.addTaskProposal(statistics);
    }
  }  

  private void queryTransitionProbabilities(JbpmContext jbpmContext,
      long nodeId, ElementStatistics statistics) {
    Query transQuery = jbpmContext.getSession().getNamedQuery("Simulation.calculateLeavingTransitionProbability");
    transQuery.setLong("nodeId", nodeId);
    transQuery.setTime("fromTime", fromTimeOfDay);
    transQuery.setTime("tillTime", tillTimeOfDay);
    
    Iterator transitionProbabiliyIterator = transQuery.list().iterator();
    while (transitionProbabiliyIterator.hasNext()) {
      Object[] probs = (Object[]) transitionProbabiliyIterator.next();
      
      String transitionName = (String) probs[1];
      long count = ((Long)probs[4]).longValue();        
      statistics.addTransitionProbability( new TransitionProbability( transitionName,  count ));
    }
  }
  
  /**
   * I used this testcode to test a bit locally
   * Shoud be deleted or replaced by a real unit test later!!
   * 
   * TODO: delete and replace with JUnit test
   */
  public static void main(String[] args) throws Exception {
    JbpmContext ctx = JbpmConfiguration.getInstance("org/jbpm/sim/bam/jbpm.cfg.xml").createJbpmContext();    
    GetSimulationInputCommand cmd = new GetSimulationInputCommand();
    cmd.setName("RegisterDomain");
    cmd.setVersion(6);
    Calendar cal1 = Calendar.getInstance();
    cal1.set(0, 0, 0, 18, 00);
    Calendar cal2 = Calendar.getInstance();
    cal2.set(0, 0, 0, 22, 35);
    
    cmd.setFromTimeOfDay(cal1.getTime());
    cmd.setTillTimeOfDay(cal2.getTime());
    BamSimulationProposal result = (BamSimulationProposal) cmd.execute(ctx);
    BamSimulationProposal.print(result);
  }

  public Date getFromTimeOfDay() {
    return fromTimeOfDay;
  }

  public void setFromTimeOfDay(Date fromTimeOfDay) {
    this.fromTimeOfDay = fromTimeOfDay;
  }

  public Date getTillTimeOfDay() {
    return tillTimeOfDay;
  }

  public void setTillTimeOfDay(Date tillTimeOfDay) {
    this.tillTimeOfDay = tillTimeOfDay;
  }

}
