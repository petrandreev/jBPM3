package org.jbpm.graph.log;

import java.util.Date;

import org.jbpm.graph.def.Node;
import org.jbpm.graph.exe.ProcessInstance;

public class ProcessStateLog extends NodeLog {

  private static final long serialVersionUID = 1L;
  
  ProcessInstance subProcessInstance;
  
  public ProcessStateLog() {
  }

  public ProcessStateLog(Node node, Date nodeEnter, Date date, ProcessInstance subProcessInstance) {
    super(node, nodeEnter, date);
    this.subProcessInstance = subProcessInstance;
  }

  public String toString() {
    return "sub-process["+subProcessInstance.getId()+"]";
  }
  
  public ProcessInstance getSubProcessInstance() {
    return subProcessInstance;
  }
  
  public void setSubProcessInstance(ProcessInstance subProcessInstance) {
    this.subProcessInstance = subProcessInstance;
  }
}
