package org.jbpm.scheduler.ejbtimer;

import java.util.Collection;

import javax.ejb.FinderException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;
import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.jbpm.db.JobSession;
import org.jbpm.ejb.LocalTimerEntity;
import org.jbpm.ejb.LocalTimerEntityHome;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.job.Timer;
import org.jbpm.scheduler.SchedulerService;

public class EntitySchedulerService implements SchedulerService {

	private static final long serialVersionUID = 1L;

	JobSession jobSession;
	Session session;
	LocalTimerEntityHome timerEntityHome;

	public EntitySchedulerService(LocalTimerEntityHome timerEntityHome) {
		JbpmContext jbpmContext = JbpmContext.getCurrentJbpmContext();
		if(jbpmContext == null) {
			throw new JbpmException("entity scheduler service must be created inside a jbpm context");
		}
		this.jobSession = jbpmContext.getJobSession();
		this.session = jbpmContext.getSession();
		this.timerEntityHome = timerEntityHome;
	}

	public void createTimer(Timer timer) {
		log.debug("creating " + timer);
		jobSession.saveJob(timer);
		session.flush();
		try {
			LocalTimerEntity timerEntity = timerEntityHome.findByPrimaryKey(new Long(timer.getId()));
			timerEntity.createTimer(timer);
		}
		catch (FinderException e) {
			log.error("failed to retrieve entity for " + timer, e);
		}
	}

	public void deleteTimer(Timer timer) {
    log.debug("deleting " + timer);
    try {
      LocalTimerEntity timerEntity = timerEntityHome.findByPrimaryKey(new Long(timer.getId()));
      timerEntity.cancelTimer(timer);
    }
    catch (FinderException e) {
      log.error("failed to retrieve entity for " + timer, e);
    }
    jobSession.deleteJob(timer);
  }

  public void deleteTimersByName(String timerName, Token token) {
		try {
		  Collection<LocalTimerEntity> timerEntities = timerEntityHome.findByNameAndTokenId(timerName, new Long(token.getId()));
			log.debug("found " + timerEntities.size() + " timer entities by name '" + timerName +  "' for " + token);
			for (LocalTimerEntity timerEntity : timerEntities) {
			  timerEntity.cancelTimersByName(timerName, token);
      }
		}
		catch (FinderException e) {
			log.error("failed to retrieve timer entities by name '" + timerName + "' for " + token, e);
		}
		jobSession.deleteTimersByName(timerName, token);
	}

	public void deleteTimersByProcessInstance(ProcessInstance processInstance) {
		try {
			Collection<LocalTimerEntity> timerEntities = timerEntityHome.findByProcessInstanceId(new Long(processInstance.getId()));
			log.debug("found " + timerEntities.size() + " timer entities for " + processInstance);
			for (LocalTimerEntity timerEntity : timerEntities) {
			  timerEntity.cancelTimersForProcessInstance(processInstance);
      }
		}
		catch (FinderException e) {
			log.error("failed to retrieve timer entities for " + processInstance, e);
		}
		jobSession.deleteJobsForProcessInstance(processInstance);
	}

	public void close() {
	  timerEntityHome = null;
	}

	private static Log log = LogFactory.getLog(EntitySchedulerService.class);
}
