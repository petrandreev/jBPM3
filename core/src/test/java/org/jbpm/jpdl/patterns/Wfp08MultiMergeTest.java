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
package org.jbpm.jpdl.patterns;

import org.jbpm.AbstractJbpmTestCase;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.Token;
import org.jbpm.graph.node.Merge;

/**
 * http://is.tm.tue.nl/research/patterns/download/swf/pat_8.swf
 */
public class Wfp08MultiMergeTest extends AbstractJbpmTestCase {

  private static ProcessDefinition multiMergeProcessDefinition = createMultiMergeProcessDefinition();

  public static ProcessDefinition createMultiMergeProcessDefinition() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <start-state name='start'>" +
      "    <transition to='a' />" +
      "  </start-state>" +
      "  <state name='a'>" +
      "    <transition to='multichoice' />" +
      "  </state>" +
      "  <fork name='multichoice'>" +
      "    <script>" +
      "      <variable name='transitionNames' access='write' />" +
      "      <expression>" +
      "        transitionNames = new ArrayList();" +
      "        if ( scenario == 1 ) {" +
      "          transitionNames.add( \"to b\" );" +
      "        } else if ( scenario == 2 ) {" +
      "          transitionNames.add( \"to c\" );" +
      "        } else if ( scenario >= 3 ) {" +
      "          transitionNames.add( \"to b\" );" +
      "          transitionNames.add( \"to c\" );" +
      "        }" +
      "      </expression>" +
      "    </script>" +
      "    <transition name='to b' to='b' />" +
      "    <transition name='to c' to='c' />" +
      "  </fork>" +
      "  <state name='b'>" +
      "    <transition to='multimerge' />" +
      "  </state>" +
      "  <state name='c'>" +
      "    <transition to='multimerge' />" +
      "  </state>" +
      "  <merge name='multimerge'>" +
      "    <transition to='d' />" +
      "  </merge>" +
      "  <state name='d' />" +
      "</process-definition>"
    );
          
    return processDefinition;
  }

  public void testMultiMergeScenario1() {
    ProcessDefinition pd = multiMergeProcessDefinition;
    Token root = Wfp06MultiChoiceTest.executeScenario(pd,1);
    Token tokenB = root.getChild("to b"); 
    tokenB.signal();
    assertSame( pd.getNode("d"), tokenB.getNode() );
  }
  
  public void testMultiMergeScenario2() {
    ProcessDefinition pd = multiMergeProcessDefinition;
    Token root = Wfp06MultiChoiceTest.executeScenario(pd,2);
    Token tokenC = root.getChild("to c"); 
    tokenC.signal();
    assertSame( pd.getNode("d"), tokenC.getNode() );
  }
  
  private static ProcessDefinition synchronizingMultiMergeProcessDefinition = createSynchronizingMultiMergeProcessDefinition();

  public static ProcessDefinition createSynchronizingMultiMergeProcessDefinition() {
    ProcessDefinition pd = createMultiMergeProcessDefinition();

    // get the multimerge handler
    Merge merge = (Merge) pd.getNode("multimerge");
    merge.setSynchronized( true );
    
    return pd;
  }

  public void testMultiMergeScenario3() {
    ProcessDefinition pd = synchronizingMultiMergeProcessDefinition;
    Token root = Wfp06MultiChoiceTest.executeScenario(pd,3);
    Token tokenB = root.getChild("to b"); 
    Token tokenC = root.getChild("to c");
    
    tokenB.signal();
    assertSame( pd.getNode("multimerge"), tokenB.getNode() );
    assertSame( pd.getNode("c"), tokenC.getNode() );
    
    tokenC.signal();
    assertSame( pd.getNode("d"), tokenB.getNode() );
    assertSame( pd.getNode("d"), tokenC.getNode() );
  }
  
  public void testMultiMergeScenario4() {
    ProcessDefinition pd = synchronizingMultiMergeProcessDefinition;
    Token root = Wfp06MultiChoiceTest.executeScenario(pd,4);
    Token tokenB = root.getChild("to b"); 
    Token tokenC = root.getChild("to c");
    
    tokenC.signal();
    assertSame( pd.getNode("b"), tokenB.getNode() );
    assertSame( pd.getNode("multimerge"), tokenC.getNode() );
    
    tokenB.signal();
    assertSame( pd.getNode("d"), tokenB.getNode() );
    assertSame( pd.getNode("d"), tokenC.getNode() );
  }

  public void testMultiMergeScenario5() {
    ProcessDefinition pd = multiMergeProcessDefinition;
    Token root = Wfp06MultiChoiceTest.executeScenario(pd,5);
    Token tokenB = root.getChild("to b"); 
    Token tokenC = root.getChild("to c");
    
    tokenB.signal();
    assertSame( pd.getNode("d"), tokenB.getNode() );
    assertSame( pd.getNode("c"), tokenC.getNode() );
    
    tokenC.signal();
    assertSame( pd.getNode("d"), tokenB.getNode() );
    assertSame( pd.getNode("d"), tokenC.getNode() );
  }

  public void testMultiMergeScenario6() {
    ProcessDefinition pd = multiMergeProcessDefinition;
    Token root = Wfp06MultiChoiceTest.executeScenario(pd,6);
    Token tokenB = root.getChild("to b"); 
    Token tokenC = root.getChild("to c");
    
    tokenC.signal();
    assertSame( pd.getNode("b"), tokenB.getNode() );
    assertSame( pd.getNode("d"), tokenC.getNode() );
    
    tokenB.signal();
    assertSame( pd.getNode("d"), tokenB.getNode() );
    assertSame( pd.getNode("d"), tokenC.getNode() );
  }
}
