package org.jbpm.jbpm2908;

import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;

public class AlternateConfigurationAction implements ActionHandler {

  private static final long serialVersionUID = 1L;

  public void execute(ExecutionContext exeContext) throws Exception {
    // load another configuration
    JbpmConfiguration jbpmConfiguration = JbpmConfiguration.parseResource("org/jbpm/jbpm2908/jbpm.cfg.xml");
    try {
      JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();
      try {
        // start instance
        ProcessInstance processInstance = jbpmContext.newProcessInstance("process2");
        processInstance.signal();
        jbpmContext.save(processInstance);
      }
      finally {
        jbpmContext.close();
      }
    }
    finally {
      jbpmConfiguration.close();
    }
  }
}
