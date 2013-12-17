package org.jbpm.job;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.jbpm.JbpmContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.taskmgmt.exe.TaskInstance;

public abstract class Job implements Serializable {

  private static final long serialVersionUID = 1L;
  private static final DateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss,SSS");

  long id;
  int version;

  Date dueDate;

  ProcessInstance processInstance;
  Token token;
  TaskInstance taskInstance;
  
  boolean isSuspended;

  /**
   * specifies if this job can be executed concurrently with other jobs for the 
   * same process instance. 
   */
  boolean isExclusive;

  /**
   * name of the job executor thread that has locked this job.
   */
  String lockOwner;

  /**
   * the time the job executor thread locked this job.
   */
  Date lockTime;

  String exception;
  int retries = 1;
  
  /**
   * The configuration property was used in earlier
   * versions and is a legacy artifact.
   * 
   * But it can be easily used to remember additional
   * information for a job.
   * 
   * By default it is <b>not</b> persistent, but you could
   * use your own hibernate mapping to do so.
   * 
   * see <a href="http://www.jboss.org/index.html?module=bb&op=viewtopic&t=151249">Forum</b>
   */
  String configuration;
 
  public Job() {
  }

  public Job(Token token) {
    this.token = token;
    this.processInstance = token.getProcessInstance();
  }

  public abstract boolean execute(JbpmContext jbpmContext) throws Exception;
  
  public String toString() {
    return "Job("+id+')';
  }
  
  public String toStringLongFormat() {
    return "id="+id
       + ", version="+version
       + ", dueDate="+(dueDate!=null ? dateFormat.format(dueDate) : null)
       + ", isSuspended="+isSuspended
       + ", isExclusive="+isExclusive
       + ", lockOwner="+lockOwner
       + ", lockTime="+lockTime
       + ", exception="+exception
       + ", retries="+retries
       + ", configuration="+configuration;
  }

  public ProcessInstance getProcessInstance() {
    return processInstance;
  }
  public void setProcessInstance(ProcessInstance processInstance) {
    this.processInstance = processInstance;
  }
  public Token getToken() {
    return token;
  }
  public void setToken(Token token) {
    this.token = token;
  }
  public long getId() {
    return id;
  }
  public Date getAqcuireDate() {
    return lockTime;
  }
  public void setLockTime(Date aqcuireDate) {
    this.lockTime = aqcuireDate;
  }
  public Date getDueDate() {
    return dueDate;
  }
  public void setDueDate(Date dueDate) {
    this.dueDate = dueDate;
  }
  public String getException() {
    return exception;
  }
  public void setException(String exception) {
    this.exception = exception;
  }
  public boolean isExclusive() {
    return isExclusive;
  }
  public void setExclusive(boolean isExclusive) {
    this.isExclusive = isExclusive;
  }
  public String getJobExecutorName() {
    return lockOwner;
  }
  public void setLockOwner(String jobExecutorName) {
    this.lockOwner = jobExecutorName;
  }
  public int getRetries() {
    return retries;
  }
  public void setRetries(int retries) {
    this.retries = retries;
  }
  public TaskInstance getTaskInstance() {
    return taskInstance;
  }
  public void setTaskInstance(TaskInstance taskInstance) {
    this.taskInstance = taskInstance;
  }
  public String getConfiguration() {
    return configuration;
  }
  public void setConfiguration(String configuration) {
    this.configuration = configuration;
  }
  public String getLockOwner() {
    return lockOwner;
  }
  public Date getLockTime() {
    return lockTime;
  }
  public boolean isSuspended() {
    return isSuspended;
  }
  public void setSuspended(boolean isSuspended) {
    this.isSuspended = isSuspended;
  }
  public int getVersion() {
    return version;
  }
}
