/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
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
package org.jbpm.jbpm2958;

import java.util.Calendar;
import java.util.Date;

import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.db.JbpmSchema;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.job.Timer;
import org.jbpm.persistence.db.DbPersistenceServiceFactory;
import org.jbpm.svc.Services;

/**
 * Timer should employ the very calendar that computed the first due date to calculate repeat
 * dates.
 * 
 * @see <a href="https://jira.jboss.org/browse/JBPM-2958">JBPM-2958</a>
 * @author Alejandro Guizar
 */
public class TimerCalendarTest extends AbstractDbTestCase {

  protected JbpmConfiguration getJbpmConfiguration() {
    if (jbpmConfiguration == null) {
      jbpmConfiguration = JbpmConfiguration.parseResource("org/jbpm/jbpm2958/saturday-jbpm.cfg.xml");

      JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();
      try {
        DbPersistenceServiceFactory persistenceServiceFactory = (DbPersistenceServiceFactory) jbpmContext.getServiceFactory(Services.SERVICENAME_PERSISTENCE);
        JbpmSchema jbpmSchema = new JbpmSchema(persistenceServiceFactory.getConfiguration());
        jbpmSchema.updateTable("JBPM_JOB");
      }
      finally {
        jbpmContext.close();
      }
    }
    return jbpmConfiguration;
  }

  protected void setUp() throws Exception {
    super.setUp();

    ProcessDefinition processDefinition = ProcessDefinition.parseXmlResource("org/jbpm/jbpm2958/processdefinition.xml");
    deployProcessDefinition(processDefinition);
  }

  protected void tearDown() throws Exception {
    super.tearDown();
    jbpmConfiguration.close();
  }

  public void testTimerCalendarResource() {
    // baseDate is a Friday, one hour before close of business
    Calendar calendar = Calendar.getInstance();
    calendar.set(2010, Calendar.OCTOBER, 8, 16, 0, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    Date baseDate = calendar.getTime();

    // processInstance schedules a timer due on baseDate
    // in jbpmConfiguration, Saturday is a work day
    ProcessInstance processInstance = jbpmContext.newProcessInstanceForUpdate("jbpm2958");
    processInstance.getContextInstance().setVariable("baseDate", baseDate);
    processInstance.signal();

    closeJbpmContext();
    try {
      // in standardConfiguration, Saturday is NOT a work day
      JbpmConfiguration standardConfiguration = JbpmConfiguration.parseResource("org/jbpm/jbpm2958/jbpm.cfg.xml");
      JbpmContext standardContext = standardConfiguration.createJbpmContext();
      try {
        Timer timer = (Timer) standardContext.getSession()
          .createCriteria(Timer.class)
          .uniqueResult();
        timer.execute(standardContext);
      }
      catch (Exception e) {
        standardContext.setRollbackOnly();
        fail(e.getMessage());
      }
      finally {
        standardContext.close();
        standardConfiguration.close();
      }
    }
    finally {
      createJbpmContext();
    }

    calendar.add(Calendar.HOUR, 2);
    // there are 16 hours between 17:00 and 9:00
    calendar.add(Calendar.HOUR, 16);
    // repeatDate is a Saturday
    Date repeatDate = calendar.getTime();

    Timer timer = (Timer) session.createCriteria(Timer.class).uniqueResult();
    assertEquals(repeatDate, new Date(timer.getDueDate().getTime()));

    processInstance = jbpmContext.loadProcessInstanceForUpdate(processInstance.getId());
    processInstance.signal();
    assert processInstance.hasEnded() : "expected " + processInstance + " to have ended";
  }
}
