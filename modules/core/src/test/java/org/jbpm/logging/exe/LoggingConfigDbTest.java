package org.jbpm.logging.exe;

import java.util.List;

import org.jbpm.JbpmConfiguration;
import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.logging.log.ProcessLog;

public class LoggingConfigDbTest extends AbstractDbTestCase {

  @Override
  protected JbpmConfiguration getJbpmConfiguration() {
    if (jbpmConfiguration == null) {
      jbpmConfiguration = JbpmConfiguration.parseResource("org/jbpm/logging/exe/nologging.jbpm.cfg.xml");
    }
    return jbpmConfiguration;
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    jbpmConfiguration.close();
  }

  public void testLoggingconfiguration() {
    ProcessDefinition processDefinition = new ProcessDefinition("logging");
    jbpmContext.deployProcessDefinition(processDefinition);
    ProcessInstance processInstance = jbpmContext.newProcessInstance("logging");
    processInstance.getContextInstance().setVariable("a", "1");

    newTransaction();
    List<?> logs = session.createCriteria(ProcessLog.class).list();
    assertEquals(0, logs.size());

    graphSession.deleteProcessDefinition(processDefinition.getId());
  }
}
