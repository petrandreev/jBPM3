package org.jbpm.logging.exe;

import java.util.List;

import org.jbpm.JbpmConfiguration;
import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.logging.log.ProcessLog;

@SuppressWarnings({
  "rawtypes", "unchecked"
})
public class LoggingConfigDbTest extends AbstractDbTestCase {

  protected JbpmConfiguration getJbpmConfiguration() {
    if (jbpmConfiguration == null) {
      jbpmConfiguration = JbpmConfiguration.parseResource("org/jbpm/logging/exe/nologging.jbpm.cfg.xml");
    }
    return jbpmConfiguration;
  }

  protected void tearDown() throws Exception {
    super.tearDown();
    jbpmConfiguration.close();
  }

  public void testLoggingconfiguration() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition name='test-log-process'>" +
      "  <start-state>" +
      "    <transition to='s' />" +
      "  </start-state>" +
      "  <state name='s'>" +
      "    <transition to='s' />" +
      "  </state>" +
      "</process-definition>" 
    );
    deployProcessDefinition(processDefinition);

    ProcessInstance processInstance = jbpmContext.newProcessInstance("test-log-process");
    processInstance.getContextInstance().setVariable("a", "1");
    processInstance.signal();

    processInstance = saveAndReload(processInstance);
    
    newTransaction();
    List logs = session.createCriteria(ProcessLog.class).list();
    assertEquals(0, logs.size());
  }
}
