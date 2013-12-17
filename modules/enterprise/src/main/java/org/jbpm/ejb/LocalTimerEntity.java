package org.jbpm.ejb;

import javax.ejb.EJBLocalObject;

import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.job.Timer;

public interface LocalTimerEntity extends EJBLocalObject {

  public void createTimer(Timer timer);

  public void cancelTimer(Timer timer);

	public void cancelTimersByName(String timerName, Token token);

	public void cancelTimersForProcessInstance(ProcessInstance processInstance);

}