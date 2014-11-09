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
package org.jbpm.jbpm3509;

import java.io.Serializable;

import org.jbpm.AbstractJbpmTestCase;
import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;

@SuppressWarnings({
  "rawtypes", "unchecked"
})
public class JBPM3509Test extends AbstractJbpmTestCase {

  public static class DataCarrier implements Serializable {
  
    private static final long serialVersionUID = 1L;
    
    private transient boolean theBoolean;
  
    public boolean getTheBoolean() {
      return theBoolean;
    }
  
    public void setTheBoolean(Boolean theBoolean) {
      if( theBoolean == null ) { 
        this.theBoolean = false;
      }
      else { 
        this.theBoolean = theBoolean.booleanValue();
      }
    }
  
    public String getTheBad() {
      return TestExpressionEvaluator.BAD_VALUE;
    }
  
  }

  private JbpmContext jbpmContext = null;
  private static JbpmConfiguration jbpmConfiguration = JbpmConfiguration.parseResource("org/jbpm/jbpm3509/jbpm.cfg.xml");
  
  public void setUp() throws Exception {
    super.setUp();
    jbpmContext = jbpmConfiguration.createJbpmContext();
  } 
  
  public void tearDown() throws Exception {
    jbpmContext.close();
    super.tearDown();
  } 
  
  public void testEvaluateBooleanResultExpression() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<?xml version='1.0'?>"
    + "<process-definition name='jbpm3509' xmlns='urn:jbpm.org:jpdl-3.2'>"
    + " <start-state>" 
    + "   <transition to='d' />" 
    + " </start-state>"
    + " <decision name='d' expression='#{dataCarrier.getTheBoolean}'>"
    + "   <transition name='true' to='a' />" 
    + "   <transition name='false' to='b' />"
    + " </decision>" 
    + " <state name='a' />" 
    + " <state name='b' />" 
    + "</process-definition>");
    
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    
    // Insert process variables
    DataCarrier dataCarrier = new DataCarrier();
    dataCarrier.setTheBoolean(new Boolean(false));
    processInstance.getContextInstance().setVariable("dataCarrier", dataCarrier);

    // start process
    processInstance.signal();

    // validate test
    assertEquals("Chose wrong path!", "b", processInstance.getRootToken().getNode().getName());
  }

  public void testEvaluateBADResultExpression() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>"
    + " <start-state>" 
    + "   <transition to='d' />" 
    + " </start-state>"
    + " <decision name='d' expression='#{dataCarrier.getTheBad}'>"
    + "   <transition name='true' to='a' />" 
    + "   <transition name='false' to='b' />"
    + " </decision>" 
    + " <state name='a' />" 
    + " <state name='b' />" 
    + "</process-definition>");
    
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    
    // Insert process variables
    DataCarrier dataCarrier = new DataCarrier();
    dataCarrier.setTheBoolean(new Boolean(false));
    processInstance.getContextInstance().setVariable("dataCarrier", dataCarrier);
  
    // start process
    try { 
      processInstance.signal();
    } catch( JbpmException je ) { 
      assertEquals("Condition returned an object of unknown type when determining transition.", je.getMessage());
    }
  
  }

}
