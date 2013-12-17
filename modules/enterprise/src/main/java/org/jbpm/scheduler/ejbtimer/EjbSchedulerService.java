package org.jbpm.scheduler.ejbtimer;

import javax.ejb.CreateException;
import javax.ejb.RemoveException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;
import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.jbpm.db.JobSession;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.job.Timer;
import org.jbpm.scheduler.SchedulerService;

/**
 * @author Tom Baeyens
 * @deprecated replaced by {@link EntitySchedulerService}
 */
public class EjbSchedulerService implements SchedulerService {
  
  private static final long serialVersionUID = 1L;

  JobSession jobSession;
  Session session;
  LocalTimerService timerService;
  
  public EjbSchedulerService(LocalTimerServiceHome timerServiceHome) {
    JbpmContext jbpmContext = JbpmContext.getCurrentJbpmContext();
    if (jbpmContext==null) {
      throw new JbpmException("instantiation of the EjbSchedulerService requires a current JbpmContext");
    }
    this.jobSession = jbpmContext.getJobSession();
    this.session = jbpmContext.getSession();

    try {
      timerService = timerServiceHome.create();
    } catch (CreateException e) {
      throw new JbpmException("ejb local timer creation problem", e);
    }
  }

  public void createTimer(Timer timer) {
    log.debug("creating " + timer);
    jobSession.saveJob(timer);
    session.flush();
    timerService.createTimer(timer);
  }

  public void deleteTimer(Timer timer) {
    log.debug("deleting " + timer);
    timerService.cancelTimer(timer);
    jobSession.deleteJob(timer);
  }

  public void deleteTimersByName(String timerName, Token token) {
    log.debug("deleting timers by name '" + timerName  + "' for " + token);
    timerService.cancelTimersByName(timerName, token);
    jobSession.deleteTimersByName(timerName, token);
  }

  public void deleteTimersByProcessInstance(ProcessInstance processInstance) {
    log.debug("deleting timers for " + processInstance);
    timerService.cancelTimersForProcessInstance(processInstance);
    jobSession.deleteJobsForProcessInstance(processInstance);
  }

  public void close() {
    try {
      log.debug("removing the timer service session bean");
      timerService.remove();
    } catch (RemoveException e) {
      throw new JbpmException("ejb local timer service close problem", e);
    }
  }
  
  private static Log log = LogFactory.getLog(EjbSchedulerService.class);
}
