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
package org.jbpm.soa2010;

import java.util.Iterator;
import java.util.List;

import org.jbpm.JbpmException;
import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.jpdl.JpdlException;
import org.jbpm.jpdl.xml.Problem;

/**
 * SOA-2010: Conditional transition cannot be considered as a default one. 
 * 
 * Solution was
 * - Parser restricts conditional transitions to only those transitions leaving decisions. 
 * 
 * @see <a href="https://jira.jboss.org/browse/SOA-2010">SOA-2010</a>
 * @author Marco Rietveld
 */
@SuppressWarnings({
  "rawtypes", "unchecked"
})
public class SOA2010Test extends AbstractDbTestCase {
  
  protected void setUp() throws Exception {
    super.setUp();
  }

  // 
  // The following tests parse different jpdl docs to test for different situations
  // 
 
  /**
   * Simple flow in which there are: 
   * - normal transitions between a couple different types (just state, node, task-node)
   * - various conditional transitions from a decision node to an end node
   */
  public void testResolveTransitionDestinationGoodSimple() {
    try{
      ProcessDefinition.parseXmlResource("org/jbpm/soa2010/procdef_parse_good_simple.xml");
    }
    catch( Exception e) {
      fail("unexpected exception thrown: " + getExceptionClassName(e) + ": " + e.getMessage());
    }
  }
 
  private String getExceptionClassName(Exception e) {
    String className = e.getClass().getName();
    return className.substring(className.lastIndexOf('.'));
  }
  
  /**
   * Complexer flow in which there are: 
   * - normal transitions between a couple different types (just state, node, task-node)
   * - various conditional transitions from a decision node to an end node
   */
  public void testResolveTransitionDestinationGoodComplex() {
    try{
      ProcessDefinition.parseXmlResource("org/jbpm/soa2010/procdef_parse_good_complex.xml");
    }
    catch( Exception e) {
      fail("unexpected exception thrown: " + getExceptionClassName(e) + ": " + e.getMessage());
    }
  }
  
  public void testResolveTransitionDestinationBadSimple() {
    try {
      ProcessDefinition.parseXmlResource("org/jbpm/soa2010/procdef_parse_bad_simple.xml");
    }
    catch( JpdlException je) {
      checkJpdlException(je, 3);
    }
    catch( Exception e) {
      fail("unexpected exception thrown: " + getExceptionClassName(e) + ": " + e.getMessage());
    }
  }
  
  public void testResolveTransitionDestinationBadComplex() {
    try {
      ProcessDefinition.parseXmlResource("org/jbpm/soa2010/procdef_parse_bad_complex.xml");
    }
    catch( JpdlException je) {
      // 7 types x (7 + 2) connections + 1 start x 7 conn + 1 x 3 conn = 73
      checkJpdlException(je, 73);
    }
    catch( Exception e) {
      fail("unexpected exception thrown: " + getExceptionClassName(e) + ": " + e.getMessage());
    }
  }


  /**
   * Check that the given {@link JpdlException} only contains the given number of problems
   *  and that all problems
   */
  private void checkJpdlException(JpdlException jpdlException, int expectedNumCondTransProblems ) {
      List problems = jpdlException.getProblems();
      int conditionOnTransitionProblems = 0;
      Iterator iter = problems.iterator();
      while( iter.hasNext() ) {
        Problem problem = (Problem) iter.next();
        String problemMessage = problem.getDescription();
        if( problemMessage.indexOf("conditions on transitions only usable leaving decisions") > -1) {
          ++conditionOnTransitionProblems;
        }
        else {
          fail("problem that was not conditions/transitions found: " + problemMessage );
        }
      }
      assertTrue(expectedNumCondTransProblems + " problems expected, not " + conditionOnTransitionProblems, conditionOnTransitionProblems == expectedNumCondTransProblems);
   
  }
  
  //  
  // The following tests illustrated the behaviour of Transition.take() on non-Decision nodes 
  //  **BEFORE SOA-2010 was implemented!**
  // 
  // These tests will currently fail (because the process will no longer be accepted), 
  //  but have been left in as a form of documentation. 
  //
  
  /**
   * Illustrate the following:
   * - node with no unconditional transitions
   * - transition with a condition that evaluates to false, as first transition (in jpdl doc) 
   * - other conditional transitions also present from node
   * 
   * will throw an exception because the first/default transition is conditional evaluating to false. 
   */
  public void ignoreTestNodeWithFalseDefaultConditionalTransition() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlResource("org/jbpm/soa2010/procdef_cond_false.xml");
    deployProcessDefinition(processDefinition);
    
    ProcessInstance processInstance = jbpmContext.newProcessInstance("soa2010cf");
    boolean exceptionThrown = false;
    try {
      processInstance.signal();
    }
    catch(JbpmException je) {
      assertTrue("expected exception on condition", je.getMessage().indexOf("guarding Transition(to false) not met") > -1);
      exceptionThrown = true;
    }
    assertTrue("expected exception on condition", exceptionThrown);
  }
  
  /**
   * Illustrate the following:
   * - node with no unconditional transitions
   * - transition with a condition that evaluates to true, as first transition (in jpdl doc) 
   * - other conditional transitions also present from node
   * 
   * will succeed, because first/default is conditional that eval's to true. 
   * 
   * (otherwise EXACTLY the same jpdl/graph as previous test)
   */
  public void ignoreTestNodeWithTrueDefaultConditionalTransition() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlResource("org/jbpm/soa2010/procdef_cond_true.xml");
    deployProcessDefinition(processDefinition);
    
    ProcessInstance processInstance = jbpmContext.newProcessInstance("soa2010ct");
    try {
      processInstance.signal();
    }
    catch(JbpmException je) {
      fail("Did not expect exception: " + je.getMessage() );
    }
    assertEquals("true-condition-end", processInstance.getRootToken().getNode().getName());

  }
  
}
