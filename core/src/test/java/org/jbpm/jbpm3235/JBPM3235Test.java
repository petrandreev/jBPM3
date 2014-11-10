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
package org.jbpm.jbpm3235;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.db.JobSession;
import org.jbpm.db.hibernate.HibernateHelper;
import org.jbpm.graph.def.EventCallback;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.job.ExecuteActionJob;
import org.jbpm.job.Job;
import org.jbpm.persistence.db.DbPersistenceServiceFactory;
import org.jbpm.svc.Services;

/**
 * JobSession.getFirstDueJob was not performing due to the queries having to sort the duedate field without an index. 
 * 
 * In order to see actual queries and query times, I used p6spy-2.0-SNAPSHOT. (see spy.properties in org/jbpm/jbpm3235/). 
 * @see <a href="https://jira.jboss.org/jira/browse/JBPM-3235">JBPM-3235</a>
 */
@SuppressWarnings({
  "rawtypes"
})
public class JBPM3235Test extends AbstractDbTestCase {

  private static final String PROCESS_NAME = "JBPM3235";
  private long[] monitoredJobIds = null;
 
  private String postgresDatabaseProperties = "org/jbpm/jbpm3235/hibernate.postgresql.properties";
  private String hsqlDatabaseProperties = "org/jbpm/jbpm3235/hibernate.hsql.properties";
  /** 
   * Feel free to make your own database properties file 
   *  and fill it appropriately: see the above file for necessary fields.
   *  
   * private String oracleDatabaseProperties = "org/jbpm/jbpm3235/hibernate.oracle.properties";
   */

  // Number of rows to add to the Job table
  private int numJobsToAdd = 10;

  // Make sure this test does not run in Hudson
  private boolean thisIsAnAutomatedTest = true;
  private boolean startWithCleanDatabase = false;
  private boolean cleanDatabaseAfterTest = false;
  
  private boolean usingHSQLDB = false;
  
  /**
   * Setup the test case.
   */
  protected void setUp() throws Exception {
    if( thisIsAnAutomatedTest ) return;
    
    DbPersistenceServiceFactory persistenceServiceFactory = (DbPersistenceServiceFactory) getJbpmConfiguration().getServiceFactory(Services.SERVICENAME_PERSISTENCE);
    String dialect = persistenceServiceFactory.getConfiguration().getProperty(Environment.DIALECT);
    
    setUpDatabase();
    if( startWithCleanDatabase && ! usingHSQLDB ) { 
      ensureCleanDatabase();
    }
    super.setUp();

    ProcessDefinition processDefinition = new ProcessDefinition(PROCESS_NAME);
    deployProcessDefinition(processDefinition);
  }

  private void setUpDatabase() {
    String propertiesResource = null;
    if (usingHSQLDB) { 
      propertiesResource = hsqlDatabaseProperties;
    }
    else { 
      propertiesResource = postgresDatabaseProperties;
    }
    
    // Ensure that the hibernate session uses the properties we configured (see propertiesResource above)
    Configuration configuration = HibernateHelper.createConfiguration(null, propertiesResource);
    
    DbPersistenceServiceFactory persistenceServiceFactory = (DbPersistenceServiceFactory) getJbpmConfiguration().getServiceFactory(Services.SERVICENAME_PERSISTENCE);
    persistenceServiceFactory.setConfiguration(configuration);
  }

  /**
   * Clean up after the test case.
   */
  protected void tearDown() throws Exception {
    if( thisIsAnAutomatedTest ) { return; }
    
    if( cleanDatabaseAfterTest && ! usingHSQLDB ) {
      ensureCleanDatabase();
      deleteProcessDefinitions(); 
    }
    
    closeJbpmContext();
    log.info("### END " + getName() + " ####################");
    EventCallback.clear();
    super.tearDown();
  }

  //
  // ACTUAL TESTS
  //

  private SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS yyyy-MM-dd");
  private boolean debug = false;
  public void testQueries() {
    if( thisIsAnAutomatedTest ) { return; } 

    addJobsToDb();

    JobSession jobSession = jbpmContext.getJobSession();

    if( debug ) { 
      Query query = session.createQuery("select job from org.jbpm.job.Job job");
      List jobList = query.list();
      assertTrue( "WTF!?!", jobList.size() > 0 );
    
      Iterator iter = jobList.iterator();
      while( iter.hasNext() ) { 
        Job job = (Job) iter.next();
        log.info("RET: [" + job.getLockOwner() + "/" + job.getRetries() + "]: " + sdf.format(job.getDueDate()) );
      }
    }
    
    // Query: JobSession.getFirstUnownedDueJob
    Date wakeUpDate = new Date();
    log.trace( "Wake Up Date: " + sdf.format(wakeUpDate) );
    Date nextDueDate = null;
    if( ! debug ) { 
      nextDueDate = jobSession.getNextUnownedDueJobDueDate(wakeUpDate);
    }
    else { 
      Query query = session.getNamedQuery("JobSession.getNextUnownedDueJobDueDate") .setTimestamp("wakeUpDate", wakeUpDate);
      List dateList = query.list();
      assertTrue( "No Dates!!?!", dateList.size() > 0 );
      assertTrue( "TOO MANY Dates!!?!", dateList.size() > 1 );
      
      nextDueDate = (Date) dateList.get(0);
      Iterator iter = dateList.iterator();
      while( iter.hasNext() ) { 
          Date ndt = (Date) iter.next();  
          log.info( "- ndt: " + sdf.format(ndt));
      }
    }
    assertTrue("Next due date was NOT found!!", nextDueDate != null);
    log.info( "Next Due Date: " + sdf.format(nextDueDate) );
  }

  protected void addJobsToDb() {
    ProcessInstance processInstance = jbpmContext.newProcessInstanceForUpdate(PROCESS_NAME);

    Job[] jobs = new Job[numJobsToAdd];
    Token rootToken = processInstance.getRootToken();
    int i = 0;
    for (; i < (numJobsToAdd/2); ++i) {
      jobs[i] = createJob(rootToken, i);
      jbpmContext.getJobSession().saveJob(jobs[i]);
    }
    for (; i < numJobsToAdd; ++i) {
      jobs[i] = createJob(rootToken, PROCESS_NAME, i);
      jbpmContext.getJobSession().saveJob(jobs[i]);
    }
    
    newTransaction();

    monitoredJobIds = new long[jobs.length];
    for (i = 0; i < jobs.length; ++i) {
      monitoredJobIds[i] = jobs[i].getId();
    }
  }

  /**
   *  select min(job.dueDate)
   *    from org.jbpm.job.Job job
   *   where job.dueDate <= :wakeUpDate
   *     and job.lockOwner is null
   *     and job.retries > 0
   *     and job.isSuspended = false
   */
  private Job createJob(Token token, int i) {
    return  createJob(token, null, i);
  }
  
  private Job createJob(Token token, String lockOwner, int i) {
    Job job = new ExecuteActionJob(token);

    // Add information used for querying
    job.setLockOwner(lockOwner);
    job.setRetries(i+1);
    job.setSuspended(false);

    // Fields used in query for sorting
    job.setDueDate(getOneHourAgoMinus(i));
    if( debug ) { 
      log.info("Job [" + lockOwner + "/" + job.getRetries() + "]: " + sdf.format(job.getDueDate()) );
    }

    // Other fields
    job.setException("Simulated Exception");
    job.setSuspended(false);
    
    return job;
  }
  
  
  
  private Date getOneHourAgoMinus(int seconds) {
    Calendar calendar = Calendar.getInstance();
    calendar.add(Calendar.HOUR, -1);
    // databases such as mysql do not have millisecond precision
    calendar.set(Calendar.MILLISECOND, 0);
    calendar.roll(Calendar.SECOND, -seconds);
    Date oneHourAgo = calendar.getTime();
    return oneHourAgo;
  }

}
