package org.jbpm.command;

import org.jbpm.JbpmContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.util.Clock;

/**
 * Cancel the given {@link ProcessInstance} with all {@link Token}s. <b>Maybe running sub process
 * instances are currently not canceled.</b>
 * 
 * @author Bernd Ruecker (bernd.ruecker@camunda.com)
 */
public class CancelProcessInstanceCommand extends AbstractCancelCommand {

  private static final long serialVersionUID = 7145293049356621597L;

  private long processInstanceId;

  public CancelProcessInstanceCommand() {
  }

  public CancelProcessInstanceCommand(long processInstanceId) {
    this.processInstanceId = processInstanceId;
  }

  public Object execute(JbpmContext jbpmContext) throws Exception {
    this.jbpmContext = jbpmContext;
    cancelProcess(processInstanceId);
    this.jbpmContext = null;
    return null;
  }

  protected void cancelProcess(long processIdToCancel) {
    ProcessInstance pi = jbpmContext.getGraphSession().loadProcessInstance(processIdToCancel);

    log.info("cancel process instance " + pi.getId());

    // Record a standardized variable that we can use to determine that this
    // process has been 'canceled' and not just ended.
    pi.getContextInstance().createVariable(CANCELLATION_INDICATOR_VARIABLE_NAME,
        Clock.getCurrentTime());

    try {
      // End the process instance and any open tokens
      // TODO: Think about maybe canceling sub processes?
      cancelToken(pi.getRootToken());

      pi.end();

      log.info("finished process cancellation");
    }
    catch (RuntimeException ex) {
      log.error("problems while cancel process", ex);
      throw ex;
    }
  }

  public long getProcessInstanceId() {
    return processInstanceId;
  }

  public void setProcessInstanceId(long processInstanceId) {
    this.processInstanceId = processInstanceId;
  }

  /**
   * @deprecated use getProcessInstanceId instead
   */
  public long getProcessId() {
    return processInstanceId;
  }

  /**
   * @deprecated use setProcessInstanceId instead
   */
  public void setProcessId(long processId) {
    this.processInstanceId = processId;
  }

  public String getAdditionalToStringInformation() {
    return "processInstanceId=" + processInstanceId;
  }

  // methods for fluent programming

  public CancelProcessInstanceCommand processInstanceId(long processInstanceId) {
    setProcessInstanceId(processInstanceId);
    return this;
  }
  
  
}
