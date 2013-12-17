package org.jbpm.jbpm2375;

import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;

/**
 * Test if the JobExecutorThread recovers from an Error.
 * 
 * @see <a href="https://jira.jboss.org/jira/browse/JBPM-2375">JBPM-2375</a>
 * @author mputz@redhat.com
 * @author Alejandro Guizar
 * @since 30-Jun-2009
 */
public class JBPM2375Test extends AbstractDbTestCase {

  static boolean throwError;

  // a process definition with two timers moving the token forward
  // the second state has an action associated with the node-enter event,
  // which can simulate an Error condition by throwing a NoClassDefFoundError
  private static final String PROCESS_DEFINITION = "<process-definition name='jbpm2375'>"
    + "  <start-state name='start'>"
    + "    <transition to='state1' name='to_state1'/>"
    + "  </start-state>"
    + "  <state name='state1'>"
    + "    <timer name='moveToNextStateAfter1second' duedate='1 second' transition='to_state2'/>"
    + "    <transition to='state2' name='to_state2'/>"
    + "  </state>"
    + "  <state name='state2'>"
    + "    <timer name='moveToEndAfter1second' duedate='1 second' transition='to_end'/>"
    + "    <event type='node-enter'>"
    + "      <action name='exceptionTest' class='"
    + TimerExceptionAction.class.getName()
    + "'>"
    + "      </action>"
    + "    </event>"
    + "    <transition to='end' name='to_end'/>"
    + "  </state>"
    + "  <end-state name='end' />"
    + "</process-definition>";

  protected void setUp() throws Exception {
    super.setUp();
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(PROCESS_DEFINITION);
    deployProcessDefinition(processDefinition);
  }

  /** check if the process ends correctly if no Error is thrown */
  public void testTimerWithoutErrorAction() {
    throwError = false;
    runTimerErrorAction();
  }

  /** check if the process ends correctly if an Error is thrown in the ActionHandler */
  public void testTimerWithErrorAction() {
    throwError = true;
    runTimerErrorAction();
  }

  private void runTimerErrorAction() {
    // kick off process instance
    ProcessInstance processInstance = jbpmContext.newProcessInstanceForUpdate("jbpm2375");
    processInstance.signal();

    processJobs();
    processInstance = jbpmContext.loadProcessInstance(processInstance.getId());
    assert processInstance.hasEnded() : "expected " + processInstance + " to have ended";
  }

  public static class TimerExceptionAction implements ActionHandler {
    private static final long serialVersionUID = 1L;

    public void execute(ExecutionContext executionContext) throws Exception {
      if (throwError) {
        throwError = false;
        throw new NoClassDefFoundError("org.jbpm.no.such.Class");
      }
    }
  }
}
