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
package org.jbpm.graph.exe;

import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.Action;
import org.jbpm.graph.def.Event;
import org.jbpm.graph.def.ProcessDefinition;

public class RuntimeActionDbTest extends AbstractDbTestCase {

  public void testRuntimeActionEvent() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition name='"
      + getName()
      + "'>"
      + "  <event type='process-start' />"
      + "  <action name='gotocheetahs' class='com.secret.LetsDoItSneeky'/>"
      + "</process-definition>");
    deployProcessDefinition(processDefinition);

    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    Event event = processInstance.getProcessDefinition().getEvent("process-start");
    Action action = processInstance.getProcessDefinition().getAction("gotocheetahs");
    processInstance.addRuntimeAction(new RuntimeAction(event, action));

    processInstance = saveAndReload(processInstance);
    RuntimeAction runtimeAction = (RuntimeAction) processInstance.getRuntimeActions().get(0);
    event = processInstance.getProcessDefinition().getEvent("process-start");
    assertEquals(event.getGraphElement(), runtimeAction.getGraphElement());
    assertEquals(event.getEventType(), runtimeAction.getEventType());
  }

  public void testRuntimeActionAction() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition name='"
      + getName()
      + "'>"
      + "  <event type='process-start' />"
      + "  <action name='gotocheetahs' class='com.secret.LetsDoItSneeky'/>"
      + "</process-definition>");
    deployProcessDefinition(processDefinition);

    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    Event event = processInstance.getProcessDefinition().getEvent("process-start");
    Action action = processInstance.getProcessDefinition().getAction("gotocheetahs");
    processInstance.addRuntimeAction(new RuntimeAction(event, action));

    processInstance = saveAndReload(processInstance);
    RuntimeAction runtimeAction = (RuntimeAction) processInstance.getRuntimeActions().get(0);
    action = processInstance.getProcessDefinition().getAction("gotocheetahs");
    assertSame(action, runtimeAction.getAction());
  }

  public void testRuntimeActionOnNonExistingEvent() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition name='"
      + getName()
      + "'>"
      + "  <action name='gotocheetahs' class='com.secret.LetsDoItSneeky'/>"
      + "</process-definition>");
    deployProcessDefinition(processDefinition);

    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    Action action = processInstance.getProcessDefinition().getAction("gotocheetahs");
    processInstance.addRuntimeAction(new RuntimeAction(processDefinition, "process-start",
      action));

    processInstance = saveAndReload(processInstance);
    RuntimeAction runtimeAction = (RuntimeAction) processInstance.getRuntimeActions().get(0);
    action = processInstance.getProcessDefinition().getAction("gotocheetahs");
    assertSame(action, runtimeAction.getAction());
  }

}
