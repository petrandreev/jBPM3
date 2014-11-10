/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jbpm.jbpm3430;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.hibernate.cfg.Configuration;
import org.jbpm.JbpmConfiguration;
import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.db.JbpmSchema;
import org.jbpm.db.hibernate.HibernateHelper;
import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.def.Event;
import org.jbpm.graph.def.EventCallback;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.job.Timer;
import org.jbpm.jpdl.JpdlException;
import org.jbpm.jpdl.xml.Problem;
import org.jbpm.persistence.db.DbPersistenceServiceFactory;
import org.jbpm.scheduler.db.DbSchedulerService;
import org.jbpm.svc.Services;

/**
 * JobSession.getFirstDueJob was not performing due to the queries having to sort the duedate
 * field without an index.
 * 
 * Which led me to look at the queries, and to discover that the queries were not at all matched
 * with what they were being used for.
 * 
 * The queries where written to retrieve the first due job (and possibly the first due job w/
 * lock owner, or etc.). They were being used to retrieve the next due date.
 * 
 * So I rewrote the queries.
 * 
 * (Needs the postgresql jdbc driver jar to be attached to the build path
 * to work, as well as a local postgresql db, of course.)
 */
@SuppressWarnings({
  "rawtypes", "unchecked"
})
public class JBPM3430Test extends AbstractDbTestCase {

  private static final String PROCESS_NAME = "jbpm3430";

  boolean doNotRunUnlessManually = true;
  
  protected void setUp() throws Exception {
    if( doNotRunUnlessManually ) { 
      return;
    }
    
    super.setUp();
    
    setUpAndEnsureCleanDatabase();
    setupTimerDueDates();

    ProcessDefinition processDefinition = null;
    try { 
      processDefinition = ProcessDefinition.parseXmlString(getProcessDefinitionString());
    } catch( JpdlException je ) { 
      Iterator iter = je.getProblems().iterator();
      while( iter.hasNext() ) { 
        Problem prob = (Problem) iter.next();
        log.error( prob.getDescription() );
      }
      fail( je.getMessage() );
    }
    
    deployProcessDefinition(processDefinition);
  }
  
  /**
   * What do we want? 
   * 
   * 1. The job executor and dispatcher threads start. 
   *   - no jobs available, 
   */
  private void setupTimerDueDates() { 
    jobExecutor = getJbpmConfiguration().getJobExecutor();
    dispatcherIdleInterval = jobExecutor.getIdleInterval();
    assertTrue( "Idle interval is too small: " + dispatcherIdleInterval, dispatcherIdleInterval > 2500 );
    
    withinIdleSeconds = (int) (3 * ((double) dispatcherIdleInterval)/(4 * 1000) + ((double) dispatcherIdleInterval/1000));
    afterIdleSeconds = (int) (3 * ((double) dispatcherIdleInterval)/1000 + 1);
    assertTrue( "Within idle timer duration not larger than 0", withinIdleSeconds > 0 );
    assertTrue( "Within idle timer duration equal to idle interval. [" + withinIdleSeconds + " >= " + dispatcherIdleInterval/1000 + "]", (withinIdleSeconds % (dispatcherIdleInterval/1000)-1) > 0 );
  }
  
  private String postgresDatabaseProperties = "org/jbpm/jbpm3235/hibernate.postgresql.properties";
  
  private void setUpAndEnsureCleanDatabase() {
    String propertiesResource = postgresDatabaseProperties;
    
    /**
     * Postgresql/other database setup
     */
    // Ensure that the hibernate session uses the properties we configured (see propertiesResource above)
    Configuration configuration = HibernateHelper.createConfiguration(null, propertiesResource);
    
    DbPersistenceServiceFactory persistenceServiceFactory = (DbPersistenceServiceFactory) getJbpmConfiguration().getServiceFactory(Services.SERVICENAME_PERSISTENCE);
    if (persistenceServiceFactory == null) fail("Expected to be able to configure persistence, but no peristence service factory can be found.");
    persistenceServiceFactory.setConfiguration(configuration);
  
    /**
     * Clean up jbpm schema in database
     */
    boolean hasLeftOvers = false;
    JbpmSchema jbpmSchema = new JbpmSchema(configuration);
  
    for (Iterator i = jbpmSchema.getRowsPerTable().entrySet().iterator(); i.hasNext();) {
      Map.Entry entry = (Map.Entry) i.next();
      Long count = (Long) entry.getValue();
      if (count.intValue() != 0) {
        hasLeftOvers = true;
        log.debug(getName() + " left " + count + " records in " + entry.getKey());
      }
    }
  
    if (hasLeftOvers) {
      jbpmSchema.cleanSchema();
    }
  }

//  protected void startJobExecutor() { 
//    jobExecutor.start();
//  }
    
  protected void tearDown() throws Exception {
    stopJobExecutor();
    EventCallback.clear();
    
    log.info("### END " + getName() + " ####################");
  }
  
  protected JbpmConfiguration getJbpmConfiguration() {
    if (jbpmConfiguration == null) {
      // use postgresql properties
      jbpmConfiguration = JbpmConfiguration.getInstance("org/jbpm/jbpm3430/jbpm-3430.cfg.xml");
    }
    return jbpmConfiguration;
  } 

  // ACTUAL TEST
  
  
  private static HashMap timerActionDates = new HashMap();
  
  private static long dispatcherIdleInterval;
  private static int withinIdleSeconds;
  private static int afterIdleSeconds;
  private static final String overdueTimerDate = "overDueDate";

  private static final String overdueTimer = "overdue-timer";
  private static final String withinIdleTimer = "pre-idle-timer";
  private static final String afterIdleTimer = "after-idle-timer";
  
  // DBG 
  private static final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss,SSS");

  
  public void testTimerScheduling() throws Exception {
    if( doNotRunUnlessManually ) { 
      return;
    }
    
    // multiple job executors not supported on hsql
    if (getHibernateDialect().indexOf("HSQL") != -1) { 
      return;
    }

    ProcessInstance processInstance = jbpmContext.newProcessInstanceForUpdate(PROCESS_NAME);
    processInstance.getContextInstance().setVariable("eventCallback", new EventCallback());
    
    Calendar overDueDateCal = Calendar.getInstance();
    overDueDateCal.clear(Calendar.MILLISECOND);
    overDueDateCal.add(Calendar.HOUR, -1);
    processInstance.getContextInstance().setVariable(overdueTimerDate, overDueDateCal.getTime());
   
    // start dispatcher, job executor and lock monitor threads
    startJobExecutor();
    
    // kick off process instance
    processInstance.signal();

    // Make sure that the jobExecutor is not notified
    DbSchedulerService schedulerService = (DbSchedulerService) jbpmContext.getServices().getSchedulerService();
    Field hasProducedTimersField = DbSchedulerService.class.getDeclaredField("hasProducedTimers");
    hasProducedTimersField.setAccessible(true);
    hasProducedTimersField.setBoolean(schedulerService, false);
    
    closeJbpmContext();
    try {
      EventCallback.waitForEvent(Event.EVENTTYPE_PROCESS_END);
    }
    finally {
      createJbpmContext();
    }

    assertTrue(timerActionDates.size() == 4);
    
    long startTime = ((Long) timerActionDates.get("start")).longValue();
    { long overdueTime = ((Long) timerActionDates.get(overdueTimer)).longValue();
       assertTrue( "Overdue job ran " + ((double) overdueTime-startTime)/1000 + " secs after process started [ < " + ((double) dispatcherIdleInterval)/1000 + "]", startTime+dispatcherIdleInterval + 500 > overdueTime );
    }
    
    { long withinIdleTime = ((Long) timerActionDates.get(withinIdleTimer)).longValue();
      assertTrue( "Pre idle job did not run " + withinIdleSeconds + " seconds after start [" + ((double) startTime-withinIdleTime)/1000 + "]", isWithin1SecondOf(startTime + withinIdleSeconds*1000, withinIdleTime) );
    }
    
    { long afterIdleTime = ((Long) timerActionDates.get(afterIdleTimer)).longValue();
      assertTrue( "After idle job did not run " + afterIdleSeconds + " seconds after start [" + ((double) startTime-afterIdleTime)/1000 + "]", isWithin1SecondOf(startTime + afterIdleSeconds*1000, afterIdleTime) );
    }
  }

  // ACTUAL TESTS
  //

  boolean isWithin1SecondOf(long compare, long expected) { 
    boolean withinOneSecond = false;
    
    double thisSeconds = ((double) compare)/1000;
    double thatSeconds = ((double) expected)/1000;
   
    if( thatSeconds - 0.5d <= thisSeconds && thisSeconds <= thatSeconds + 0.5 ) { 
      withinOneSecond = true;
    }
    
    return withinOneSecond;
  }


  public static class TimerAction implements ActionHandler {

    private static final long serialVersionUID = -1723705242334355414L;

    public void execute(ExecutionContext executionContext) throws Exception {
      Timer timer = executionContext.getTimer();
      String timerName = timer.getName();
      
      System.out.println("--> " + timerName  + " [" + sdf.format(new Date()) + " <> " + sdf.format(timer.getDueDate()) + "]" );
      
      timerActionDates.put(timerName, new Long(new Date().getTime()));
    }
  }

  
  public static class StartAction implements ActionHandler {

    private static final long serialVersionUID = -8083148121782750200L;

    public void execute(ExecutionContext executionContext) throws Exception {
      System.out.println("START: " + sdf.format(new Date()));
      timerActionDates.put("start", new Long(new Date().getTime()));
    } 
    
  }
  
  private String getProcessDefinitionString() { 
    return 
      "<?xml version='1.0' encoding='UTF-8'?>" + 
      "<process-definition name='jbpm3430' xmlns='urn:jbpm.org:jpdl-3.2'>" + 
      "  <event type='process-end'>" + 
      "    <action expression='#{eventCallback.processEnd}' />" + 
      "  </event>" + 
      ""   + 
      "  <start-state>" + 
      "    <transition to='timers'>" + 
      "      <action class='" + StartAction.class.getName() + "'/>" + 
      "    </transition>" + 
      " </start-state>" + 
      "" + 
      "  <state name='timers'>" + 
      "    <event type='timer'>" + 
      "      <action class='" + TimerAction.class.getName() + "'/>" + 
      "    </event>" + 
      "    <timer duedate='#{" + overdueTimerDate + "}' name='" + overdueTimer + "' />" + 
      "    <timer duedate='" + withinIdleSeconds + " seconds' name='" + withinIdleTimer + "' />" + 
      "    <timer duedate='" + afterIdleSeconds + " seconds' name='" + afterIdleTimer + "' transition='done' />" + 
      "    <transition to='end' name='done' />" + 
      "  </state>" + 
      "" + 
      "  <end-state name='end' />" + 
      "</process-definition>";
  }
  
}
