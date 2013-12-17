package org.jbpm.seam;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.job.executor.JobExecutor;

public class JobExecutorCustomizationTest extends AbstractDbTestCase {

  private static List<String> jobEvents = new ArrayList<String>();

  protected String getJbpmTestConfig() {
    return "org/jbpm/seam/custom.job.executor.jbpm.cfg.xml";
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    jbpmConfiguration.close();
  }

  public void testCustomJobExecutor() {
    JobExecutor jobExecutor = getJbpmConfiguration().getJobExecutor();
    assertEquals(CustomJobExecutor.class, jobExecutor.getClass());

    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition name='customjobexecution' initial='start'>"
        + "  <node name='start'>"
        + "    <transition to='end'>"
        + "      <action async='true' class='"
        + AsyncAction.class.getName()
        + "' />"
        + "    </transition>"
        + "  </node>"
        + "  <state name='end' />"
        + "</process-definition>");
    jbpmContext.deployProcessDefinition(processDefinition);

    newTransaction();
    jbpmContext.newProcessInstanceForUpdate("customjobexecution");

    processJobs(20 * 1000);
    List<String> expectedJobEvents = Arrays.asList("before", "execute action", "after");
    assertEquals(expectedJobEvents, jobEvents);

    graphSession.deleteProcessDefinition(processDefinition.getId());
  }

  public static void addJobEvent(String event) {
    jobEvents.add(event);
  }

}
