package org.jbpm.job.executor;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbpm.JbpmConfiguration;
import org.jbpm.job.Job;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@SuppressWarnings({
  "rawtypes", "unchecked"
})
public class JobExecutor implements Serializable {

  private static final long serialVersionUID = 1L;

  protected JbpmConfiguration jbpmConfiguration;
  protected String name;
  protected int nbrOfThreads;
  protected int idleInterval;
  protected int maxIdleInterval;
  private int retryInterval;

  protected int maxLockTime;
  protected int lockMonitorInterval;

  private transient ThreadGroup threadGroup;
  private int waitingExecutorCount ;
  private boolean waitingDispatcher ;
  private boolean dispatcherActive ;
  private Lock waitingExecutorLock = new ReentrantLock() ;
  private Condition waitingExecutorCondition = waitingExecutorLock.newCondition() ;
  private Condition waitingDispatcherCondition = waitingExecutorLock.newCondition() ;
  private LinkedList dispatchedJobs = new LinkedList();

  protected Map monitoredJobIds = new Hashtable();
  protected boolean isStarted;

  /**
   * Starts all the threads needed. 
   * 
   * This method has been split into smaller methods
   *  in order to make the logic testable.
   */
  public synchronized void start() {
    if (!isStarted) {
      log.info("starting " + name);

      activateDispatcher() ;
      
      // create thread group
      threadGroup = new ThreadGroup(name) {
        public void uncaughtException(Thread thread, Throwable throwable) {
          if (thread instanceof JobExecutorThread) {
            startThread(thread.getName());
          }
          else if (thread instanceof DispatcherThread) {
            startDispatcherThread();
          }
          else if (thread instanceof LockMonitorThread) {
            startLockMonitorThread();
          }
          super.uncaughtException(thread, throwable);
        }
      };

      // start executor threads
      for (int i = 1; i <= nbrOfThreads; i++) {
        startThread(getThreadName(i));
      }

      // start control threads
      startDispatcherThread();
      startLockMonitorThread();
      isStarted = true;
    }
    else if (log.isDebugEnabled()) {
      log.debug("ignoring start: " + name + " already started");
    }
  }
    
  /**
   * tells all threads in this job executor to stop. Threads may be in the middle of processing
   * a job and they will finish that first. Use {@link #stopAndJoin()} in case you want a method
   * that blocks until all the threads are actually finished.
   * 
   * @return a list of the stopped threads. In case no threads were stopped an empty list will
   * be returned.
   */
  public synchronized List stop() {
    if (!isStarted) {
      if (log.isDebugEnabled()) log.debug("ignoring stop, " + name + " not started");
      return Collections.EMPTY_LIST;
    }

    log.info("stopping " + name);
    isStarted = false;

    // fetch active threads
    Thread[] activeThreads = new Thread[nbrOfThreads + 2];
    int activeCount = threadGroup.enumerate(activeThreads, false);

    // deactivate threads
    List deactivatedThreads = new ArrayList(activeCount);
    for (int i = 0; i < activeCount; i++) {
      Thread thread = activeThreads[i];
      if (thread instanceof Deactivable) {
        Deactivable deactivable = (Deactivable) thread;
        deactivable.deactivate();
        deactivatedThreads.add(thread);
      }
    }

    deactivateDispatcher() ;
    
    // return deactivated threads
    return deactivatedThreads;
  }

  public void stopAndJoin() throws InterruptedException {
    // deactivate threads
    List threads = stop();

    // join deactivated threads
    for (Iterator i = threads.iterator(); i.hasNext();) {
      Thread thread = (Thread) i.next();
      thread.join();
    }
  }

  public void ensureThreadsAreActive() {
    Map threads = getThreads();

    // check executor threads
    for (int i = 1; i <= nbrOfThreads; i++) {
      String threadName = getThreadName(i);
      if (!threads.containsKey(threadName)) {
        startThread(threadName);
      }
    }

    // check control threads
    if (!threads.containsKey(getDispatcherThreadName())) {
      startDispatcherThread();
    }
    if (!threads.containsKey(getLockMonitorThreadName())) {
      startLockMonitorThread();
    }
  }

  ThreadGroup getThreadGroup() {
    return threadGroup;
  }

  private String getThreadName(int index) {
    return name + '@' + getHostAddress() + ":Executor-" + index;
  }

  protected void startThread(String threadName) {
    Thread thread = createThread(threadName);

    if (log.isDebugEnabled()) log.debug("starting " + threadName);
    thread.start();
  }

  protected Thread createThread(String threadName) {
    return new JobExecutorThread(threadName, this);
  }

  private String getDispatcherThreadName() {
    return name + '@' + getHostAddress() + ':' + DispatcherThread.DEFAULT_NAME;
  }

  protected Thread createDispatcherThread(String threadName) {
	    return new DispatcherThread(threadName, this);
  }

  void startDispatcherThread() {
    String threadName = getDispatcherThreadName();
    Thread dispatcherThread = createDispatcherThread(threadName);

    if (log.isDebugEnabled()) log.debug("starting " + threadName);
    dispatcherThread.start();
  }

  private String getLockMonitorThreadName() {
    return name + '@' + getHostAddress() + ':' + LockMonitorThread.DEFAULT_NAME;
  }

  protected Thread createLockMonitorThread(String threadName) {
	    return new LockMonitorThread(threadName, this);
  }
  
  void startLockMonitorThread() {
    String threadName = getLockMonitorThreadName();
    Thread lockMonitorThread = createLockMonitorThread(threadName);

    if (log.isDebugEnabled()) log.debug("starting " + threadName);
    lockMonitorThread.start();
  }

  private static String getHostAddress() {
    try {
      return InetAddress.getLocalHost().getHostAddress();
    }
    catch (UnknownHostException e) {
      return "127.0.0.1";
    }
  }

  public Set getMonitoredJobIds() {
    return new HashSet(monitoredJobIds.values());
  }

  public void addMonitoredJobId(String threadName, long jobId) {
    monitoredJobIds.put(threadName, new Long(jobId));
  }

  public void removeMonitoredJobId(String threadName) {
    monitoredJobIds.remove(threadName);
  }

  public int getIdleInterval() {
    return idleInterval;
  }

  public void setIdleInterval(int idleInterval) {
    if (idleInterval <= 0) {
      throw new IllegalArgumentException("idle interval must be positive");
    }
    this.idleInterval = idleInterval;
  }

  /**
   * Tells whether this job executor has been {@linkplain #start() started}.
   */
  public boolean isStarted() {
    return isStarted;
  }

  public JbpmConfiguration getJbpmConfiguration() {
    return jbpmConfiguration;
  }

  public void setJbpmConfiguration(JbpmConfiguration jbpmConfiguration) {
    this.jbpmConfiguration = jbpmConfiguration;
  }

  public int getRetryInterval() {
    return retryInterval;
  }

  public void setRetryInterval(int retryInterval) {
    if (retryInterval <= 0) {
      throw new IllegalArgumentException("retry interval must be positive");
    }
    this.retryInterval = retryInterval;
  }

  public int getMaxIdleInterval() {
    return maxIdleInterval;
  }

  public void setMaxIdleInterval(int maxIdleInterval) {
    if (maxIdleInterval <= 0) {
      throw new IllegalArgumentException("max idle interval must be positive");
    }
    this.maxIdleInterval = maxIdleInterval;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Map getThreads() {
    // fetch active threads
    Thread[] activeThreads = new Thread[nbrOfThreads + 2];
    int activeCount = threadGroup.enumerate(activeThreads, false);

    // map threads by name
    Map threadMap = new HashMap(activeCount);
    for (int i = 0; i < activeCount; i++) {
      Thread thread = activeThreads[i];
      threadMap.put(thread.getName(), thread);
    }
    return threadMap;
  }

  public int getMaxLockTime() {
    return maxLockTime;
  }

  public void setMaxLockTime(int maxLockTime) {
    if (maxLockTime <= 0) {
      throw new IllegalArgumentException("max lock time must be positive");
    }
    this.maxLockTime = maxLockTime;
  }

  public int getLockMonitorInterval() {
    return lockMonitorInterval;
  }

  public void setLockMonitorInterval(int lockMonitorInterval) {
    if (lockMonitorInterval <= 0) {
      throw new IllegalArgumentException("lock monitor interval must be positive");
    }
    this.lockMonitorInterval = lockMonitorInterval;
  }

  public int getNbrOfThreads() {
    return nbrOfThreads;
  }

  public void setNbrOfThreads(int nbrOfThreads) {
    if (nbrOfThreads <= 0) {
      throw new IllegalArgumentException("number of threads must be positive");
    }
    this.nbrOfThreads = nbrOfThreads;
  }

  private boolean hasFreeExecutor() {
    waitingExecutorLock.lock() ;
    try {
      return (waitingExecutorCount > dispatchedJobs.size()) ;
    } finally {
      waitingExecutorLock.unlock() ;
    }
  }

  // return false when interrupted
  boolean waitForFreeExecutorThread() {
    waitingExecutorLock.lock() ;
    try {
      waitingDispatcher = true ;
      if (dispatcherActive) {
        if (hasFreeExecutor()) {
          return true ;
        } else {
          waitingDispatcherCondition.await() ;
          return hasFreeExecutor() ;
        }
      }
    } catch (final InterruptedException ie) {
    } finally {
      waitingDispatcher = false ;
      waitingExecutorLock.unlock() ;
    }
    return false ;
  }
  
  // return null when interrupted
  Job getJob() {
    waitingExecutorLock.lock() ;
    try {
      waitingExecutorCount++ ;
      if (dispatcherActive) { 
        if (waitingDispatcher && hasFreeExecutor()) {
          waitingDispatcherCondition.signal() ;
        }
        if (dispatchedJobs.isEmpty()) {
          waitingExecutorCondition.await() ;
        }
        if (dispatchedJobs.size() > 0) {
          return (Job)dispatchedJobs.remove(0) ;
        }
      }
    } catch (final InterruptedException ie) {
    } finally {
      waitingExecutorCount-- ;
      waitingExecutorLock.unlock() ;
    }
    return null ;
  }
  
  boolean submitJob(final Job job) {
    waitingExecutorLock.lock() ;
    try {
      if (hasFreeExecutor()) {
        dispatchedJobs.add(job) ;
        waitingExecutorCondition.signal() ;
        return true ;
      }
    } finally {
      waitingExecutorLock.unlock() ;
    }
    return false ;
  }

  private void activateDispatcher() {
    waitingExecutorLock.lock() ;
    dispatcherActive = true ;
    waitingExecutorLock.unlock() ;
  }
  
  private void deactivateDispatcher() {
    waitingExecutorLock.lock() ;
    try {
      dispatcherActive = false ;
      waitingDispatcherCondition.signal() ;
      waitingExecutorCondition.signalAll() ;
    } finally {
      waitingExecutorLock.unlock() ;
    }
  }
  
  private static Log log = LogFactory.getLog(JobExecutor.class);
  
  /**
   * ====================
   * CRUFT! CRUFT! CRUFT!
   * ====================
   */
   
  /** @deprecated property has no effect */
  protected int historyMaxSize;
  /** @deprecated property has no effect */
  protected int lockBufferTime;
  /** @deprecated call {@link #getThreads()} instead */
  protected Map threads;
  /** @deprecated call {@link #getThreads()} instead */
  protected transient LockMonitorThread lockMonitorThread;
  /** @deprecated this field was just an aid for generating thread names */
  protected static String hostName;

  /**
   * @deprecated call {@link #addMonitoredJobId(String, long)} or
   * {@link #removeMonitoredJobId(String)} to manipulate the set of monitored jobs
   */
  public void setMonitoredJobIds(Map monitoredJobIds) {
  }

  /** @deprecated property has no effect */
  public int getHistoryMaxSize() {
    return historyMaxSize;
  }

  /** @deprecated property has no effect */
  public void setHistoryMaxSize(int historyMaxSize) {
    this.historyMaxSize = historyMaxSize;
  }

  /**
   * This method has no effect.
   * 
   * @deprecated call {@link #start()} or {@link #stop()} to control this job executor.
   */
  public void setStarted(boolean isStarted) {
  }

  /**
   * @deprecated replaced by {@link #getNbrOfThreads()}
   */
  public int getSize() {
    return nbrOfThreads;
  }

  /**
   * @deprecated replaced by {@link #setNbrOfThreads(int)}
   */
  public void setSize(int nbrOfThreads) {
    this.nbrOfThreads = nbrOfThreads;
  }
  
  /**
   * This method has no effect.
   * 
   * @deprecated this job executor manages its own thread pool
   */
  public void setThreads(Map threads) {
  }

  /** @deprecated property has no effect */
  public int getLockBufferTime() {
    return lockBufferTime;
  }

  /** @deprecated property has no effect */
  public void setLockBufferTime(int lockBufferTime) {
    this.lockBufferTime = lockBufferTime;
  }
}
