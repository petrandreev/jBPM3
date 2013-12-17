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

import org.dom4j.Element;

import org.jbpm.graph.def.Action;
import org.jbpm.graph.def.Event;
import org.jbpm.graph.def.Node;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.def.Transition;
import org.jbpm.instantiation.Delegation;
import org.jbpm.jpdl.JpdlException;

// TODO create validation paths for each element positive test and negative test

public class ActionValidatingXmlTest extends AbstractXmlTestCase {

  private static final String jpdlNamespace = "urn:jbpm.org:jpdl-3.2";

  public void testInvalidXML() {
    try {
      ProcessDefinition.parseXmlString("<process-definition xmlns='"
          + jpdlNamespace
          + "' name='pd'>"
          + "  <event type='process-start'>"
          + "    <action xyz='2' class='one'/>"
          + "    <action class='two'/>"
          + "    <action class='three'/>"
          + "  </event>"
          + "</process-definition>");
      fail("expected exception");
    }
    catch (JpdlException je) {
      // OK
    }
  }

  public void testReadProcessDefinitionAction() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition xmlns='"
        + jpdlNamespace
        + "' name='pd'>"
        + "  <event type='process-start'>"
        + "    <action class='one'/>"
        + "    <action class='two'/>"
        + "    <action class='three'/>"
        + "  </event>"
        + "</process-definition>");

    Event event = processDefinition.getEvent("process-start");
    assertEquals(3, event.getActions().size());
    assertEquals("one", ((Action) event.getActions().get(0)).getActionDelegation()
        .getClassName());
    assertEquals("two", ((Action) event.getActions().get(1)).getActionDelegation()
        .getClassName());
    assertEquals("three", ((Action) event.getActions().get(2)).getActionDelegation()
        .getClassName());
  }

  public void testReadActionConfigType() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition xmlns='"
        + jpdlNamespace
        + "' name='pd'>"
        + "  <action name='burps' class='org.foo.Burps' config-type='bean' />"
        + "</process-definition>");

    assertEquals("bean", processDefinition.getAction("burps")
        .getActionDelegation()
        .getConfigType());
  }

  public void testReadActionXmlConfiguration() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition xmlns='"
        + jpdlNamespace
        + "' name='pd'>\n"
        + "  <action name='burps' class='org.foo.Burps' config-type='bean'>\n"
        + "    <id>63</id>\n"
        + "    <greeting>aloha</greeting>\n"
        + "  </action>\n"
        + "</process-definition>");

    Action action = processDefinition.getAction("burps");
    Delegation instantiatableDelegate = action.getActionDelegation();
    log.debug("configuration: " + instantiatableDelegate.getConfiguration());
    assertTrue(instantiatableDelegate.getConfiguration().indexOf(
        "<id xmlns=\"" + jpdlNamespace + "\">63</id>") != -1);
    assertTrue(instantiatableDelegate.getConfiguration().indexOf(
        "<greeting xmlns=\"" + jpdlNamespace + "\">aloha</greeting>") != -1);
  }

  public void testReadActionTextConfiguration() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition xmlns='"
        + jpdlNamespace
        + "' name='pd'>\n"
        + "  <action name='burps' class='org.foo.Burps' config-type='constructor'>\n"
        + "    a piece of configuration text\n"
        + "  </action>\n"
        + "</process-definition>");

    Action action = processDefinition.getAction("burps");
    Delegation instantiatableDelegate = action.getActionDelegation();
    assertTrue(instantiatableDelegate.getConfiguration().indexOf(
        "a piece of configuration text") != -1);
  }

  public void testReadActionAcceptPropagatedEventsDefault() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition xmlns='"
        + jpdlNamespace
        + "' name='pd'>"
        + "  <action name='burps' class='org.foo.Burps' />"
        + "</process-definition>");

    Action action = processDefinition.getAction("burps");
    assertTrue(action.acceptsPropagatedEvents());
  }

  public void testReadActionAcceptPropagatedEventsTrue() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition xmlns='"
        + jpdlNamespace
        + "' name='pd'>"
        + "  <action name='burps' class='org.foo.Burps' accept-propagated-events='true' />"
        + "</process-definition>");

    Action action = processDefinition.getAction("burps");
    assertTrue(action.acceptsPropagatedEvents());
  }

  public void testReadActionAcceptPropagatedEventsFalse() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition xmlns='"
        + jpdlNamespace
        + "' name='pd'>"
        + "  <action name='burps' class='org.foo.Burps' accept-propagated-events='false' />"
        + "</process-definition>");

    Action action = processDefinition.getAction("burps");
    assertFalse(action.acceptsPropagatedEvents());
  }

  public void testReadNodeActionName() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition xmlns='"
        + jpdlNamespace
        + "' name='pd'>"
        + "  <node name='a'>"
        + "    <action class='one'/>"
        + "    <event type='node-enter'>"
        + "      <action name='burps' class='org.foo.Burps'/>"
        + "    </event>"
        + "  </node>"
        + "</process-definition>");
    Action burps = (Action) processDefinition.getNode("a")
        .getEvent("node-enter")
        .getActions()
        .get(0);
    assertEquals("burps", burps.getName());
  }

  public void testReadNodeEnterAction() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition xmlns='"
        + jpdlNamespace
        + "' name='pd'>"
        + "  <node name='a'>"
        + "    <action class='one'/>"
        + "    <event type='node-enter'>"
        + "      <action class='org.foo.Burps'/>"
        + "    </event>"
        + "  </node>"
        + "</process-definition>");
    assertEquals("org.foo.Burps", ((Action) processDefinition.getNode("a").getEvent(
        "node-enter").getActions().get(0)).getActionDelegation().getClassName());
  }

  public void testParseAndWriteOfNamedEventActions() throws Exception {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition xmlns='"
        + jpdlNamespace
        + "' name='pd'>\n"
        + "  <node name='a'>"
        + "    <action class='one'/>"
        + "    <event type='node-enter'>"
        + "      <action name='burps' class='org.foo.Burps'/>"
        + "    </event>"
        + "  </node>"
        + "</process-definition>");
    Action burps = (Action) processDefinition.getNode("a")
        .getEvent("node-enter")
        .getActions()
        .get(0);
    assertSame(burps, processDefinition.getAction("burps"));
    Element processDefinitionElement = toXmlAndParse(processDefinition, "/process-definition");
    assertEquals(0, processDefinitionElement.elements("action").size());
  }

  public void testParseStateAction() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition xmlns='"
        + jpdlNamespace
        + "' name='pd'>\n"
        + "  <state name='a'>\n"
        + "    <event type='node-enter'>\n"
        + "      <action class='org.foo.Burps' config-type='constructor'>\n"
        + "        this text should be passed in the constructor\n"
        + "      </action>\n"
        + "    </event>\n"
        + "  </state>\n"
        + "</process-definition>");

    Node node = processDefinition.getNode("a");
    Event event = node.getEvent("node-enter");
    Action action = (Action) event.getActions().iterator().next();
    Delegation instantiatableDelegate = action.getActionDelegation();
    assertEquals("org.foo.Burps", instantiatableDelegate.getClassName());
    assertEquals("constructor", instantiatableDelegate.getConfigType());
    assertTrue(instantiatableDelegate.getConfiguration().indexOf(
        "this text should be passed in the constructor") != -1);
  }

  public void testParseTransitionAction() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition xmlns='"
        + jpdlNamespace
        + "' name='pd'>\n"
        + "  <state name='a'>"
        + "    <transition to='b'>"
        + "      <action class='org.foo.Burps'/>"
        + "    </transition>"
        + "  </state>"
        + "  <state name='b' />"
        + "</process-definition>");

    Node node = processDefinition.getNode("a");
    assertEquals(1, node.getLeavingTransitionsMap().size());
    Transition transition = node.getDefaultLeavingTransition();
    Event event = transition.getEvent(Event.EVENTTYPE_TRANSITION);
    Action action = (Action) event.getActions().iterator().next();
    Delegation instantiatableDelegate = action.getActionDelegation();
    assertEquals("org.foo.Burps", instantiatableDelegate.getClassName());
  }

  public void testParseReferencedAction() {
    ProcessDefinition processDefinition = null;
    try {
      processDefinition = ProcessDefinition.parseXmlString("<process-definition xmlns='"
          + jpdlNamespace
          + "' name='pd'>\n"
          + "  <node name='a'>"
          + "    <action class='one'/>"
          + "    <transition to='b'>"
          + "      <action ref-name='scratch'/>"
          + "    </transition>"
          + "  </node>"
          + "  <node name='b'>"
          + "    <action class='two'/>"
          + "  </node>"
          + "  <action name='scratch' class='com.itch.Scratch' />"
          + "</process-definition>");
    }
    catch (JpdlException je) {
      fail("XML did not pass validation as expected:\n" + je.toString());
    }

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
    StringReader stringReader = new StringReader("<process-definition xmlns='"
        + jpdlNamespace
        + "' name='pd'>\n"
        + "  <node name='a'>\n"
        + "    <action class='one'/>"
        + "    <event type='node-enter'>\n"
        + "      <action />\n"
        + "    </event>\n"
        + "  </node>\n"
        + "</process-definition>");
    JpdlXmlReader jpdlReader = new JpdlXmlReader(stringReader);
    jpdlReader.readProcessDefinition();
    assertTrue(Problem.containsProblemsOfLevel(jpdlReader.problems, Problem.LEVEL_WARNING));
  }

  public void testParseActionWithInvalidReference() {
    StringReader stringReader = new StringReader("<process-definition xmlns='"
        + jpdlNamespace
        + "' name='pd'>\n"
        + "  <node name='a'>"
        + "    <action class='one'/>"
        + "    <event type='node-enter'>"
        + "      <action ref-name='non-existing-action-name'/>"
        + "    </event>"
        + "  </node>"
        + "</process-definition>");
    JpdlXmlReader jpdlReader = new JpdlXmlReader(stringReader);
    jpdlReader.readProcessDefinition();
    assertTrue(Problem.containsProblemsOfLevel(jpdlReader.problems, Problem.LEVEL_WARNING));
  }
}
