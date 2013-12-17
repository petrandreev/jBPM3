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
import org.jbpm.graph.node.Join;

/**
 * http://is.tm.tue.nl/research/patterns/download/swf/pat_9.swf
 */
public class Wfp09DiscriminatorTest extends AbstractJbpmTestCase {

  private static ProcessDefinition discriminatorProcessDefinition = createDiscriminatorProcessDefinition();

  public static ProcessDefinition createDiscriminatorProcessDefinition() {
    ProcessDefinition pd = createSynchronizingDiscriminatorProcessDefinition();
    
    // configure the join as a discriminator
    Join join = (Join) pd.getNode("discriminator");
    join.setDiscriminator(true);
    
    return pd;
  }

  private static ProcessDefinition synchronizingdiscriminatorProcessDefinition = createSynchronizingDiscriminatorProcessDefinition();

  public static ProcessDefinition createSynchronizingDiscriminatorProcessDefinition() {
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
      "    <transition to='discriminator' />" +
      "  </state>" +
      "  <state name='c'>" +
      "    <transition to='discriminator' />" +
      "  </state>" +
      "  <join name='discriminator'>" +
      "    <transition to='d' />" +
      "  </join>" +
      "  <state name='d' />" +
      "</process-definition>"
    );
          
    return processDefinition;
  }

  public void testDiscriminatorScenario1() {
    ProcessDefinition pd = discriminatorProcessDefinition;
    Token root = Wfp06MultiChoiceTest.executeScenario(pd,1);
    Token tokenB = root.getChild("to b"); 

    tokenB.signal();
    assertSame( pd.getNode("d"), root.getNode() );
    assertSame( pd.getNode("discriminator"), tokenB.getNode() );
  }
  
  public void testDiscriminatorScenario2() {
    ProcessDefinition pd = discriminatorProcessDefinition;
    Token root = Wfp06MultiChoiceTest.executeScenario(pd,2);
    Token tokenC = root.getChild("to c"); 

    tokenC.signal();
    assertSame( pd.getNode("d"), root.getNode() );
    assertSame( pd.getNode("discriminator"), tokenC.getNode() );
  }
  
  public void testDiscriminatorScenario3() {
    ProcessDefinition pd = discriminatorProcessDefinition;
    Token root = Wfp06MultiChoiceTest.executeScenario(pd,3);
    Token tokenB = root.getChild("to b"); 
    Token tokenC = root.getChild("to c"); 

    tokenB.signal();
    assertSame( pd.getNode("d"), root.getNode() );
    assertSame( pd.getNode("discriminator"), tokenB.getNode() );
    assertSame( pd.getNode("c"), tokenC.getNode() );

    tokenC.signal();
    assertSame( pd.getNode("d"), root.getNode() );
    assertSame( pd.getNode("discriminator"), tokenB.getNode() );
    assertSame( pd.getNode("discriminator"), tokenC.getNode() );
  }
  
  public void testDiscriminatorScenario4() {
    ProcessDefinition pd = discriminatorProcessDefinition;
    Token root = Wfp06MultiChoiceTest.executeScenario(pd,4);
    Token tokenB = root.getChild("to b"); 
    Token tokenC = root.getChild("to c"); 

    tokenC.signal();
    assertSame( pd.getNode("d"), root.getNode() );
    assertSame( pd.getNode("b"), tokenB.getNode() );
    assertSame( pd.getNode("discriminator"), tokenC.getNode() );

    tokenB.signal();
    assertSame( pd.getNode("d"), root.getNode() );
    assertSame( pd.getNode("discriminator"), tokenB.getNode() );
    assertSame( pd.getNode("discriminator"), tokenC.getNode() );
  }

  public void testDiscriminatorScenario5() {
    ProcessDefinition pd = synchronizingdiscriminatorProcessDefinition;
    Token root = Wfp06MultiChoiceTest.executeScenario(pd,5);
    Token tokenB = root.getChild("to b"); 
    Token tokenC = root.getChild("to c"); 

    tokenB.signal();
    assertSame( pd.getNode("multichoice"), root.getNode() );
    assertSame( pd.getNode("discriminator"), tokenB.getNode() );
    assertSame( pd.getNode("c"), tokenC.getNode() );

    tokenC.signal();
    assertSame( pd.getNode("d"), root.getNode() );
    assertSame( pd.getNode("discriminator"), tokenB.getNode() );
    assertSame( pd.getNode("discriminator"), tokenC.getNode() );
  }

  public void testDiscriminatorScenario6() {
    ProcessDefinition pd = synchronizingdiscriminatorProcessDefinition;
    Token root = Wfp06MultiChoiceTest.executeScenario(pd,3);
    Token tokenB = root.getChild("to b"); 
    Token tokenC = root.getChild("to c"); 

    tokenC.signal();
    assertSame( pd.getNode("multichoice"), root.getNode() );
    assertSame( pd.getNode("b"), tokenB.getNode() );
    assertSame( pd.getNode("discriminator"), tokenC.getNode() );

    tokenB.signal();
    assertSame( pd.getNode("d"), root.getNode() );
    assertSame( pd.getNode("discriminator"), tokenB.getNode() );
    assertSame( pd.getNode("discriminator"), tokenC.getNode() );
  }
}
