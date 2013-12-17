package org.jbpm.jbpm983;

import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;

/**
 * Concurrent process execution fails.
 * 
 * @see <a href="https://jira.jboss.org/jira/browse/JBPM-983">JBPM-983</a>
 * @author Tom Baeyens
 */
public class JBPM983Test extends AbstractDbTestCase {

  protected void setUp() throws Exception {
    super.setUp();

    ProcessDefinition subProcessDefinition = ProcessDefinition.parseXmlString(SUBPROCESS_XML);
    deployProcessDefinition(subProcessDefinition);

    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(PROCESS_XML);
    deployProcessDefinition(processDefinition);

    // [JBPM-2115] multiple threads not supported on DB2 < 9.7
    // multiple threads not supported on HSQL
    String dialect = getHibernateDialect();
    if (dialect.indexOf("DB2") == -1 && dialect.indexOf("HSQL") == -1) {
      jbpmConfiguration.getJobExecutor().setNbrOfThreads(4);
    }
  }

  protected void tearDown() throws Exception {
    jbpmConfiguration.getJobExecutor().setNbrOfThreads(1);
    super.tearDown();
  }

  static final String SUBPROCESS_XML = "<?xml version='1.0'?>"
    + "<process-definition xmlns='urn:jbpm.org:jpdl-3.2' name='subprocess'>"
    + "<start-state name='start-state1'>"
    + "   <description>start of the process</description>"
    + "   <transition name='start-to-check' to='fileCheck'/>"
    + "</start-state>"
    + "<node name='fileCheck' async='true'>"
    + "   <action name='action_filecheck' class='"
    + TestAction.class.getName()
    + "'>"
    + "   </action>"
    + "   <transition name='check-to-do' to='doWhatever'/>"
    + "</node>"
    + "<node name='doWhatever' async='true'>"
    + "   <action name='action_do' class='"
    + TestAction.class.getName()
    + "'/>"
    + "   <transition name='check-to-end' to='end-state-success'/>"
    + "</node>"
    + "<end-state name='end-state-success'>"
    + "   <description>process finished normally</description>"
    + "</end-state>"
    + "</process-definition>";

  static final String PROCESS_XML = "<?xml version='1.0'?>"
    + "<process-definition xmlns='urn:jbpm.org:jpdl-3.2' name='superprocess'>"
    + "<start-state name='start-state1'>"
    + "   <description>start of the process</description>"
    + "   <transition name='start-to-check' to='fileCheck'/>"
    + "</start-state>"
    + ""
    + "<node name='fileCheck' async='true'>"
    + "   <action name='action_check' class='"
    + TestAction.class.getName()
    + "'/>"
    + "   <transition name='check-to-fork' to='fork1'/>"
    + "</node>"
    + "<fork name='fork1'>"
    + "   <transition name='toNode1' to='node1'/>"
    + "   <transition name='toNode2' to='node2'/>"
    + "</fork>"
    + "<process-state name='node1' async='true'>"
    + "   <sub-process name='subprocess'/>"
    + "   <transition name='node1toJoin1' to='join1'/>"
    + "</process-state>"
    + "<process-state name='node2' async='true'>"
    + "   <sub-process name='subprocess'/>"
    + "   <transition name='node2toJoin1' to='join1'/>"
    + "</process-state>"
    + "<join name='join1'>"
    + "   <transition name='joinToEnd' to='end-state-success'/>"
    + "</join>"
    + "<end-state name='end-state-success'>"
    + "   <description>process finished normally</description>"
    + "</end-state>"
    + "</process-definition>";

  static final int INSTANCE_COUNT = 10;

  public void testConcurrentJobs() throws Exception {
    long[] processInstanceIds = new long[INSTANCE_COUNT];
    for (int i = 0; i < INSTANCE_COUNT; i++) {
      ProcessInstance processInstance = jbpmContext.newProcessInstanceForUpdate("superprocess");
      processInstance.getContextInstance().setVariable("test", "true");
      processInstance.signal();
      processInstanceIds[i] = processInstance.getId();
    }

    processJobs();
    for (int i = 0; i < INSTANCE_COUNT; i++) {
      long piId = processInstanceIds[i];
      ProcessInstance pi = jbpmContext.loadProcessInstance(piId);
      assertEquals("end-state-success", pi.getRootToken().getNode().getName());
    }
  }

  public static class TestAction implements ActionHandler {

    private static final long serialVersionUID = 1L;

    public void execute(ExecutionContext executionContext) throws Exception {
      Thread.sleep(200);
      executionContext.leaveNode();
    }
  }
}
