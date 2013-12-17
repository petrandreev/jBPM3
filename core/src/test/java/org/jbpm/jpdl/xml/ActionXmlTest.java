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

import java.io.StringReader;
import java.util.List;

import org.dom4j.Element;
import org.jbpm.graph.action.Script;
import org.jbpm.graph.def.Action;
import org.jbpm.graph.def.Event;
import org.jbpm.graph.def.Node;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.def.Transition;
import org.jbpm.graph.node.State;
import org.jbpm.instantiation.Delegation;

public class ActionXmlTest extends AbstractXmlTestCase {
  
  public void testReadProcessDefinitionAction() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString( 
      "<process-definition>" +
      "  <event type='node-enter'>" +
      "    <action class='one'/>" +
      "    <action class='two'/>" +
      "    <action class='three'/>" +
      "  </event>" +
      "</process-definition>"
    );
    Event event = processDefinition.getEvent("node-enter");
    assertEquals(3, event.getActions().size());
    assertEquals("one", ((Action)event.getActions().get(0)).getActionDelegation().getClassName());
    assertEquals("two", ((Action)event.getActions().get(1)).getActionDelegation().getClassName());
    assertEquals("three", ((Action)event.getActions().get(2)).getActionDelegation().getClassName());
  }

  public void testWriteProcessDefinitionAction() throws Exception {
    ProcessDefinition processDefinition = new ProcessDefinition();
    Event event = new Event("node-enter");
    processDefinition.addEvent(event);
    event.addAction(new Action(new Delegation("one")));
    event.addAction(new Action(new Delegation("two")));
    event.addAction(new Action(new Delegation("three")));
    
    Element eventElement = toXmlAndParse( processDefinition, "/process-definition/event" );
    
    List actionElements = eventElement.elements("action");

    assertEquals(3, actionElements.size());
    assertEquals("one", ((Element)actionElements.get(0)).attributeValue("class"));
    assertEquals("two", ((Element)actionElements.get(1)).attributeValue("class"));
    assertEquals("three", ((Element)actionElements.get(2)).attributeValue("class"));
  }

  public void testReadActionConfigType() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString( 
      "<process-definition>" +
      "  <action name='burps' class='org.foo.Burps' config-type='bean' />" +
      "</process-definition>" 
    );
  
    assertEquals("bean", processDefinition.getAction("burps").getActionDelegation().getConfigType() );
  }

  public void testWriteActionConfigType() throws Exception {
    ProcessDefinition processDefinition = new ProcessDefinition();
    Delegation actionDelegate = new Delegation("one");
    actionDelegate.setConfigType("bean");
    Action action = new Action(actionDelegate);
    action.setName("a");
    processDefinition.addAction(action);
    Element actionElement = toXmlAndParse( processDefinition, "/process-definition/action" );
    assertEquals("bean", actionElement.attributeValue("config-type"));
  }

  public void testReadActionXmlConfiguration() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString( 
      "<process-definition>" +
      "  <action name='burps' class='org.foo.Burps' config-type='bean'>" +
      "    <id>63</id>" +
      "    <greeting>aloha</greeting>" +
      "  </action>" +
      "</process-definition>" 
    );
  
    Action action = processDefinition.getAction("burps");
    Delegation instantiatableDelegate = action.getActionDelegation(); 
    assertTrue(instantiatableDelegate.getConfiguration().indexOf("<id>63</id>")!=-1);
    assertTrue(instantiatableDelegate.getConfiguration().indexOf("<greeting>aloha</greeting>")!=-1 );
  }

  public void testWriteActionXmlConfiguration() throws Exception {
    ProcessDefinition processDefinition = new ProcessDefinition();
    Delegation actionDelegate = new Delegation("one");
    actionDelegate.setConfiguration("<id>63</id><greeting>aloha</greeting>");
    Action action = new Action(actionDelegate);
    action.setName("a");
    processDefinition.addAction(action);
    Element actionElement = toXmlAndParse( processDefinition, "/process-definition/action" );
    assertEquals("63", actionElement.elementTextTrim("id"));
    assertEquals("aloha", actionElement.elementTextTrim("greeting"));
  }

  public void testReadActionTextConfiguration() {
	    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString( 
	      "<process-definition>" +
	      "  <action name='burps' class='org.foo.Burps' config-type='constructor'>" +
	      "    a piece of configuration text" +
	      "  </action>" +
	      "</process-definition>" 
	    );
	  
	    Action action = processDefinition.getAction("burps");
	    Delegation instantiatableDelegate = action.getActionDelegation(); 
	    assertTrue(instantiatableDelegate.getConfiguration().indexOf("a piece of configuration text")!=-1);
	  }

  public void testReadScriptTextConfiguration() {
	    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString( 
	      "<process-definition>" +
	      "  <event type='node-enter'>" +
	      "    <script class='one'>" +
	      "      // System.out.println(\"blabla\");" +
	      "    </script>" +
	      "  </event>" +
	      "</process-definition>"
	    );
	    Event event = processDefinition.getEvent("node-enter");
	    Script script = (Script)event.getActions().get(0);
	    assertEquals("// System.out.println(\"blabla\");", script.getExpression().trim());
	  }

  public void testWriteActionTextConfiguration() throws Exception {
    ProcessDefinition processDefinition = new ProcessDefinition();
    Delegation actionDelegate = new Delegation("one");
    actionDelegate.setConfiguration("a piece of configuration text");
    actionDelegate.setConfigType("constructor");
    Action action = new Action(actionDelegate);
    action.setName("a");
    processDefinition.addAction(action);
    Element actionElement = toXmlAndParse( processDefinition, "/process-definition/action" );
    assertEquals("a piece of configuration text", actionElement.getTextTrim());
  }

  public void testReadActionAcceptPropagatedEventsDefault() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString( 
      "<process-definition>" +
      "  <action name='burps' class='org.foo.Burps' />" +
      "</process-definition>" 
    );
  
    Action action = processDefinition.getAction("burps");
    assertTrue(action.acceptsPropagatedEvents());
  }

  public void testReadActionAcceptPropagatedEventsTrue() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString( 
      "<process-definition>" +
      "  <action name='burps' class='org.foo.Burps' accept-propagated-events='true' />" +
      "</process-definition>" 
    );
  
    Action action = processDefinition.getAction("burps");
    assertTrue(action.acceptsPropagatedEvents());
  }

  public void testReadActionAcceptPropagatedEventsFalse() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString( 
      "<process-definition>" +
      "  <action name='burps' class='org.foo.Burps' accept-propagated-events='false' />" +
      "</process-definition>" 
    );
  
    Action action = processDefinition.getAction("burps");
    assertFalse(action.acceptsPropagatedEvents());
  }

  public void testWriteActionAcceptPropagatedEventsDefault() throws Exception {
    ProcessDefinition processDefinition = new ProcessDefinition();
    Delegation actionDelegate = new Delegation("one");
    Action action = new Action(actionDelegate);
    action.setName("a");
    processDefinition.addAction(action);
    Element actionElement = toXmlAndParse( processDefinition, "/process-definition/action" );
    assertNull(actionElement.attribute("accept-propagated-events"));
  }

  public void testWriteActionAcceptPropagatedEventsTrue() throws Exception {
    ProcessDefinition processDefinition = new ProcessDefinition();
    Delegation actionDelegate = new Delegation("one");
    Action action = new Action(actionDelegate);
    action.setName("a");
    action.setPropagationAllowed(true);
    processDefinition.addAction(action);
    Element actionElement = toXmlAndParse( processDefinition, "/process-definition/action" );
    assertNull(actionElement.attribute("accept-propagated-events"));
  }

  public void testWriteActionAcceptPropagatedEventsFalse() throws Exception {
    ProcessDefinition processDefinition = new ProcessDefinition();
    Delegation actionDelegate = new Delegation("one");
    Action action = new Action(actionDelegate);
    action.setName("a");
    action.setPropagationAllowed(false);
    processDefinition.addAction(action);
    Element actionElement = toXmlAndParse( processDefinition, "/process-definition/action" );
    assertEquals("false", actionElement.attributeValue("accept-propagated-events"));
  }

  public void testReadNodeActionName() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString( 
      "<process-definition>" +
      "  <node name='a'>" +
      "    <event type='node-enter'>" +
      "      <action name='burps' class='org.foo.Burps'/>" +
      "    </event>" +
      "  </node>" +
      "</process-definition>"
    );
    Action burps = (Action)processDefinition.getNode("a").getEvent("node-enter").getActions().get(0);
    assertEquals("burps", burps.getName());
  }

  public void testWriteNodeActionName() throws Exception {
    ProcessDefinition processDefinition = new ProcessDefinition();
    Node node = processDefinition.addNode( new Node() );
    Delegation instantiatableDelegate = new Delegation();
    instantiatableDelegate.setClassName("com.foo.Fighting");
    node.setAction(new Action(instantiatableDelegate));
    
    Element actionElement = AbstractXmlTestCase.toXmlAndParse( processDefinition, "/process-definition/node/action" );
    assertNotNull(actionElement);
    assertEquals("action", actionElement.getName());
    assertEquals("com.foo.Fighting", actionElement.attributeValue("class"));
  }

  public void testReadNodeEnterAction() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString( 
      "<process-definition>" +
      "  <node name='a'>" +
      "    <event type='node-enter'>" +
      "      <action class='org.foo.Burps'/>" +
      "    </event>" +
      "  </node>" +
      "</process-definition>"
    );
    assertEquals("org.foo.Burps", ((Action)processDefinition.getNode("a").getEvent("node-enter").getActions().get(0)).getActionDelegation().getClassName());
  }

  public void testWriteNodeEnterAction() throws Exception {
    ProcessDefinition processDefinition = new ProcessDefinition();
    Node node = processDefinition.addNode( new Node() );
    Delegation instantiatableDelegate = new Delegation();
    instantiatableDelegate.setClassName("com.foo.Fighting");
    node.addEvent(new Event("node-enter")).addAction(new Action(instantiatableDelegate));
    Element element = AbstractXmlTestCase.toXmlAndParse( processDefinition, "/process-definition/node[1]/event[1]" );
    
    assertNotNull(element);
    assertEquals("event", element.getName());
    assertEquals("node-enter", element.attributeValue("type"));
    assertEquals(1, element.elements("action").size());

    element = element.element("action");
    assertNotNull(element);
    assertEquals("action", element.getName());
    assertEquals("com.foo.Fighting", element.attributeValue("class"));
  }

  public void testParseAndWriteOfNamedEventActions() throws Exception {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString( 
      "<process-definition>" +
      "  <node name='a'>" +
      "    <event type='node-enter'>" +
      "      <action name='burps' class='org.foo.Burps'/>" +
      "    </event>" +
      "  </node>" +
      "</process-definition>"
    );
    Action burps = (Action)processDefinition.getNode("a").getEvent("node-enter").getActions().get(0);
    assertSame(burps, processDefinition.getAction("burps"));
    Element processDefinitionElement = toXmlAndParse( processDefinition, "/process-definition" );
    assertEquals(0, processDefinitionElement.elements("action").size());
  }

  public void testParseStateAction() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString( 
      "<process-definition>" +
      "  <state name='a'>" +
      "    <event type='node-enter'>" +
      "      <action class='org.foo.Burps' config-type='constructor-text'>" +
      "        this text should be passed in the constructor" +
      "      </action>" +
      "    </event>" +
      "  </state>" +
      "</process-definition>"
    );
    
    Node node = processDefinition.getNode("a");
    Event event = node.getEvent("node-enter");
    Action action = (Action) event.getActions().iterator().next();
    Delegation instantiatableDelegate = action.getActionDelegation(); 
    assertEquals("org.foo.Burps", instantiatableDelegate.getClassName());
    assertEquals("constructor-text", instantiatableDelegate.getConfigType());
    assertTrue(instantiatableDelegate.getConfiguration().indexOf("this text should be passed in the constructor")!=-1 );
  }

  public void testParseTransitionAction() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString( 
      "<process-definition>" +
      "  <state name='a'>" +
      "    <transition to='b'>" +
      "      <action class='org.foo.Burps'/>" +
      "    </transition>" +
      "  </state>" +
      "  <state name='b' />" +
      "</process-definition>"
    );
    
    Node node = processDefinition.getNode("a");
    assertEquals( 1, node.getLeavingTransitionsMap().size() );
    Transition transition = node.getDefaultLeavingTransition();
    Event event = transition.getEvent(Event.EVENTTYPE_TRANSITION);
    Action action = (Action) event.getActions().iterator().next();
    Delegation instantiatableDelegate = action.getActionDelegation(); 
    assertEquals("org.foo.Burps", instantiatableDelegate.getClassName());
  }

  public void testParseReferencedAction() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString( 
      "<process-definition>" +
      "  <node name='a'>" +
      "    <transition to='b'>" +
      "      <action ref-name='scratch'/>" +
      "    </transition>" +
      "  </node>" +
      "  <node name='b' />" +
      "  <action name='scratch' class='com.itch.Scratch' />" +
      "</process-definition>"
    );
    
    Node node = processDefinition.getNode("a");
    Transition transition = node.getDefaultLeavingTransition();
    Event event = transition.getEvent(Event.EVENTTYPE_TRANSITION);
    Action transitionAction = (Action) event.getActions().iterator().next();
    
    Action processAction = processDefinition.getAction("scratch");
    assertEquals("scratch", processAction.getName());
    assertSame(processAction, transitionAction.getReferencedAction());
    
    Delegation instantiatableDelegate = processAction.getActionDelegation(); 
    assertEquals("com.itch.Scratch", instantiatableDelegate.getClassName());
  }
  
  public void testParseActionWithoutClass() {
    StringReader stringReader = new StringReader(
      "<process-definition>" +
      "  <node>" +
      "    <event type='node-enter'>" +
      "      <action />" +
      "    </event>" +
      "  </node>" +
      "</process-definition>");
    JpdlXmlReader jpdlReader = new JpdlXmlReader(stringReader);
    jpdlReader.readProcessDefinition();
    assertTrue(Problem.containsProblemsOfLevel(jpdlReader.problems, Problem.LEVEL_WARNING));
  }

  public void testParseActionWithInvalidReference() {
    StringReader stringReader = new StringReader(
      "<process-definition>" +
      "  <node>" +
      "    <event type='node-enter'>" +
      "      <action ref-name='non-existing-action-name'/>" +
      "    </event>" +
      "  </node>" +
      "</process-definition>");
    JpdlXmlReader jpdlReader = new JpdlXmlReader(stringReader);
    jpdlReader.readProcessDefinition();
    assertTrue(Problem.containsProblemsOfLevel(jpdlReader.problems, Problem.LEVEL_WARNING));
  }

  public void testWriteTransitionAction() throws Exception {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <state name='a'>" +
      "    <transition to='b' />" +
      "  </state>" +
      "  <state name='b' />" +
      "</process-definition>"
    );
    Transition transition = processDefinition.getNode("a").getDefaultLeavingTransition();
    
    Delegation instantiatableDelegate = new Delegation();
    instantiatableDelegate.setClassName("com.foo.Fighting");
    transition.addEvent(new Event(Event.EVENTTYPE_TRANSITION)).addAction(new Action(instantiatableDelegate));
    Element element = AbstractXmlTestCase.toXmlAndParse( processDefinition, "/process-definition/state/transition" );

    assertNotNull(element);
    assertEquals("transition", element.getName());
    assertEquals(1, element.elements("action").size());

    element = element.element("action");
    assertNotNull(element);
    assertEquals("action", element.getName());
    assertEquals("com.foo.Fighting", element.attributeValue("class"));
  }

  public void testWriteConfigurableAction() throws Exception {
    ProcessDefinition processDefinition = new ProcessDefinition();
    State state = (State) processDefinition.addNode( new State("a") );
    Delegation instantiatableDelegate = new Delegation();
    instantiatableDelegate.setClassName("com.foo.Fighting");
    instantiatableDelegate.setConfigType("bean");
    instantiatableDelegate.setConfiguration("<id>4</id><greeting>aloha</greeting>");
    state.addEvent(new Event("node-enter")).addAction(new Action(instantiatableDelegate));
    Element element = AbstractXmlTestCase.toXmlAndParse( processDefinition, "/process-definition/state[1]/event[1]/action[1]" );

    assertNotNull(element);
    assertEquals("action", element.getName());
    assertEquals("bean", element.attributeValue("config-type"));
    assertEquals("4", element.element("id").getTextTrim() );
    assertEquals("aloha", element.element("greeting").getTextTrim() );
  }

  public void testWriteReferenceAction() throws Exception {
    ProcessDefinition processDefinition = new ProcessDefinition();

    // add a global action with name 'pina colada'
    Delegation instantiatableDelegate = new Delegation();
    instantiatableDelegate.setClassName("com.foo.Fighting");
    instantiatableDelegate.setConfigType("bean");
    instantiatableDelegate.setConfiguration("<id>4</id><greeting>aloha</greeting>");
    Action action = new Action();
    action.setName("pina colada");
    action.setActionDelegation(instantiatableDelegate);
    processDefinition.addAction(action);
    
    // now create a reference to it from event node-enter on state 'a'
    State state = (State) processDefinition.addNode( new State() );
    Action refAction = new Action();
    refAction.setReferencedAction(action);
    state.addEvent(new Event(Event.EVENTTYPE_NODE_ENTER)).addAction(refAction);
    
    AbstractXmlTestCase.toXmlAndParse( processDefinition, "/process-definition/state[1]/event[1]/action[1]" );
  }
}
