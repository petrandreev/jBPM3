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
package org.jbpm.jpdl.xml;

import org.jbpm.AbstractJbpmTestCase;
import org.jbpm.graph.action.Script;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.scheduler.def.CancelTimerAction;
import org.jbpm.scheduler.def.CreateTimerAction;

public class TimerValidatingXmlTest extends AbstractJbpmTestCase {
	
  private static final String schemaNamespace = "xmlns=\"urn:jbpm.org:jpdl-3.2\"";
  
  public void testTimerCreateAction() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
        "<process-definition " + schemaNamespace +" name='testTimerCreateAction'>" +
      "  <node name='catch crooks'>" +
      "    <timer name='reminder' " +
      "           duedate='2 business hours' " +
      "           transition='time-out-transition' >" +
      "      <action class='the-remainder-action-class-name' />" +
      "    </timer>" +
      "  </node>" +
      "</process-definition>"
    );
    
    CreateTimerAction createTimerAction = 
        (CreateTimerAction) processDefinition
          .getNode("catch crooks")
          .getEvent("node-enter")
          .getActions()
          .get(0);
    
    assertEquals("reminder", createTimerAction.getTimerName());
    assertEquals("2 business hours", createTimerAction.getDueDate());
    assertEquals("time-out-transition", createTimerAction.getTransitionName());
    assertEquals("the-remainder-action-class-name", createTimerAction.getTimerAction().getActionDelegation().getClassName());
  }

  public void testTimerCreateActionRepeat() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition " + schemaNamespace +" name='testTimerCreateActionRepeat'>" +
      "  <node name='catch crooks'>" +
      "    <timer name='reminder' " +
      "           duedate='2 business hours' " +
      "           repeat='10 business minutes'>" +
      "      <action class='the-remainder-action-class-name' />" +
      "    </timer>" +
      "  </node>" +
      "</process-definition>"
    );
    
    CreateTimerAction createTimerAction = 
        (CreateTimerAction) processDefinition
          .getNode("catch crooks")
          .getEvent("node-enter")
          .getActions()
          .get(0);
    
    assertEquals("reminder", createTimerAction.getTimerName());
    assertEquals("2 business hours", createTimerAction.getDueDate());
    assertEquals("10 business minutes", createTimerAction.getRepeat());
    assertEquals("the-remainder-action-class-name", createTimerAction.getTimerAction().getActionDelegation().getClassName());
  }

  public void testTimerDefaultName() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition " + schemaNamespace +" name='testTimerDefaultName'>" +
      "  <node name='catch crooks'>" +
      "    <timer duedate='5 days and 10 minutes and 3 seconds'>" +
      "       <script/>" + 
      "    </timer>"+
      "  </node>" +
      "</process-definition>"
    );
    
    CreateTimerAction createTimerAction = 
        (CreateTimerAction) processDefinition
          .getNode("catch crooks")
          .getEvent("node-enter")
          .getActions()
          .get(0);
    
    assertEquals("catch crooks", createTimerAction.getTimerName());
  }

  public void testTimerCancelAction() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition " + schemaNamespace +" name='testTimerCancelAction'>" +
      "  <node name='catch crooks'>" +
      "    <timer duedate='5 minutes and 1 second'><script/></timer>" +
      "  </node>" +
      "</process-definition>"
    );
    
    CancelTimerAction cancelTimerAction = 
        (CancelTimerAction) processDefinition
          .getNode("catch crooks")
          .getEvent("node-leave")
          .getActions()
          .get(0);
    
    assertEquals("catch crooks", cancelTimerAction.getTimerName());
  }

  public void testTimerScript() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition " + schemaNamespace +" name='testTimerScript'>" +
      "  <node name='catch crooks'>" +
      "    <timer duedate='1 day'>" +
      "      <script />" +
      "    </timer>" +
      "  </node>" +
      "</process-definition>"
    );
    
    CreateTimerAction createTimerAction = 
        (CreateTimerAction) processDefinition
          .getNode("catch crooks")
          .getEvent("node-enter")
          .getActions()
          .get(0);
    
    assertEquals(Script.class, createTimerAction.getTimerAction().getClass());
  }

  public void testCreateTimerAction() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition " + schemaNamespace +" name='testCreateTimerAction'>" +
      "  <node name='catch crooks'>" +
      "    <event type='node-enter'>" +
      "      <create-timer name='reminder' " +
      "                    duedate='2 business hours' " +
      "                    transition='time-out-transition' >" +
      "        <action class='the-remainder-action-class-name' />" +
      "      </create-timer>" +
      "    </event>" +
      "  </node>" +
      "</process-definition>"
    );
    
    CreateTimerAction createTimerAction = 
        (CreateTimerAction) processDefinition
          .getNode("catch crooks")
          .getEvent("node-enter")
          .getActions()
          .get(0);
    
    assertEquals("reminder", createTimerAction.getTimerName());
    assertEquals("2 business hours", createTimerAction.getDueDate());
    assertEquals("time-out-transition", createTimerAction.getTransitionName());
    assertEquals("the-remainder-action-class-name", createTimerAction.getTimerAction().getActionDelegation().getClassName());
  }

  public void testCreateTimerActionRepeat() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition " + schemaNamespace +" name='testCreateTimerActionRepeat'>" +
      "  <node name='catch crooks'>" +
      "    <event type='node-enter'>" +
      "      <create-timer name='reminder' " +
      "                    duedate='2 business hours' " +
      "                    repeat='10 business minutes'>" +
      "        <action class='the-remainder-action-class-name' />" +
      "      </create-timer>" +
      "    </event>" +
      "  </node>" +
      "</process-definition>"
    );
    
    CreateTimerAction createTimerAction = 
        (CreateTimerAction) processDefinition
          .getNode("catch crooks")
          .getEvent("node-enter")
          .getActions()
          .get(0);
    
    assertEquals("reminder", createTimerAction.getTimerName());
    assertEquals("2 business hours", createTimerAction.getDueDate());
    assertEquals("10 business minutes", createTimerAction.getRepeat());
    assertEquals("the-remainder-action-class-name", createTimerAction.getTimerAction().getActionDelegation().getClassName());
  }

  public void testCancelTimerAction() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition " + schemaNamespace +" name='testCancelTimerAction'>" +
      "  <node name='catch crooks'>" +
      "    <event type='node-enter'>" +
      "      <cancel-timer name='reminder' />" +
      "    </event>" +
      "  </node>" +
      "</process-definition>"
    );
    
    CancelTimerAction cancelTimerAction = 
        (CancelTimerAction) processDefinition
          .getNode("catch crooks")
          .getEvent("node-enter")
          .getActions()
          .get(0);
    
    assertEquals("reminder", cancelTimerAction.getTimerName());
  }
  
  public void testELTimer() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition " + schemaNamespace +" name='testELTimer'>" +
      "  <node name='get old'>" +
      "    <timer duedate='#{dateOfBirth} + 65 years'>" +
      "      <script />" +
      "    </timer>" +
      "  </node>" +
      "</process-definition>"
    );
    
    CreateTimerAction createTimerAction = 
      (CreateTimerAction) processDefinition
        .getNode("get old")
        .getEvent("node-enter")
        .getActions()
        .get(0);
  
    assertEquals("#{dateOfBirth} + 65 years", createTimerAction.getDueDate());
    
  }
}
