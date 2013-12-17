package org.jbpm.jbpm1798;

import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ExecutionContext;

/**
 * Potential null pointer in asynchronous jobs when process ends
 * https://jira.jboss.org/jira/browse/JBPM-1798
 * 
 * @author Thomas.Diesler@jboss.com
 */
public class JBPM1798Test extends AbstractDbTestCase {

  private static final long maxWaitTime = 10 * 1000;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    getJbpmConfiguration().getJobExecutor().setNbrOfThreads(2);
  }

  @Override
  protected void tearDown() throws Exception {
    getJbpmConfiguration().getJobExecutor().setNbrOfThreads(1);
    super.tearDown();
  }

  public void testJobExecution() {
    ProcessDefinition pd = getProcessDefinition();
    jbpmContext.deployProcessDefinition(pd);
    newTransaction();
    try {
      pd.createProcessInstance();
      newTransaction();
      assertEquals(1, getNbrOfJobsAvailable());
      processJobs(maxWaitTime);
    }
    finally {
      graphSession.deleteProcessDefinition(pd.getId());
    }
  }

  private ProcessDefinition getProcessDefinition() {
    ProcessDefinition pd = ProcessDefinition.parseXmlString("<process-definition name='customjobexecution' initial='start'>"
        + "  <node name='start'>"
        + "    <transition to='end'>"
        + "      <action async='true' class='"
        + AsyncAction.class.getName()
        + "' />"
        + "    </transition>"
        + "  </node>"
        + "  <end-state name='end' />"
        + "</process-definition>");
    return pd;
  }

  public static class AsyncAction implements ActionHandler {
    private static final long serialVersionUID = 1L;

    public void execute(ExecutionContext executionContext) throws Exception {
      System.out.println("do stuff");
    }
  }
}
