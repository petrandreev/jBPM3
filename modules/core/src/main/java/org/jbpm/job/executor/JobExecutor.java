package org.jbpm.job.executor;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbpm.JbpmConfiguration;

public class JobExecutor implements Serializable {

  private static final long serialVersionUID = 1L;

  protected JbpmConfiguration jbpmConfiguration;
  protected String name;
  protected int nbrOfThreads;
  protected int idleInterval;
  protected int maxIdleInterval;
  /** @deprecated this field was never used */
  @Deprecated
  protected int historyMaxSize;

  protected int maxLockTime;
  protected int lockMonitorInterval;
  protected int lockBufferTime;

  protected Map<String, Thread> threads = new HashMap<String, Thread>();
  protected LockMonitorThread lockMonitorThread;
  protected Map<String, Long> monitoredJobIds = new Hashtable<String, Long>();

  protected boolean isStarted = false;

  private static String hostName;

  public synchronized void start() {
    if (!isStarted) {
      log.debug("starting job executor '" + name + "'...");
      for (int i = 0; i < nbrOfThreads; i++) {
        startThread();
      }
      lockMonitorThread = new LockMonitorThread(this);
      isStarted = true;
    }
    else {
      log.debug("ignoring start: job executor '" + name + "' is already started'");
    }
  }

  /**
   * signals to all threads in this job executor to stop. Threads may be in the middle of processing
   * a job and they will finish that first. Use {@link #stopAndJoin()} in case you want a method
   * that blocks until all the threads are actually finished.
   * 
   * @return a list of the stopped threads. In case no threads were stopped an empty list will be
   * returned.
   */
  public synchronized List<Thread> stop() {
    List<Thread> stoppedThreads;
    if (isStarted) {
      log.debug("stopping job executor '" + name + "'...");
      isStarted = false;

      stoppedThreads = new ArrayList<Thread>(threads.size());
      for (int i = 0; i < nbrOfThreads; i++) {
        stoppedThreads.add(stopThread());
      }

      if (lockMonitorThread != null) lockMonitorThread.deactivate();
    }
    else {
      log.debug("ignoring stop: job executor '" + name + "' not started");
      stoppedThreads = Collections.emptyList();
    }
    return stoppedThreads;
  }

  public void stopAndJoin() throws InterruptedException {
    for (Thread thread : stop()) {
      thread.join();
    }

    if (lockMonitorThread != null) lockMonitorThread.join();
  }

  protected synchronized void startThread() {
    String threadName = getNextThreadName();
    Thread thread = createThread(threadName);
    threads.put(threadName, thread);

    log.debug("starting new job executor thread '" + threadName + "'");
    thread.start();
  }

  protected Thread createThread(String threadName) {
    return new JobExecutorThread(threadName, this);
  }

  protected String getNextThreadName() {
    return getThreadName(threads.size() + 1);
  }

  protected String getLastThreadName() {
    return getThreadName(threads.size());
  }

  private String getThreadName(int index) {
    return name + '@' + getHostAddress() + ':' + index;
  }

  private static String getHostAddress() {
    if (hostName == null) {
      try {
        hostName = InetAddress.getLocalHost().getHostAddress();
      }
      catch (UnknownHostException e) {
        hostName = "127.0.0.1";
      }
    }
    return hostName;
  }

  protected synchronized Thread stopThread() {
    String threadName = getLastThreadName();
    log.debug("removing job executor thread '" + threadName + "'");

    Thread thread = threads.remove(threadName);
    if (thread instanceof JobExecutorThread) {
      JobExecutorThread jobThread = (JobExecutorThread) thread;
      jobThread.deactivate();
    }
    return thread;
  }

  public Set<Long> getMonitoredJobIds() {
    return new HashSet<Long>(monitoredJobIds.values());
  }

  public void addMonitoredJobId(String threadName, long jobId) {
    monitoredJobIds.put(threadName, new Long(jobId));
  }

  public void removeMonitoredJobId(String threadName) {
    monitoredJobIds.remove(threadName);
  }

  /**
   * @throws UnsupportedOperationException to prevent invocation
   * @deprecated <code>monitoredJobIds</code> is an internal control field
   */
  @Deprecated
  public void setMonitoredJobIds(Map<String, Long> monitoredJobIds) {
    throw new UnsupportedOperationException();
  }

  /** @deprecated this property was never used */
  @Deprecated
  public int getHistoryMaxSize() {
    return historyMaxSize;
  }

  /** @deprecated this property was never used */
  @Deprecated
  public void setHistoryMaxSize(int historyMaxSize) {
    this.historyMaxSize = historyMaxSize;
  }

  public int getIdleInterval() {
    return idleInterval;
  }

  public void setIdleInterval(int idleInterval) {
    this.idleInterval = idleInterval;
  }

  /**
   * Tells whether this job executor has been {@linkplain #start() started}.
   */
  public boolean isStarted() {
    return isStarted;
  }

  /**
   * @throws UnsupportedOperationException to prevent invocation
   * @deprecated <code>isStarted</code> is an internal control field
   */
  @Deprecated
  public void setStarted(boolean isStarted) {
    throw new UnsupportedOperationException();
  }

  public JbpmConfiguration getJbpmConfiguration() {
    return jbpmConfiguration;
  }

  public void setJbpmConfiguration(JbpmConfiguration jbpmConfiguration) {
    this.jbpmConfiguration = jbpmConfiguration;
  }

  public int getMaxIdleInterval() {
    return maxIdleInterval;
  }

  public void setMaxIdleInterval(int maxIdleInterval) {
    this.maxIdleInterval = maxIdleInterval;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  /**
   * @deprecated Replaced by {{@link #getNbrOfThreads()}
   */
  @Deprecated
  public int getSize() {
    return nbrOfThreads;
  }

  /**
   * @deprecated Replaced by {@link #setNbrOfThreads(int)}
   */
  @Deprecated
  public void setSize(int nbrOfThreads) {
    this.nbrOfThreads = nbrOfThreads;
  }

  public Map<String, Thread> getThreads() {
    return threads;
  }

  /**
   * @throws UnsupportedOperationException to prevent invocation
   * @deprecated <code>threads</code> is an internal control field
   */
  @Deprecated
  public void setThreads(Map<String, Thread> threads) {
    throw new UnsupportedOperationException();
  }

  public int getMaxLockTime() {
    return maxLockTime;
  }

  public void setMaxLockTime(int maxLockTime) {
    this.maxLockTime = maxLockTime;
  }

  public int getLockBufferTime() {
    return lockBufferTime;
  }

  public void setLockBufferTime(int lockBufferTime) {
    this.lockBufferTime = lockBufferTime;
  }

  public int getLockMonitorInterval() {
    return lockMonitorInterval;
  }

  public void setLockMonitorInterval(int lockMonitorInterval) {
    this.lockMonitorInterval = lockMonitorInterval;
  }

  public int getNbrOfThreads() {
    return nbrOfThreads;
  }

  public void setNbrOfThreads(int nbrOfThreads) {
    this.nbrOfThreads = nbrOfThreads;
  }

  private static Log log = LogFactory.getLog(JobExecutor.class);
}
