package org.jbpm.scheduler.ejbtimer;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbpm.JbpmContext;
import org.jbpm.command.Command;
import org.jbpm.job.Timer;

public class ExecuteTimerCommand implements Command {

  private final long timerId;

  private static final long serialVersionUID = 1L;

  public ExecuteTimerCommand(long timerId) {
    this.timerId = timerId;
  }

  public Object execute(JbpmContext jbpmContext) throws Exception {
    Timer timer = jbpmContext.getJobSession().loadTimer(timerId);
    timer.setLockOwner(getClass().getName()); // prevent others from removing timer
    log.debug("executing " + timer);
    try {
      if (timer.execute(jbpmContext)) {
        jbpmContext.getServices().getSchedulerService().deleteTimer(timer);
      }
    }
    catch (RuntimeException e) {
      // nothing to do but clean up and exit
      throw e;
    }
    catch (Exception e) {
      // save data about recoverable error condition
      log.error("exception while executing " + timer, e);
      StringWriter memoryWriter = new StringWriter();
      e.printStackTrace(new PrintWriter(memoryWriter));
      timer.setException(memoryWriter.toString());
      timer.setRetries(timer.getRetries() - 1);
    }
    return timer;
  }

  private static final Log log = LogFactory.getLog(ExecuteTimerCommand.class);
}
