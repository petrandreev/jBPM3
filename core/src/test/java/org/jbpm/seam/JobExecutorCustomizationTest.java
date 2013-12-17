package org.jbpm.seam;

import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.job.executor.JobExecutor;

public class JobExecutorCustomizationTest extends AbstractDbTestCase {

  protected String getJbpmTestConfig() {
    return "org/jbpm/seam/custom.job.executor.jbpm.cfg.xml";
  }

  protected void tearDown() throws Exception {
    super.tearDown();
    jbpmConfiguration.close();
  }

  public void testCustomJobExecutor() {
    JobExecutor jobExecutor = getJbpmConfiguration().getJobExecutor();
    assertEquals(CustomJobExecutor.class, jobExecutor.getClass());

    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition name='custom job exec'>"
      + "  <start-state name='start'>"
      + "    <transition to='end'/>"
      + "  </start-state>"
      + "  <end-state name='end' async='true' />"
      + "</process-definition>");
    deployProcessDefinition(processDefinition);

    ProcessInstance processInstance = jbpmContext.newProcessInstanceForUpdate("custom job exec");
    processInstance.signal();

    processJobs();

    processInstance = jbpmContext.loadProcessInstance(processInstance.getId());
    assertEquals(Boolean.TRUE, processInstance.getContextInstance().getVariable("custom"));
  }

}
