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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.Node;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.def.Transition;

public class TokenDbTest extends AbstractDbTestCase {

  public void testTokenName() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <start-state />" +
      "</process-definition>" );
    graphSession.saveProcessDefinition(processDefinition);
    try
    {
      ProcessInstance processInstance = new ProcessInstance(processDefinition);
      processInstance.getRootToken().name = "roottoken";

      processInstance = saveAndReload(processInstance);

      assertEquals("roottoken", processInstance.getRootToken().getName());
    }
    finally
    {
      jbpmContext.getGraphSession().deleteProcessDefinition(processDefinition.getId());
    }
  }

  public void testTokenStartAndEndDate() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <start-state>" +
      "    <transition to='end' />" +
      "  </start-state>" +
      "  <end-state name='end'/>" +
      "</process-definition>" );
    graphSession.saveProcessDefinition(processDefinition);
    try
    {
      ProcessInstance processInstance = new ProcessInstance(processDefinition);
      processInstance.signal();

      processInstance = saveAndReload(processInstance);
      Token token = processInstance.getRootToken();
      
      assertNotNull(token.getStart());
      assertNotNull(token.getEnd());
    }
    finally
    {
      jbpmContext.getGraphSession().deleteProcessDefinition(processDefinition.getId());
    }
  }

  public void testTokenNode() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <start-state name='s' />" +
      "</process-definition>");
    graphSession.saveProcessDefinition(processDefinition);
    try
    {
      ProcessInstance processInstance = new ProcessInstance(processDefinition);
      processInstance = saveAndReload(processInstance);
      
      Node s = processInstance.getProcessDefinition().getStartState();
      
      assertSame(s , processInstance.getRootToken().getNode());
    }
    finally
    {
      jbpmContext.getGraphSession().deleteProcessDefinition(processDefinition.getId());
    }
  }

  public void testTokenProcessInstance() {
    ProcessDefinition processDefinition = new ProcessDefinition();
    graphSession.saveProcessDefinition(processDefinition);
    try
    {
      ProcessInstance processInstance = new ProcessInstance(processDefinition);
      processInstance = saveAndReload(processInstance);
      
      assertSame(processInstance , processInstance.getRootToken().getProcessInstance());
    }
    finally
    {
      jbpmContext.getGraphSession().deleteProcessDefinition(processDefinition.getId());
    }
  }

  public void testTokenParent() {
    ProcessDefinition processDefinition = new ProcessDefinition();
    graphSession.saveProcessDefinition(processDefinition);
    try
    {
      ProcessInstance processInstance = new ProcessInstance(processDefinition);
      new Token(processInstance.getRootToken(), "one");

      processInstance = saveAndReload(processInstance);
      Token rootToken = processInstance.getRootToken();
      Token childOne = rootToken.getChild("one");
      
      assertSame(rootToken , childOne.getParent());
    }
    finally
    {
      jbpmContext.getGraphSession().deleteProcessDefinition(processDefinition.getId());
    }
  }
  
  public void testTokenChildren() {
    ProcessDefinition processDefinition = new ProcessDefinition();
    graphSession.saveProcessDefinition(processDefinition);
    try
    {
      ProcessInstance processInstance = new ProcessInstance(processDefinition);
      new Token(processInstance.getRootToken(), "one");
      new Token(processInstance.getRootToken(), "two");
      new Token(processInstance.getRootToken(), "three");

      processInstance = saveAndReload(processInstance);
      Token rootToken = processInstance.getRootToken();
      Token childOne = rootToken.getChild("one");
      Token childTwo = rootToken.getChild("two");
      Token childThree = rootToken.getChild("three");
      
      assertEquals("one" , childOne.getName());
      assertEquals("two" , childTwo.getName());
      assertEquals("three" , childThree.getName());
    }
    finally
    {
      jbpmContext.getGraphSession().deleteProcessDefinition(processDefinition.getId());
    }
  }

  public void testAvailableTransitions() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
        "<process-definition name='conditionsprocess'>" +
        "  <start-state name='zero'>" +
        "    <transition to='one'   condition='#{a==5}' />" +
        "    <transition to='two'   condition='#{a&gt;7}' />" +
        "    <transition to='three' />" +
        "    <transition to='four'  condition='#{a&lt;7}' />" +
        "  </start-state>" +
        "  <state name='one' />" +
        "  <state name='two' />" +
        "  <state name='three' />" +
        "  <state name='four' />" +
        "</process-definition>"
      );
      jbpmContext.deployProcessDefinition(processDefinition);
      try
      {
        newTransaction();
        
        ProcessInstance processInstance = jbpmContext.newProcessInstance("conditionsprocess");
        processInstance.getContextInstance().setVariable("a", new Integer(5));
        processInstance = saveAndReload(processInstance);
        
        Set availableTransitions = processInstance.getRootToken().getAvailableTransitions();
        Set availableToNames = new HashSet();
        Iterator iter = availableTransitions.iterator();
        while (iter.hasNext()) {
          Transition transition = (Transition) iter.next();
          availableToNames.add(transition.getTo().getName());
        }
        
        Set expectedToNames = new HashSet();
        expectedToNames.add("one");
        expectedToNames.add("three");
        expectedToNames.add("four");

        assertEquals(expectedToNames, availableToNames);
      }
      finally
      {
        jbpmContext.getGraphSession().deleteProcessDefinition(processDefinition.getId());
      }
  }
}
