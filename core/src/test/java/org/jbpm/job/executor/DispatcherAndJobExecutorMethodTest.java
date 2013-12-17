package org.jbpm.job.executor;


import java.util.Calendar;
import java.util.Date;

import junit.framework.TestCase;

public class DispatcherAndJobExecutorMethodTest extends TestCase {

  private DispatcherThread dispatcherThread = null;
  private JobExecutor jobExecutor = null;
  private JobExecutorThread jobExecutorThread = null;
  
  public void setUp() { 
    jobExecutor = new JobExecutor();
    dispatcherThread = new DispatcherThread("Test-Dispatcher", jobExecutor);
    jobExecutorThread = new JobExecutorThread("Test-JobExecutorThread", jobExecutor);
  }
  
  public void tearDown() { 
    jobExecutor = null;
    dispatcherThread = null;
    jobExecutorThread = null;
  }
 
  public void testGetWaitPeriod() { 
    long currentIdleInterval = 20 * 1000;
    
    Calendar cal = Calendar.getInstance();
    cal.add(Calendar.SECOND, -10);
    Date nextDueDate = cal.getTime();
    
    long waitPeriod = DispatcherThread.getWaitPeriod((int) currentIdleInterval, nextDueDate);
    assertTrue( "The due date has passed, we expect no wait period, not " + waitPeriod/1000, waitPeriod <= 0 );
    
    cal = Calendar.getInstance();
    cal.add(Calendar.SECOND, 30);
    nextDueDate = cal.getTime();
    
    waitPeriod = DispatcherThread.getWaitPeriod((int) currentIdleInterval, nextDueDate);
    assertTrue( "The due date is past the idle interval, we expect a wait period of " + currentIdleInterval + " not " + waitPeriod,
      waitPeriod == currentIdleInterval );
    
    cal = Calendar.getInstance();
    cal.add(Calendar.SECOND, 10);
    nextDueDate = cal.getTime();
    
    waitPeriod = DispatcherThread.getWaitPeriod((int) currentIdleInterval, nextDueDate);
    assertTrue( "The due date is past the idle interval, we expect a wait period of approximately " + 10 * 1000 + " not " + waitPeriod,
      9 * 1000 <= waitPeriod && waitPeriod <= 11 * 1000 );
  }
  
}

