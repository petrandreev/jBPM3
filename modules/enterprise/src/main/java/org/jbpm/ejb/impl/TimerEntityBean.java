package org.jbpm.ejb.impl;

import java.util.Collection;

import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.EntityBean;
import javax.ejb.EntityContext;
import javax.ejb.TimedObject;
import javax.ejb.TimerService;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbpm.ejb.LocalCommandService;
import org.jbpm.ejb.LocalCommandServiceHome;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.job.Timer;
import org.jbpm.scheduler.ejbtimer.ExecuteTimerCommand;
import org.jbpm.scheduler.ejbtimer.TimerInfo;
import org.jbpm.util.CollectionUtil;

/**
 * Entity bean that interacts with the EJB timer service to schedule jBPM {@linkplain Timer timers}.
 * <h3>Environment</h3>
 * <p>
 * The environment entries and resources available for customization are summarized in the table
 * below.
 * </p>
 * <table border="1">
 * <tr>
 * <th>Name</th>
 * <th>Type</th>
 * <th>Description</th>
 * </tr>
 * <tr>
 * <td><code>ejb/LocalCommandServiceBean</code></td>
 * <td>EJB Reference</td>
 * <td>Link to the local {@linkplain CommandServiceBean session bean} that executes timers on a
 * separate jBPM context.</td>
 * </tr>
 * </table>
 * 
 * @author Tom Baeyens
 * @author Alejandro Guizar
 * @author Fady Matar
 */
public abstract class TimerEntityBean implements EntityBean, TimedObject {
  private EntityContext entityContext;
  private LocalCommandService commandService;

  private static final long serialVersionUID = 1L;
  private static final Log log = LogFactory.getLog(TimerEntityBean.class);

  public abstract Long getTimerId();

  public abstract void setTimerId(Long timerId);

  public abstract String getName();

  public abstract void setName(String name);

  public abstract Long getTokenId();

  public abstract void setTokenId(Long tokenId);

  public abstract Long getProcessInstanceId();

  public abstract void setProcessInstanceId(Long processInstanceId);

  public abstract String getDiscriminator();

  public abstract void setDiscriminator(String discriminator);

  public void ejbActivate() {
    try {
      Context initial = new InitialContext();
      LocalCommandServiceHome commandServiceHome = (LocalCommandServiceHome) initial.lookup("java:comp/env/ejb/LocalCommandServiceBean");
      commandService = commandServiceHome.create();
    }
    catch (NamingException e) {
      throw new EJBException("failed to retrieve command service home", e);
    }
    catch (CreateException e) {
      throw new EJBException("command service creation failed", e);
    }
  }

  public void ejbPassivate() {
    commandService = null;
  }

  public void ejbRemove() {
    commandService = null;
  }

  public void ejbLoad() {
  }

  public void ejbStore() {
  }

  public void setEntityContext(EntityContext entityContext) {
    this.entityContext = entityContext;
  }

  public void unsetEntityContext() {
    entityContext = null;
  }

  /**
   * No ejbCreate operation is allowed. One approach of ensuring that an EJB is set as read-only.
   * 
   * @throws CreateException
   */
  public Long ejbCreate() throws CreateException {
    throw new CreateException("direct creation of timer entities is prohibited");
  }

  public void ejbPostCreate() {
  }

  public void ejbTimeout(javax.ejb.Timer ejbTimer) {
    log.debug(ejbTimer + " fired");
    TimerInfo timerInfo = (TimerInfo) ejbTimer.getInfo();
    Timer timer = (Timer) commandService.execute(new ExecuteTimerCommand(timerInfo.getTimerId()));
    // if the timer has repeat
    if (timer.getRepeat() != null) {
      // create a new timer
      log.debug("scheduling timer for repeat on " + timer.getDueDate());
      createTimer(timer);
    }
  }

  public void createTimer(org.jbpm.job.Timer timer) {
    TimerService timerService = entityContext.getTimerService();
    javax.ejb.Timer ejbTimer = timerService.createTimer(timer.getDueDate(), new TimerInfo(timer));
    log.debug("created " + ejbTimer);
  }

  public void cancelTimer(org.jbpm.job.Timer timer) {
    long timerId = timer.getId();
    Collection<javax.ejb.Timer> timers = CollectionUtil.checkCollection(entityContext.getTimerService()
        .getTimers(), javax.ejb.Timer.class);
    log.debug("retrieved " + timers.size() + " ejb timer(s) by id " + timerId);

    int count = 0;
    for (javax.ejb.Timer ejbTimer : timers) {
      TimerInfo timerInfo = (TimerInfo) ejbTimer.getInfo();
      if (timerInfo.getTimerId() == timerId) {
        ejbTimer.cancel();
        ++count;
      }
    }
    log.debug("canceled " + count + " ejb timer(s) by id " + timerId);
  }

  public void cancelTimersByName(String timerName, Token token) {
    Collection<javax.ejb.Timer> timers = CollectionUtil.checkCollection(entityContext.getTimerService()
        .getTimers(), javax.ejb.Timer.class);
    log.debug("retrieved "
        + timers.size()
        + " ejb timer(s) by name '"
        + timerName
        + "' for "
        + token);

    int count = 0;
    for (javax.ejb.Timer ejbTimer : timers) {
      TimerInfo timerInfo = (TimerInfo) ejbTimer.getInfo();
      if (timerInfo.matchesName(timerName, token)) {
        ejbTimer.cancel();
        ++count;
      }
    }
    log.debug("canceled " + count + " ejb timer(s) by name '" + timerName + "' for " + token);
  }

  public void cancelTimersForProcessInstance(ProcessInstance processInstance) {
    Collection<javax.ejb.Timer> timers = CollectionUtil.checkCollection(entityContext.getTimerService()
        .getTimers(), javax.ejb.Timer.class);
    log.debug("retrieved " + timers.size() + " timer(s) for " + processInstance);

    int count = 0;
    for (javax.ejb.Timer ejbTimer : timers) {
      TimerInfo timerInfo = (TimerInfo) ejbTimer.getInfo();
      if (timerInfo.matchesProcessInstance(processInstance)) {
        ejbTimer.cancel();
        ++count;
      }
    }
    log.debug("canceled " + count + " ejb timer(s) for " + processInstance);
  }

}
