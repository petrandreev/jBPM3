package org.jbpm.ejb;

import java.rmi.RemoteException;

import javax.ejb.EJBObject;

import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.job.Timer;

public interface TimerEntity extends EJBObject
{
  public void createTimer(Timer timer) throws RemoteException;

  public void cancelTimer(Timer timer) throws RemoteException;

  public void cancelTimersByName(String timerName, Token token) throws RemoteException;

  public void cancelTimersForProcessInstance(ProcessInstance processInstance) throws RemoteException;

}