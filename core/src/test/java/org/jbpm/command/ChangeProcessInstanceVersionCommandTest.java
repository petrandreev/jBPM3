package org.jbpm.command;

import java.util.HashMap;
import java.util.Iterator;

import org.jbpm.JbpmException;
import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.job.Timer;
import org.jbpm.taskmgmt.exe.TaskInstance;

/**
 * Tests for {@link ChangeProcessInstanceVersionCommand}
 * 
 * @author bernd.ruecker@camunda.com
 */
public class ChangeProcessInstanceVersionCommandTest extends AbstractDbTestCase {

  private ProcessDefinition pd1;
  private ProcessDefinition pd2;

  protected void tearDown() throws Exception {
    newTransaction();
    // IMPORTANT: The ProcessDefinitions have to be deleted in one transaction,
    // in the right order (new definition with ProcessInstance first) or the
    // ProcessInstance has to be deleted first independently.

    // This is because Logs of the ProcessInstance point to both ProcessDefinitions 
    // (the old and the new one) but only with the new ProcessDefinition the 
    // ProcessInstance is deleted

    // exceptions look like this: could not delete: [org.jbpm.graph.def.Transition#9]
    // Integrity constraint violation FK_LOG_TRANSITION table: JBPM_LOG in statement
    // [delete from JBPM_TRANSITION where ID_=?]

    // IMPORTANT: Keep this order of deletions! Otherwise if there is
    // more than one ProcessInstance for the ProcessDefinition a HibernateSeassion.flush
    // is called when querying the second ProcessInstance after deleting the first
    // one which may fire an integrity constraint violation (same problem as described
    // above), in this case I got
    // could not delete: [org.jbpm.taskmgmt.def.TaskMgmtDefinition#2]
    // Integrity constraint violation FK_TASKMGTINST_TMD table: JBPM_MODULEINSTANCE in statement
    // [delete from JBPM_MODULEDEFINITION where ID_=?]
    graphSession.deleteProcessDefinition(pd2.getId());
    graphSession.deleteProcessDefinition(pd1.getId());

    super.tearDown();
  }

  /**
   * test easy version migration (no fork or other stuff) but with name mapping
   * (different state name in new process definition)
   */
  public void testNameMapping() throws Exception {
    String xmlVersion1 = "<process-definition name='TestChangeVersion'>"
      + "   <start-state name='start'>"
      + "      <transition name='state1' to='state1' />"
      + "      <transition name='state2' to='state2' />"
      + "   </start-state>"
      + "   <state name='state1'>"
      + "      <transition name='end1' to='end' />"
      + "   </state>"
      + "   <state name='state2'>"
      + "      <transition name='end2' to='end' />"
      + "   </state>"
      + "   <end-state name='end'/>"
      + "</process-definition>";

    pd1 = ProcessDefinition.parseXmlString(xmlVersion1);
    jbpmContext.deployProcessDefinition(pd1);

    // start 2 instances
    ProcessInstance pi1 = jbpmContext.newProcessInstance("TestChangeVersion");
    pi1.signal("state1");
    ProcessInstance pi2 = jbpmContext.newProcessInstance("TestChangeVersion");
    pi2.signal("state2");

    String xmlVersion2 = "<process-definition name='TestChangeVersion'>"
      + "   <start-state name='start'>"
      + "      <transition name='state1' to='state1' />"
      + "      <transition name='state2' to='state2b'/>"
      + "   </start-state>"
      + "   <state name='state1'>"
      + "      <transition name='end1' to='end' />"
      + "   </state>"
      + "   <state name='state2b'>"
      + "      <transition name='end2b' to='end' />"
      + "   </state>"
      + "   <end-state name='end' />"
      + "</process-definition>";

    pd2 = ProcessDefinition.parseXmlString(xmlVersion2);
    jbpmContext.deployProcessDefinition(pd2);

    // now change all process instances to most current version
    try {
      new ChangeProcessInstanceVersionCommand().processName("TestChangeVersion")
        .execute(jbpmContext);
      fail("Exception expected, saying that state2 is missing in new version");
    }
    catch (JbpmException ex) {
      assert ex.getMessage().indexOf("state2") != -1 : ex.getMessage();
    }

    // now supply a mapping for the missing node
    new ChangeProcessInstanceVersionCommand().nodeNameMappingAdd("state2", "state2b")
      .processName("TestChangeVersion")
      .execute(jbpmContext);

    newTransaction();
    pi1 = graphSession.loadProcessInstance(pi1.getId());
    pi2 = graphSession.loadProcessInstance(pi2.getId());

    assertEquals("state1", pi1.getRootToken().getNode().getName());
    assertEquals(pd2.getNode("state1").getId(), pi1.getRootToken().getNode().getId());

    assertEquals("state2b", pi2.getRootToken().getNode().getName());
    assertEquals(pd2.getNode("state2b").getId(), pi2.getRootToken().getNode().getId());

    pi1.getRootToken().signal("end1");
    pi2.getRootToken().signal("end2b");

    newTransaction();
    pi1 = graphSession.loadProcessInstance(pi1.getId());
    pi2 = graphSession.loadProcessInstance(pi2.getId());

    assertEquals(pd2.getNode("end").getId(), pi1.getRootToken().getNode().getId());
    assertTrue(pi1.hasEnded());
    assertEquals(pd2.getNode("end").getId(), pi2.getRootToken().getNode().getId());
    assertTrue(pi2.hasEnded());
  }

  /**
   * check that update of nodes work correctly if a fork was involved and
   * multiple child tokens exist
   */
  public void testSubTokensInFork() throws Exception {
    String xmlVersion1 = "<process-definition name='TestChangeVersion'>"
      + "   <start-state name='start'>"
      + "      <transition to='fork' />"
      + "   </start-state>"
      + "   <fork name='fork'>"
      + "      <transition name='path1' to='path1' />"
      + "      <transition name='path2' to='path2' />"
      + "   </fork>"
      + "   <state name='path1'>"
      + "      <transition to='join' />"
      + "   </state>"
      + "   <state name='path2'>"
      + "      <transition to='join' />"
      + "   </state>"
      + "   <join name='join'>"
      + "      <transition to='end' />"
      + "   </join>"
      + "   <end-state name='end' />"
      + "</process-definition>";

    pd1 = ProcessDefinition.parseXmlString(xmlVersion1);
    jbpmContext.deployProcessDefinition(pd1);

    // start instance
    ProcessInstance pi1 = jbpmContext.newProcessInstance("TestChangeVersion");
    pi1.signal();
    Token t1 = pi1.getRootToken().getChild("path1");
    Token t2 = pi1.getRootToken().getChild("path2");

    String xmlVersion2 = "<process-definition name='TestChangeVersion'>"
      + "   <start-state name='start'>"
      + "      <transition to='fork' />"
      + "   </start-state>"
      + "   <fork name='fork'>"
      + "      <transition name='path1' to='path1' />"
      + "      <transition name='path2' to='path2b' />"
      + "   </fork>"
      + "   <state name='path1'>"
      + "      <transition to='join' />"
      + "   </state>"
      + "   <state name='path2b'>"
      + "      <transition name='2b' to='join' />"
      + "   </state>"
      + "   <join name='join'>"
      + "      <transition to='end' />"
      + "   </join>"
      + "   <end-state name='end' />"
      + "</process-definition>";

    pd2 = ProcessDefinition.parseXmlString(xmlVersion2);
    jbpmContext.deployProcessDefinition(pd2);

    // now change all process instances to most current version
    try {
      new ChangeProcessInstanceVersionCommand().processInstanceId(pi1.getId())
        .execute(jbpmContext);
      fail("Exception expected, saying that path2 is missing in new version");
    }
    catch (JbpmException ex) {
      assert ex.getMessage().indexOf("path2") != -1 : ex.getMessage();
    }

    // now supply a mapping for the missing node
    new ChangeProcessInstanceVersionCommand().nodeNameMappingAdd("path2", "path2b")
      .processInstanceId(pi1.getId())
      .execute(jbpmContext);

    newTransaction();

    t1 = graphSession.getToken(t1.getId());
    t2 = graphSession.getToken(t2.getId());

    assertEquals(pd2.getNode("path1").getId(), t1.getNode().getId());
    assertEquals(pd2.getNode("path2b").getId(), t2.getNode().getId());

    t1.signal();
    t2.signal("2b");

    pi1 = graphSession.getProcessInstance(pi1.getId());

    assertEquals(pd2.getNode("end").getId(), pi1.getRootToken().getNode().getId());
    assertTrue(pi1.hasEnded());
  }

  public void testTaskInFork() throws Exception {
    String xmlVersion1 = "<process-definition name='TestChangeVersion'>"
      + "   <start-state name='start'>"
      + "      <transition to='fork' />"
      + "   </start-state>"
      + "   <fork name='fork'>"
      + "      <transition name='path1' to='task1' />"
      + "      <transition name='path2' to='task2' />"
      + "   </fork>"
      + "   <task-node name='task1'>"
      + "      <task name='theTask1' />"
      + "      <transition to='join' />"
      + "   </task-node>"
      + "   <task-node name='task2'>"
      + "      <task name='theTask2' />"
      + "      <transition to='join' />"
      + "   </task-node>"
      + "   <join name='join'>"
      + "      <transition to='end' />"
      + "   </join>"
      + "   <end-state name='end' />"
      + "</process-definition>";

    pd1 = ProcessDefinition.parseXmlString(xmlVersion1);
    jbpmContext.deployProcessDefinition(pd1);

    // start instance
    ProcessInstance pi1 = jbpmContext.newProcessInstance("TestChangeVersion");
    pi1.signal();
    Token t1 = pi1.getRootToken().getChild("path1");
    Token t2 = pi1.getRootToken().getChild("path2");

    String xmlVersion2 = "<process-definition name='TestChangeVersion'>"
      + "   <start-state name='start'>"
      + "      <transition to='fork' />"
      + "   </start-state>"
      + "   <fork name='fork'>"
      + "      <transition name='path1' to='task1' />"
      + "      <transition name='path2' to='task2' />"
      + "   </fork>"
      + "   <task-node name='task1b'>"
      + "      <task name='theTask1b' />"
      + "      <transition to='join' />"
      + "   </task-node>"
      + "   <task-node name='task2b'>"
      + "      <task name='theTask2b' />"
      + "      <transition to='join' />"
      + "   </task-node>"
      + "   <join name='join'>"
      + "      <transition to='end' />"
      + "   </join>"
      + "   <end-state name='end' />"
      + "</process-definition>";

    pd2 = ProcessDefinition.parseXmlString(xmlVersion2);
    jbpmContext.deployProcessDefinition(pd2);

    HashMap nodeNameMap = new HashMap();
    nodeNameMap.put("task1", "task1b");
    nodeNameMap.put("task2", "task2b");
    HashMap taskNameMap = new HashMap();
    taskNameMap.put("theTask1", "theTask1b");
    taskNameMap.put("theTask2", "theTask2b");

    // now supply a mapping for the missing node
    new ChangeProcessInstanceVersionCommand().nodeNameMapping(nodeNameMap)
      .taskNameMapping(taskNameMap)
      .processInstanceId(pi1.getId())
      .execute(jbpmContext);

    newTransaction();

    t1 = jbpmContext.loadTokenForUpdate(t1.getId());
    assertEquals(pd2.getNode("task1b").getId(), t1.getNode().getId());

    Iterator taskInstanceIter = t1.getProcessInstance()
      .getTaskMgmtInstance()
      .getTaskInstances()
      .iterator();
    TaskInstance ti1 = (TaskInstance) taskInstanceIter.next();
    if ("theTask2b".equals(ti1.getTask().getName())) {
      // this was the wrong one
      ti1 = (TaskInstance) taskInstanceIter.next();
    }
    assertEquals("theTask1b", ti1.getTask().getName());
    assertEquals(pd2.getTaskMgmtDefinition().getTask("theTask1b").getId(), ti1.getTask()
      .getId());

    ti1.end();

    // /////
    newTransaction();

    t2 = graphSession.getToken(t2.getId());
    assertEquals(pd2.getNode("task2b").getId(), t2.getNode().getId());

    taskInstanceIter = t2.getProcessInstance()
      .getTaskMgmtInstance()
      .getTaskInstances()
      .iterator();
    TaskInstance ti2 = (TaskInstance) taskInstanceIter.next();
    if ("theTask1b".equals(ti2.getTask().getName())) {
      // this was the wrong one
      ti2 = (TaskInstance) taskInstanceIter.next();
    }
    assertEquals("theTask2b", ti2.getTask().getName());
    assertEquals(pd2.getTaskMgmtDefinition().getTask("theTask2b").getId(), ti2.getTask()
      .getId());

    ti2.end();

    newTransaction();
    pi1 = graphSession.loadProcessInstance(pi1.getId());

    assertEquals(pd2.getNode("end").getId(), pi1.getRootToken().getNode().getId());
    assertTrue(pi1.hasEnded());
  }

  /**
   * check, that TaskInstances work (TaskInstance reference to Task has to be
   * adjusted as will)
   */
  public void testTaskInstances() throws Exception {
    String xmlVersion1 = "<process-definition name='testTaskInstances'>"
      + "   <start-state name='start'>"
      + "      <transition name='path1' to='task1' />"
      + "      <transition name='path2' to='task2' />"
      + "   </start-state>"
      + "   <task-node name='task1'>"
      + "      <task name='theTask1'/>"
      + "      <transition name='end1' to='end' />"
      + "   </task-node>"
      + "   <task-node name='task2'>"
      + "      <task name='theTask2'/>"
      + "      <transition name='end2' to='end' />"
      + "   </task-node>"
      + "   <end-state name='end'/>"
      + "</process-definition>";

    pd1 = ProcessDefinition.parseXmlString(xmlVersion1);
    jbpmContext.deployProcessDefinition(pd1);

    // start 2 instances
    ProcessInstance pi1 = jbpmContext.newProcessInstance("testTaskInstances");
    pi1.signal("path1");
    ProcessInstance pi2 = jbpmContext.newProcessInstance("testTaskInstances");
    pi2.signal("path2");

    String xmlVersion2 = "<process-definition name='testTaskInstances'>"
      + "   <start-state name='start'>"
      + "      <transition name='path1' to='task1' />"
      + "      <transition name='path2' to='task2b' />"
      + "   </start-state>"
      + "   <task-node name='task1'>"
      + "      <task name='theTask1'/>"
      + "      <transition name='end1' to='end' />"
      + "   </task-node>"
      + "   <task-node name='task2b'>"
      + "      <task name='theTask2b'/>"
      + "      <transition name='end2b' to='end' />"
      + "   </task-node>"
      + "   <end-state name='end'/>"
      + "</process-definition>";

    pd2 = ProcessDefinition.parseXmlString(xmlVersion2);
    jbpmContext.deployProcessDefinition(pd2);

    // process instance 1 can be updated, state names haven't changed
    new ChangeProcessInstanceVersionCommand().processInstanceId(pi1.getId())
      .execute(jbpmContext);

    // now change all process instances to most current version
    try {
      new ChangeProcessInstanceVersionCommand().nodeNameMappingAdd("task2", "task2b")
        .processName("testTaskInstances")
        .execute(jbpmContext);
      // fail because task2 is not mapped
      fail("Exception expected, saying that theTask2 is missing in new version");
    }
    catch (JbpmException ex) {
      assertTrue( ex.getMessage(), ex.getMessage().indexOf("theTask2") != -1 );
    }

    // now supply a mapping for the missing task
    new ChangeProcessInstanceVersionCommand().nodeNameMappingAdd("task2", "task2b")
      .taskNameMappingAdd("theTask2", "theTask2b")
      .processName("testTaskInstances")
      .execute(jbpmContext);

    newTransaction();

    pi1 = graphSession.loadProcessInstance(pi1.getId());
    pi2 = graphSession.loadProcessInstance(pi2.getId());

    assertEquals(pd2.getNode("task1").getId(), pi1.getRootToken().getNode().getId());
    assertEquals(pd2.getNode("task2b").getId(), pi2.getRootToken().getNode().getId());

    TaskInstance ti1 = (TaskInstance) pi1.getTaskMgmtInstance()
      .getTaskInstances()
      .iterator()
      .next();
    TaskInstance ti2 = (TaskInstance) pi2.getTaskMgmtInstance()
      .getTaskInstances()
      .iterator()
      .next();

    assertEquals(pd2.getTaskMgmtDefinition().getTask("theTask1").getId(), ti1.getTask().getId());
    assertEquals(pd2.getTaskMgmtDefinition().getTask("theTask2b").getId(), ti2.getTask()
      .getId());

    ti1.end("end1");
    ti2.end("end2b");

    assertEquals(pd2.getNode("end").getId(), pi1.getRootToken().getNode().getId());
    assertTrue(pi1.hasEnded());
    assertEquals(pd2.getNode("end").getId(), pi2.getRootToken().getNode().getId());
    assertTrue(pi2.hasEnded());
  }

  /**
   * test if changing process version works correctly if a timer is included in
   * the process definition. Important: The timer itself IS NOT changed, so e.g.
   * used leaving transitions must be still existent
   */
  public void testTimerInState() throws Exception {
    String xmlVersion1 = "<process-definition name='TestChangeVersion'>"
      + "   <start-state name='start'>"
      + "      <transition to='timer1' />"
      + "   </start-state>"
      + "   <state name='timer1'>"
      + "      <timer name='timer1' duedate='5 seconds' transition='end' />"
      + "      <transition name='end' to='end' />"
      + "   </state>"
      + "   <end-state name='end'/>"
      + "</process-definition>";

    pd1 = ProcessDefinition.parseXmlString(xmlVersion1);
    jbpmContext.deployProcessDefinition(pd1);

    // start instance
    ProcessInstance pi1 = jbpmContext.newProcessInstance("TestChangeVersion");
    pi1.signal();
    Timer timer = (Timer) session.createQuery("from org.jbpm.job.Timer").uniqueResult();

    // check timer
    assertNotNull("Timer is null", timer);
    assertEquals("timer1", timer.getName());
    assertEquals(pd1.getNode("timer1").getId(), timer.getGraphElement().getId());

    String xmlVersion2 = "<process-definition name='TestChangeVersion'>"
      + "   <start-state name='start'>"
      + "      <transition to='timer2' />"
      + "   </start-state>"
      + "   <state name='timer2'>"
      + "      <timer name='timer1' duedate='5 seconds' transition='end1' />"
      + "      <transition name='end' to='end' />"
      + "   </state>"
      + "   <end-state name='end'/>"
      + "</process-definition>";

    pd2 = ProcessDefinition.parseXmlString(xmlVersion2);
    jbpmContext.deployProcessDefinition(pd2);

    // change version
    HashMap nameMap = new HashMap();
    nameMap.put("timer1", "timer2");
    new ChangeProcessInstanceVersionCommand().nodeNameMapping(nameMap)
      .processInstanceId(pi1.getId())
      .execute(jbpmContext);

    // load changed stuff
    newTransaction();
    pi1 = graphSession.loadProcessInstance(pi1.getId());
    timer = (Timer) session.createQuery("from org.jbpm.job.Timer").uniqueResult();

    // and check again
    assertEquals(pd2.getNode("timer2").getId(), pi1.getRootToken().getNode().getId());
    assertEquals("timer1", timer.getName());
    assertEquals(pd2.getNode("timer2").getId(), timer.getGraphElement().getId());

    timer.execute(jbpmContext);

    assertEquals(pd2.getNode("end").getId(), pi1.getRootToken().getNode().getId());
    assertTrue(pi1.hasEnded());
  }

  public void testTimerInTask() throws Exception {
    String xmlVersion1 = "<process-definition name='TestChangeVersion'>"
      + "   <start-state name='start'>"
      + "      <transition to='timer1' />"
      + "   </start-state>"
      + "   <task-node name='timer1'>"
      + "      <task name='myTask'>"
      + "        <timer name='timer1' duedate='5 seconds' transition='end' />"
      + "      </task>"
      + "      <transition name='end' to='end' />"
      + "   </task-node>"
      + "   <end-state name='end'/>"
      + "</process-definition>";

    pd1 = ProcessDefinition.parseXmlString(xmlVersion1);
    jbpmContext.deployProcessDefinition(pd1);

    // start instance
    ProcessInstance pi1 = jbpmContext.newProcessInstance("TestChangeVersion");
    pi1.signal();
    // jbpmContext.getJobSession().deleteJobsForProcessInstance(processInstance);
    // NOT UNIQUE?!
    Timer timer = (Timer) session.createQuery("from org.jbpm.job.Timer").uniqueResult();

    // check timer
    assertNotNull("Timer is null", timer);
    assertEquals("timer1", timer.getName());
    assertEquals(pd1.getTaskMgmtDefinition().getTask("myTask").getId(), timer.getGraphElement()
      .getId());
    TaskInstance ti1 = (TaskInstance) pi1.getTaskMgmtInstance()
      .getTaskInstances()
      .iterator()
      .next();
    assertEquals(pd1.getTaskMgmtDefinition().getTask("myTask").getId(), ti1.getTask().getId());

    String xmlVersion2 = "<process-definition name='TestChangeVersion'>"
      + "   <start-state name='start'>"
      + "      <transition to='timer2' />"
      + "   </start-state>"
      + "   <task-node name='timer2'>"
      + "      <task name='myTask2'>"
      + "        <timer name='timer1' duedate='5 seconds' transition='end' />"
      + "      </task>"
      + "      <transition name='end' to='end' />"
      + "   </task-node>"
      + "   <end-state name='end'/>"
      + "</process-definition>";

    pd2 = ProcessDefinition.parseXmlString(xmlVersion2);
    jbpmContext.deployProcessDefinition(pd2);

    // change version
    HashMap nameMap = new HashMap();
    nameMap.put("timer1", "timer2");
    nameMap.put("myTask", "myTask2");
    new ChangeProcessInstanceVersionCommand().nodeNameMapping(nameMap)
      .taskNameMapping(nameMap)
      .processInstanceId(pi1.getId())
      .execute(jbpmContext);

    // load changed stuff
    newTransaction();
    pi1 = graphSession.loadProcessInstance(pi1.getId());
    timer = (Timer) session.createQuery("from org.jbpm.job.Timer").uniqueResult();

    // and check again
    assertEquals(pd2.getNode("timer2").getId(), pi1.getRootToken().getNode().getId());
    assertEquals("timer1", timer.getName());
    assertEquals(pd2.getTaskMgmtDefinition().getTask("myTask2").getId(),
      timer.getGraphElement().getId());
    ti1 = (TaskInstance) pi1.getTaskMgmtInstance().getTaskInstances().iterator().next();
    assertEquals(pd2.getTaskMgmtDefinition().getTask("myTask2").getId(), ti1.getTask().getId());

    // and go on
    timer.execute(jbpmContext);

    assertEquals(pd2.getNode("end").getId(), pi1.getRootToken().getNode().getId());
    assertTrue(pi1.hasEnded());
  }

  /*
   * Asynchronous continuation is not affected by changing the version, because
   * a {@link Job} only holds {@link ProcessInstance}.id, {@link Token}.id or
   * {@link TaskInstance}.id. None of them are changed while version changes.
   */
}
