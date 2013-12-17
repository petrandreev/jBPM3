package org.jbpm.jbpm2036;

import org.jbpm.context.exe.ContextInstance;
import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;

/**
 * StaleObjectStateException when repeating timer signals the token.
 * 
 * @see <a href="https://jira.jboss.org/jira/browse/JBPM-2036">JBPM-2036</a>
 * @author Thomas.Diesler@jboss.com
 * @since 11-Feb-2009
 */
public class JBPM2036Test extends AbstractDbTestCase {

  public void testTimerAction() {
    ProcessDefinition processDefinition = getProcessDefinition();
    deployProcessDefinition(processDefinition);

    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.signal();
    jbpmContext.save(processInstance);

    processJobs();

    processInstance = jbpmContext.loadProcessInstance(processInstance.getId());
    assertTrue("expected " + processInstance + " to have ended", processInstance.hasEnded());

    ContextInstance contextInstance = processInstance.getContextInstance();
    Integer count = (Integer) contextInstance.getVariable("chaos");
    assertEquals(1, count.intValue());
    count = (Integer) contextInstance.getVariable("undead");
    assertEquals(1, count.intValue());
  }

  private static ProcessDefinition getProcessDefinition() {
    return ProcessDefinition.parseXmlString("<process-definition name='jbpm2036'>"
      + "  <start-state name='start'>"
      + "    <transition to='midway'/>"
      + "  </start-state>"
      + "  <task-node name='midway' end-tasks='yes'>"
      + "    <timer name='chaos' duedate='2 seconds' repeat='5 seconds'>"
      + "      <action class='" + TimerAction.class.getName() + "'>"
      + "        <leave>true</leave>"
      + "      </action>"
      + "    </timer>"
      + "    <task name='doit'>"
      + "      <timer name='undead' duedate='1 second' repeat='5 seconds'>"
      + "        <action class='" + TimerAction.class.getName() + "'/>"
      + "      </timer>"
      + "    </task>"
      + "    <transition to='end'/>"
      + "  </task-node>"
      + "  <end-state name='end' />"
      + "</process-definition>");
  }

  public static class TimerAction implements ActionHandler {

    private boolean leave;
    private static final long serialVersionUID = 1L;

    public void execute(ExecutionContext executionContext) throws Exception {
      String timerName = executionContext.getTimer().getName();
      Integer count = (Integer) executionContext.getVariable(timerName);
      executionContext.setVariable(timerName, new Integer(count != null ? count.intValue() + 1
        : 1));

      if (leave) executionContext.leaveNode();
    }
  }
}
