package org.jbpm.jbpm983;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;

/**
 * Concurrent process execution fails
 * 
 * https://jira.jboss.org/jira/browse/JBPM-983
 * 
 * @author Tom Baeyens
 */
public class JBPM983Test extends AbstractDbTestCase {

  private static Log log = LogFactory.getLog(JBPM983Test.class);

  private long subProcessDefinitionId;
  private long processDefinitionId;

  protected void setUp() throws Exception {
    super.setUp();

    ProcessDefinition subProcessDefinition = ProcessDefinition.parseXmlString(SUBPROCESS_XML);
    jbpmContext.deployProcessDefinition(subProcessDefinition);
    subProcessDefinitionId = subProcessDefinition.getId();

    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(PROCESS_XML);
    jbpmContext.deployProcessDefinition(processDefinition);
    processDefinitionId = processDefinition.getId();

    newTransaction();

    getJbpmConfiguration().getJobExecutor().setNbrOfThreads(5);
    startJobExecutor();
  }

  protected void tearDown() throws Exception {
    stopJobExecutor();
    getJbpmConfiguration().getJobExecutor().setNbrOfThreads(1);

    graphSession.deleteProcessDefinition(processDefinitionId);
    graphSession.deleteProcessDefinition(subProcessDefinitionId);      

    super.tearDown();
  }

  static String SUBPROCESS_XML = "<?xml version='1.0' encoding='UTF-8'?>"
      + "<process-definition xmlns='urn:jbpm.org:jpdl-3.2' name='subprocess'>"
      + "<start-state name='start-state1'>"
      + "   <description>start of the process</description>"
      + "   <transition name='start-to-check' to='fileCheck' />"
      + "</start-state>"
      + ""
      + "<node name='fileCheck' async='exclusive'>"
      + "   <action name='action_filecheck' class='"
      + TestAction.class.getName()
      + "'>"
      + "   </action>"
      + "   <transition name='check-to-do' to='doWhatever'></transition>"
      + "</node>"
      + ""
      + "<node name='doWhatever' async='exclusive'>"
      + "   <action name='action_do' class='"
      + TestAction.class.getName()
      + "'>"
      + "   </action>"
      + "   <transition name='check-to-end' to='end-state-success'></transition>"
      + "</node>"
      + ""
      + "<end-state name='end-state-success'>"
      + "   <description>process finished normally</description>"
      + "</end-state>"
      + "</process-definition>";

  static String PROCESS_XML = "<?xml version='1.0' encoding='UTF-8'?>"
      + "<process-definition xmlns='urn:jbpm.org:jpdl-3.2' name='superprocess'>"
      + "<start-state name='start-state1'>"
      + "   <description>start of the process</description>"
      + "   <transition name='start-to-check' to='fileCheck' />"
      + "</start-state>"
      + ""
      + "<node name='fileCheck' async='true'>"
      + "   <action name='action_check' class='"
      + TestAction.class.getName()
      + "'>"
      + "   </action>"
      + "   <transition name='check-to-fork' to='fork1'></transition>"
      + "</node>"
      + ""
      + "<fork name='fork1'>"
      + "   <transition name='toNode1' to='node1'></transition>"
      + "   <transition name='toNode2' to='node2'></transition>"
      + "</fork>"
      + ""
      + "<process-state name='node1' async='exclusive'>"
      + "   <sub-process name='subprocess' />"
      + "   <transition name='node1toJoin1' to='join1'></transition>"
      + "</process-state>"
      + ""
      + "<process-state name='node2' async='exclusive'>"
      + "   <sub-process name='subprocess' />"
      + "   <transition name='node2toJoin1' to='join1'></transition>"
      + "</process-state>"
      + ""
      + "<join name='join1'>"
      + "   <transition name='joinToEnd' to='end-state-success'></transition>"
      + "</join>"
      + ""
      + "<end-state name='end-state-success'>"
      + "   <description>process finished normally</description>"
      + "</end-state>"
      + "</process-definition>";

  public void testConcurrentJobs() throws Exception 
  {
    // Won't Fix [JBPM-983] concurrent process execution fails
    if (getHibernateDialect().indexOf("HSQL") != -1) 
    {
      return;
    }
    
    // create test properties
    Map testVariables = new HashMap();
    testVariables.put("test", "true");

    final int processCount = 10;
    long[] processInstanceIds = new long[processCount];
    for (int i = 0; i < processCount; i++) {
      ProcessInstance processInstance = jbpmContext.newProcessInstanceForUpdate("superprocess");
      processInstance.getContextInstance().addVariables(testVariables);
      processInstance.signal();
      processInstanceIds[i] = processInstance.getId();
      newTransaction();
    }

    for (int i = 0; i < processCount; i++) {
      long piId = processInstanceIds[i];
      waitFor(piId);

      ProcessInstance pi = jbpmContext.loadProcessInstance(piId);
      assertEquals("end-state-success", pi.getRootToken().getNode().getName());
    }

    processJobs(30 * 1000);
  }

  protected void waitFor(long piId) throws Exception {
    final int endTimeout = 30;
    long startTime = System.currentTimeMillis();

    while(!jbpmContext.loadProcessInstance(piId).hasEnded()) {
      if (System.currentTimeMillis() - startTime > endTimeout * 1000) {
        fail("Aborting after " + endTimeout + " seconds.");
        break;
      }

      newTransaction();

      log.info("waiting for workflow completion....");
      try {
        Thread.sleep(200);
      }
      catch (InterruptedException e) {
        log.error("wait for workflow was interruputed", e);
        break;
      }
    }
  }

  public static class TestAction implements ActionHandler {

    private static final long serialVersionUID = 1L;

    public void execute(ExecutionContext executionContext) throws Exception {
      String processName = executionContext.getProcessDefinition().getName()
          + ":"
          + executionContext.getProcessInstance().getId();
      String nodeName = executionContext.getToken().getNode().getName();
      String tokenName = executionContext.getToken().toString();

      log.info("ACTION (process="
          + processName
          + ",node="
          + nodeName
          + ",token="
          + tokenName
          + "): begin");

      for (int i = 0; i < 5; i++) {
        log.info("ACTION (process="
            + processName
            + ",node="
            + nodeName
            + ",token="
            + tokenName
            + "): working...");
        Thread.sleep(100);
      }

      log.info("ACTION (process="
          + processName
          + ",node="
          + nodeName
          + ",token="
          + tokenName
          + "): end");

      executionContext.leaveNode();
    }
  }
}
