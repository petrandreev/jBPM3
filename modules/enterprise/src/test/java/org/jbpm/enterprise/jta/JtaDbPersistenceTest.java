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
package org.jbpm.enterprise.jta;

import junit.framework.Test;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.jboss.bpm.api.test.IntegrationTestSetup;
import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.command.Command;
import org.jbpm.command.CommandService;
import org.jbpm.command.impl.CommandServiceImpl;
import org.jbpm.enterprise.AbstractEnterpriseTestCase;

public class JtaDbPersistenceTest extends AbstractEnterpriseTestCase {

  private static final Log log = LogFactory.getLog(JtaDbPersistenceTest.class);

  public static Test suite() throws Exception {
    return new IntegrationTestSetup(JtaDbPersistenceTest.class, "enterprise-test.war");
  }

  protected CommandService createCommandService() throws Exception {
    return getName().contains("CMT") ? super.createCommandService() : new CommandServiceImpl(
        JbpmConfiguration.getInstance());
  }

  public void testCMTSuccess() throws Exception {
    playTransaction(false);
  }

  public void testCMTFailure() throws Exception {
    playTransaction(true);
  }

  public void testBMTSuccess() throws Exception {
    playTransaction(false);
  }

  public void testBMTFailure() throws Exception {
    playTransaction(true);
  }

  private void playTransaction(boolean fail) throws Exception {
    deployProcessDefinition("<process-definition name='jta'>"
        + "  <start-state name='start'>"
        + "    <transition to='midway' />"
        + "  </start-state>"
        + "  <state name='midway'>"
        + "    <transition to='end' />"
        + "  </state>"
        + "  <end-state name='end' />"
        + "</process-definition>");
    long processInstanceId = startProcessInstance("jta").getId();
    try {
      signal(processInstanceId, fail);
    }
    catch (RuntimeException e) {
      log.debug("signal failed on process instance #" + processInstanceId, e);
    }
    assertEquals(!fail, hasProcessInstanceEnded(processInstanceId));
  }

  private void signal(final long processInstanceId, final boolean fail) throws Exception {
    commandService.execute(new Command() {
      private static final long serialVersionUID = 1L;

      public Object execute(JbpmContext jbpmContext) throws Exception {
        jbpmContext.loadProcessInstance(processInstanceId).signal();
        if (fail) throw new HibernateException("simulated failure");
        return null;
      }
    });
  }

}
