package org.jbpm.job.executor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.Action;
import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;

public class JobExecutorDbTest extends AbstractDbTestCase {

  static final int nbrOfConcurrentProcessExecutions = 20;
  static final int timeout = 60000;

  static Set<String> collectedResults = Collections.synchronizedSet(new TreeSet<String>());
  static List<Long> allocatedProcessIds = Collections.synchronizedList(new ArrayList<Long>());

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    getJbpmConfiguration().getJobExecutor().setNbrOfThreads(5);
  }

  @Override
  protected void tearDown() throws Exception {
    getJbpmConfiguration().getJobExecutor().setNbrOfThreads(1);
    super.tearDown();
  }

  public void testJobExecutor() {
    deployProcessDefinition();
    try {
      startProcessInstances();
      processJobs(timeout);
      assertEquals(getExpectedResults(), collectedResults);
    }
    finally {
      deleteProcessDefinition();
    }
  }

  void deployProcessDefinition() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition name='bulk messages'>"
        + "  <start-state>"
        + "    <transition to='a' />"
        + "  </start-state>"
        + "  <node name='a' async='true'>"
        + "    <action class='"
        + AutomaticActivity.class.getName()
        + "' />"
        + "    <transition to='b' />"
        + "  </node>"
        + "  <node name='b' async='true'>"
        + "    <event type='node-enter'>"
        + "      <action name='X' async='true' class='"
        + AsyncAction.class.getName()
        + "' />"
        + "    </event>"
        + "    <action class='"
        + AutomaticActivity.class.getName()
        + "' />"
        + "    <transition to='c' />"
        + "  </node>"
        + "  <node name='c' async='true'>"
        + "    <action class='"
        + AutomaticActivity.class.getName()
        + "' />"
        + "    <transition to='d'>"
        + "      <action name='Y' async='true' class='"
        + AsyncAction.class.getName()
        + "' />"
        + "    </transition>"
        + "  </node>"
        + "  <node name='d' async='true'>"
        + "    <action class='"
        + AutomaticActivity.class.getName()
        + "' />"
        + "    <transition to='e' />"
        + "    <event type='node-leave'>"
        + "      <action name='Z' async='true' class='"
        + AsyncAction.class.getName()
        + "' />"
        + "    </event>"
        + "  </node>"
        + "  <node name='e' async='true'>"
        + "    <action class='"
        + AutomaticActivity.class.getName()
        + "' />"
        + "    <transition to='end' />"
        + "  </node>"
        + "  <end-state name='end'/>"
        + "</process-definition>");

    jbpmContext.deployProcessDefinition(processDefinition);
    newTransaction();
  }

  void startProcessInstances() {
    for (int i = 0; i < nbrOfConcurrentProcessExecutions; i++) {
      ProcessInstance processInstance = jbpmContext.newProcessInstanceForUpdate("bulk messages");
      processInstance.signal();
      newTransaction();
    }
  }

  Set<String> getExpectedResults() {
    Set<String> expectedResults = new HashSet<String>();
    for (int i = 0; i < nbrOfConcurrentProcessExecutions; i++) {
      String prefix = (i < 10 ? "0" : "");
      expectedResults.add(prefix + i + "a");
      expectedResults.add(prefix + i + "b");
      expectedResults.add(prefix + i + "c");
      expectedResults.add(prefix + i + "d");
      expectedResults.add(prefix + i + "e");
      expectedResults.add(prefix + i + "X");
      expectedResults.add(prefix + i + "Y");
      expectedResults.add(prefix + i + "Z");
    }
    return expectedResults;
  }

  void deleteProcessDefinition() {
    ProcessDefinition processDefinition = graphSession.findLatestProcessDefinition("bulk messages");
    graphSession.deleteProcessDefinition(processDefinition);
  }

  public static class AutomaticActivity implements ActionHandler {

    private static final long serialVersionUID = 1L;

    public void execute(ExecutionContext executionContext) throws Exception {
      Long id = executionContext.getProcessInstance().getId();
      String procIndex = getProcessIndex(id);

      String nodeName = executionContext.getNode().getName();
      collectedResults.add(procIndex + nodeName);
      executionContext.leaveNode();
    }
  }

  public static class AsyncAction implements ActionHandler {

    private static final long serialVersionUID = 1L;

    public void execute(ExecutionContext executionContext) throws Exception {
      Long id = executionContext.getProcessInstance().getId();
      String procIndex = getProcessIndex(id);

      Action action = executionContext.getAction();
      String actionName = action.getName();
      collectedResults.add(procIndex + actionName);
    }
  }

  static synchronized String getProcessIndex(Long id) {
    if (allocatedProcessIds.contains(id) == false) allocatedProcessIds.add(id);

    int procIndex = allocatedProcessIds.indexOf(id);
    String prefix = (procIndex < 10 ? "0" : "");

    return prefix + procIndex;
  }
}
