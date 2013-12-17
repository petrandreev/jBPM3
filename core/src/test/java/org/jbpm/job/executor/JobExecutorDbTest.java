package org.jbpm.job.executor;

import java.text.FieldPosition;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.Action;
import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;

public class JobExecutorDbTest extends AbstractDbTestCase {

  private static final int INSTANCE_COUNT = 20;

  static Set collectedResults = Collections.synchronizedSet(new TreeSet());
  static List allocatedProcessIds = new Vector();

  protected void setUp() throws Exception {
    super.setUp();
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

  public void testJobExecutor() {
    deployProcessDefinition();
    startProcessInstances();
    processJobs();
    assertEquals(createExpectedResults(), collectedResults);
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
    deployProcessDefinition(processDefinition);
  }

  void startProcessInstances() {
    for (int i = 0; i < INSTANCE_COUNT; i++) {
      ProcessInstance processInstance = jbpmContext.newProcessInstanceForUpdate("bulk messages");
      processInstance.signal();
    }
  }

  static final char[] nodeNames = { 'a', 'b', 'c', 'd', 'e', 'X', 'Y', 'Z' };

  static Set createExpectedResults() {
    Set expectedResults = new TreeSet();

    NumberFormat formatter = NumberFormat.getIntegerInstance();
    formatter.setMinimumIntegerDigits(Integer.toString(INSTANCE_COUNT).length());
    StringBuffer text = new StringBuffer();
    FieldPosition position = new FieldPosition(NumberFormat.INTEGER_FIELD);

    for (int e = 0; e < INSTANCE_COUNT; e++) {
      text.setLength(0);
      formatter.format(e, text, position);
      text.append('\0');
      int lastIndex = text.length() - 1;

      for (int c = 0; c < nodeNames.length; c++) {
        text.setCharAt(lastIndex, nodeNames[c]);
        expectedResults.add(text.toString());
      }
    }
    return expectedResults;
  }

  public static class AutomaticActivity implements ActionHandler {

    private static final long serialVersionUID = 1L;

    public void execute(ExecutionContext executionContext) throws Exception {
      long id = executionContext.getProcessInstance().getId();
      String procIndex = getProcessIndex(id);

      String nodeName = executionContext.getNode().getName();
      collectedResults.add(procIndex + nodeName);
      executionContext.leaveNode();
    }
  }

  public static class AsyncAction implements ActionHandler {

    private static final long serialVersionUID = 1L;

    public void execute(ExecutionContext executionContext) throws Exception {
      long id = executionContext.getProcessInstance().getId();
      String procIndex = getProcessIndex(id);

      Action action = executionContext.getAction();
      String actionName = action.getName();
      collectedResults.add(procIndex + actionName);
    }
  }

  static synchronized String getProcessIndex(long id) {
    Long identifier = new Long(id);
    if (!allocatedProcessIds.contains(identifier)) allocatedProcessIds.add(identifier);

    int procIndex = allocatedProcessIds.indexOf(identifier);
    return procIndex < 10 ? "0" + procIndex : Integer.toString(procIndex);
  }
}
