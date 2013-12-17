package org.jbpm.scheduler.ejbtimer;

import java.io.Serializable;

import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.job.Timer;

public class TimerInfo implements Serializable {
  
  private static final long serialVersionUID = 1L;
  
  // DON'T CHANGE THE SERIALIZED COMPATIBILITY OF THIS CLASS LIGHTLY
  // unlike command messages,  timers will be in the timer db for a long time.  
  // when they fire, they should be deserializable !

  long timerId = -1;
  String timerName;
  long tokenId = -1;
  long processInstanceId = -1;
  
  public TimerInfo(Timer timer) {
    timerId = timer.getId();
    timerName = timer.getName();
    Token token = timer.getToken();
    tokenId = (token!=null ? token.getId() : -1);
    ProcessInstance processInstance = timer.getProcessInstance();
    processInstanceId = (processInstance!=null ? processInstance.getId() : -1);
  }
  
  public long getProcessInstanceId() {
    return processInstanceId;
  }
  public long getTimerId() {
    return timerId;
  }
  public String getTimerName() {
    return timerName;
  }
  public long getTokenId() {
    return tokenId;
  }

  public boolean matchesName(String timerName, Token token) {
    if ( (this.timerName==null)
         || (! this.timerName.equals(timerName))
         || (this.tokenId==-1)
         || (this.tokenId!=token.getId())
       ) {
      return false;
    }
    return true;
  }

  public boolean matchesProcessInstance(ProcessInstance processInstance) {
    if ( (processInstanceId==-1)
         || (processInstance==null)
         || (processInstanceId!=processInstance.getId())
       ) {
      return false;
    }
    return true;
  }
}
