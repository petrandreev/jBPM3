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
package org.jbpm.graph.node;

import org.hibernate.LockMode;

import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.db.JbpmSchema;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.persistence.db.DbPersistenceServiceFactory;
import org.jbpm.svc.Services;

/**
 * Verifies the join node can be persisted correctly.
 * 
 * @author Alejandro Guizar
 */
public class JoinDbTest extends AbstractDbTestCase {

  protected JbpmConfiguration getJbpmConfiguration() {
    if (jbpmConfiguration == null) {
      jbpmConfiguration = JbpmConfiguration.parseResource("org/jbpm/graph/node/jbpm.cfg.xml");

      JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();
      try {
        DbPersistenceServiceFactory persistenceServiceFactory = (DbPersistenceServiceFactory)
          jbpmContext.getServiceFactory(Services.SERVICENAME_PERSISTENCE);
        JbpmSchema jbpmSchema = new JbpmSchema(persistenceServiceFactory.getConfiguration());
        jbpmSchema.updateTable("JBPM_NODE");
      }
      finally {
        jbpmContext.close();
      }
    }
    return jbpmConfiguration;
  }

  protected void tearDown() throws Exception {
    super.tearDown();
    jbpmConfiguration.close();
  }

  public void testParentLockMode() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition name='lock mode'>"
      + "  <join name='read' lock='READ' />"
      + "  <join name='nowait' lock='UPGRADE_NOWAIT' />"
      + "  <join name='upgrade' lock='pessimistic' />"
      + "</process-definition>");
    deployProcessDefinition(processDefinition);

    processDefinition = graphSession.findLatestProcessDefinition("lock mode");
    Join join = (Join) processDefinition.getNode("read");
    assertEquals(LockMode.READ.toString(), join.getParentLockMode());
    join = (Join) processDefinition.getNode("nowait");
    assertEquals(LockMode.UPGRADE_NOWAIT.toString(), join.getParentLockMode());
    join = (Join) processDefinition.getNode("upgrade");
    assertEquals(LockMode.UPGRADE.toString(), join.getParentLockMode());
  }
}
