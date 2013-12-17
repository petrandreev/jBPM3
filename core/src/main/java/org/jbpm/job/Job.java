package org.jbpm.job;

import java.io.Serializable;
import java.util.Date;

import org.jbpm.JbpmConfiguration.Configs;
import org.jbpm.JbpmContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.taskmgmt.exe.TaskInstance;

public abstract class Job implements Serializable {

  private static final long serialVersionUID = 1L;

  private long id;
  private int version;

  private Date dueDate;

  private ProcessInstance processInstance;
  private Token token;
  private TaskInstance taskInstance;

  private boolean isSuspended;
  private boolean isExclusive;

  private String lockOwner;
  private Date lockTime;

  private String exception;
  private int retries = Configs.getInt("jbpm.job.retries");

  private String configuration;

  public Job() {
  }

  public Job(Token token) {
    this.token = token;
    this.processInstance = token.getProcessInstance();
  }

  public abstract boolean execute(JbpmContext jbpmContext) throws Exception;

  public String toStringLongFormat() {
    return "Job(id=" + id
       + ", version=" + version
       + ", dueDate=" + (dueDate != null ? dueDate : null)
       + ", suspended=" + isSuspended
       + ", exclusive=" + isExclusive
       + ", lockOwner=" + lockOwner
       + ", lockTime=" + lockTime
       + ", exception=" + exception
       + ", retries=" + retries
       + ", configuration=" + configuration;
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

  /**
   * what time the executor locked this job.
   */
  public Date getLockTime() {
    return lockTime;
  }

  public void setLockTime(Date lockTime) {
    this.lockTime = lockTime;
  }

  /** @deprecated call {@link #getLockTime()} instead */
  public Date getAqcuireDate() {
    return lockTime;
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

  /**
   * whether this job can be executed concurrently with other jobs for the same process
   * instance.
   */
  public boolean isExclusive() {
    return isExclusive;
  }

  public void setExclusive(boolean isExclusive) {
    this.isExclusive = isExclusive;
  }

  /**
   * name of the executor that locked this job.
   */
  public String getLockOwner() {
    return lockOwner;
  }

  public void setLockOwner(String lockOwner) {
    this.lockOwner = lockOwner;
  }

  /** @deprecated call {@link #getLockOwner()} instead */
  public String getJobExecutorName() {
    return lockOwner;
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
