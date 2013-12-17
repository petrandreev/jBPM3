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
package org.jbpm.jbpm2947;

import org.hibernate.criterion.Projections;

import org.jbpm.context.def.ContextDefinition;
import org.jbpm.context.exe.VariableInstance;
import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;

/**
 * Custom logging causes orphaned variables.
 * 
 * @see <a href="https://jira.jboss.org/browse/JBPM-2947">JBPM-2947</a>
 * @author Alejandro Guizar
 */
public class NoLoggingServiceTest extends AbstractDbTestCase {

  protected String getJbpmTestConfig() {
    return "org/jbpm/jbpm2947/no-log.cfg.xml";
  }

  protected void tearDown() throws Exception {
    super.tearDown();
    jbpmConfiguration.close();
  }

  public void testDeleteVariable() {
    assertNull("expected logging service to be absent",
      jbpmContext.getServices().getLoggingService());

    ProcessDefinition processDefinition = new ProcessDefinition("jbpm2947");
    processDefinition.addDefinition(new ContextDefinition());
    deployProcessDefinition(processDefinition);

    ProcessInstance processInstance = jbpmContext.newProcessInstance("jbpm2947");
    processInstance.getContextInstance().setVariable("var", "what's up, doc?");

    processInstance = saveAndReload(processInstance);
    processInstance.getContextInstance().deleteVariable("var");
    jbpmContext.save(processInstance);

    // verify the variable instance was deleted
    Number varCount = (Number) session.createCriteria(VariableInstance.class)
      .setProjection(Projections.rowCount())
      .uniqueResult();
    assertEquals(0, varCount.intValue());
  }
}
