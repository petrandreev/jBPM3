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
package org.jbpm.jbpm2828;

import java.util.Arrays;
import java.util.List;

import org.hibernate.criterion.Projections;

import org.jbpm.JbpmConfiguration;
import org.jbpm.context.exe.ContextInstance;
import org.jbpm.context.exe.variableinstance.ByteArrayInstance;
import org.jbpm.context.exe.variableinstance.LongInstance;
import org.jbpm.context.exe.variableinstance.StringInstance;
import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;

/**
 * Test to verify if contextInstance.deleteVariable(name) does not cause
 * orphaned variable records.
 * 
 * @see <a href="https://jira.jboss.org/jira/browse/JBPM-2828">JBPM-2828</a>
 * @author Martin Putz
 */
public class JBPM2828Test extends AbstractDbTestCase {

  static final List listVariable = Arrays.asList(new String[] { "Bart", "Lisa",
    "Marge", "Barney", "Homer", "Maggie" });
  static final Long longVariable = new Long(10L);
  private static final String stringVariable = "jBPM";

  public static class CreateVariablesAction implements ActionHandler {
    private static final long serialVersionUID = 1L;

    public void execute(ExecutionContext executionContext) throws Exception {
      executionContext.setVariable("listVariable", listVariable);
      executionContext.setVariable("longVariable", longVariable);
      executionContext.setVariable("stringVariable", stringVariable);
    }
  }

  protected JbpmConfiguration getJbpmConfiguration() {
    if (jbpmConfiguration == null) {
      jbpmConfiguration = JbpmConfiguration.parseResource("org/jbpm/jbpm2828/jbpm.nolog.cfg.xml");
    }
    return jbpmConfiguration;
  }

  protected void tearDown() throws Exception {
    super.tearDown();
    jbpmConfiguration.close();
  }

  public void testDeleteVariable() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition name='jbpm2828'>"
      + "  <start-state name='start'>"
      + "    <transition to='state1'>"
      + "      <action name='createVariables' class='"
      + CreateVariablesAction.class.getName()
      + "'/>"
      + "    </transition>"
      + "  </start-state>"
      + "  <state name='state1'>"
      + "    <transition to='state2' name='go'>"
      + "      <script>"
      + "      contextInstance = executionContext.getContextInstance();"
      + "      contextInstance.deleteVariable(\"listVariable\");"
      + "      contextInstance.deleteVariable(\"longVariable\");"
      + "      contextInstance.deleteVariable(\"stringVariable\");"
      + "      </script>"
      + "    </transition>"
      + "  </state>"
      + "  <state name='state2'>"
      + "    <transition to='end' name='go'/>"
      + "  </state>"
      + "  <end-state name='end'/>"
      + "</process-definition>");
    deployProcessDefinition(processDefinition);

    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.signal();

    processInstance = saveAndReload(processInstance);
    ContextInstance contextInstance = processInstance.getContextInstance();
    assertEquals(listVariable, contextInstance.getVariable("listVariable"));
    assertEquals(longVariable, contextInstance.getVariable("longVariable"));
    assertEquals(stringVariable, contextInstance.getVariable("stringVariable"));
    assertEquals(1, getRecordCount(ByteArrayInstance.class));
    assertEquals(1, getRecordCount(LongInstance.class));
    assertEquals(1, getRecordCount(StringInstance.class));
    processInstance.signal();

    processInstance = saveAndReload(processInstance);
    contextInstance = processInstance.getContextInstance();
    assertNull(contextInstance.getVariable("listVariable"));
    assertNull(contextInstance.getVariable("longVariable"));
    assertNull(contextInstance.getVariable("stringVariable"));
    assertEquals(0, getRecordCount(ByteArrayInstance.class));
    assertEquals(0, getRecordCount(LongInstance.class));
    assertEquals(0, getRecordCount(StringInstance.class));
  }

  private int getRecordCount(Class entity) {
    Number count = (Number) session.createCriteria(entity)
      .setProjection(Projections.rowCount())
      .uniqueResult();
    return count.intValue();
  }
}
