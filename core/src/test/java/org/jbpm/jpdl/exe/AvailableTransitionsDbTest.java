package org.jbpm.jpdl.exe;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.jbpm.context.exe.ContextInstance;
import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.Node;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.def.SuperState;
import org.jbpm.graph.def.Transition;
import org.jbpm.graph.exe.ProcessInstance;

public class AvailableTransitionsDbTest extends AbstractDbTestCase {

  public void testSimpleAvailableTransitions() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition name='conditionalavailability' initial='start'>"
      + "  <state name='start'>"
      + "    <transition to='decision'/>"
      + "  </state>" 
      + "  <decision name='decision'>"
      + "    <transition name='high' to='theone'>"
      + "      <condition expression='#{a &gt; 0}' />"
      + "    </transition>"
      + "    <transition name='medium' to='theone'>"
      + "      <condition expression='#{a == 0}' />"
      + "    </transition>"
      + "    <transition name='low' to='theother'>"
      + "      <condition expression='#{a &lt;= 0}' />"
      + "    </transition>"
      + "    <transition name='alwaysavailable' to='theother'/>"
      + "  </decision>"
      + "  <state name='theone'/>"
      + "  <state name='theother'/>"
      + "</process-definition>");
    deployProcessDefinition(processDefinition);

    ProcessInstance processInstance = jbpmContext.newProcessInstance("conditionalavailability");
    processInstance.getRootToken().setNode(processDefinition.findNode("decision"));
    processInstance = saveAndReload(processInstance);

    Set availableTransitions = processInstance.getRootToken().getAvailableTransitions();
    assertEquals(1, availableTransitions.size());
    ContextInstance contextInstance = processInstance.getContextInstance();

    contextInstance.setVariable("a", new Integer(-3));
    processInstance = saveAndReload(processInstance);
    contextInstance = processInstance.getContextInstance();

    Set expectedTransitionNames = new HashSet();
    expectedTransitionNames.add("low");
    expectedTransitionNames.add("alwaysavailable");

    availableTransitions = processInstance.getRootToken().getAvailableTransitions();
    assertEquals(expectedTransitionNames, getTransitionNames(availableTransitions));

    contextInstance.setVariable("a", new Integer(0));
    processInstance = saveAndReload(processInstance);
    contextInstance = processInstance.getContextInstance();

    expectedTransitionNames = new HashSet();
    expectedTransitionNames.add("low");
    expectedTransitionNames.add("medium");
    expectedTransitionNames.add("alwaysavailable");

    availableTransitions = processInstance.getRootToken().getAvailableTransitions();
    assertEquals(expectedTransitionNames, getTransitionNames(availableTransitions));

    contextInstance.setVariable("a", new Integer(4));
    processInstance = saveAndReload(processInstance);
    contextInstance = processInstance.getContextInstance();

    expectedTransitionNames = new HashSet();
    expectedTransitionNames.add("high");
    expectedTransitionNames.add("alwaysavailable");

    availableTransitions = processInstance.getRootToken().getAvailableTransitions();
    assertEquals(expectedTransitionNames, getTransitionNames(availableTransitions));
  }

  public Set getTransitionNames(Set availableTransitions) {
    Set transitionNames = new HashSet();
    if (availableTransitions != null) {
      for (Iterator iter = availableTransitions.iterator(); iter.hasNext();) {
        Transition transition = (Transition) iter.next();
        transitionNames.add(transition.getName());
      }
    }
    return transitionNames;
  }

  public void testSuperStateAvailableTransitions() {
    // Conditions are no longer valid on transitions from SuperStates! 
    
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition name='conditionalavailability' initial='start'>"
      + "  <super-state name='start'>"
      + "    <state name='go'>"
      + "      <transition to='decn' />"
      + "    </state>"
      + "    <decision name='decn'>"
      + "      <transition name='inneralwaysavailable' to='../theother' />"
      + "      <transition name='five' to='../theone'>"
      + "        <condition expression='#{a == 5}' />"
      + "      </transition>"
      + "    </decision>"
      + "    <transition name='high' to='theone'>"
//      + "      <condition expression='#{a &gt; 0}' />"
      + "    </transition>"
      + "    <transition name='medium' to='theone'>"
//      + "      <condition expression='#{a == 0}' />"
      + "    </transition>"
      + "    <transition name='low' to='theother'>"
//      + "      <condition expression='#{a &lt;= 0}' />"
      + "    </transition>"
      + "    <transition name='outeralwaysavailable' to='theother'/>"
      + "  </super-state>"
      + "  <state name='theone'/>"
      + "  <state name='theother'/>"
      + "</process-definition>");
    deployProcessDefinition(processDefinition);

    ProcessInstance processInstance = jbpmContext.newProcessInstance("conditionalavailability");
    SuperState superState = (SuperState) processDefinition.findNode("start");
    Node decisionNode = (Node) superState.getNodesMap().get("decn");
    processInstance.getRootToken().setNode(decisionNode);
    Set availableTransitions = processInstance.getRootToken().getAvailableTransitions();
    // 2 from decision + 3 from superstate: see Token.addAvailableTransitionsOfNode 
    // which is called by getAvailableTransitions();
    assertEquals(5, availableTransitions.size());

    ContextInstance contextInstance = processInstance.getContextInstance();
    contextInstance.setVariable("a", new Integer(5));
    processInstance = saveAndReload(processInstance);
    contextInstance = processInstance.getContextInstance();

    Set expectedTransitionNames = new HashSet();
    expectedTransitionNames.add("five");
    expectedTransitionNames.add("inneralwaysavailable");
    expectedTransitionNames.add("outeralwaysavailable");
    expectedTransitionNames.add("high");
    expectedTransitionNames.add("medium");
    expectedTransitionNames.add("low");

    availableTransitions = processInstance.getRootToken().getAvailableTransitions();
    assertEquals(expectedTransitionNames, getTransitionNames(availableTransitions));
  }
}
