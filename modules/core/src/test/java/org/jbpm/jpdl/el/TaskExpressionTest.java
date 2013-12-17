package org.jbpm.jpdl.el;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.jbpm.AbstractJbpmTestCase;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.taskmgmt.exe.PooledActor;
import org.jbpm.taskmgmt.exe.TaskInstance;

public class TaskExpressionTest extends AbstractJbpmTestCase {
  
  public static class Customer {
    String salesRepresentative;
    public Customer() {}
    public Customer(String salesRepresentative) {
      this.salesRepresentative = salesRepresentative;
    }
    public String getSalesRepresentative() {
      return salesRepresentative;
    }
    public String toString() {
      return "coca cola";
    }
  }

  public void testTaskDecriptionExpression() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <start-state>" +
      "    <transition to='t'/>" +
      "  </start-state>" +
      "  <task-node name='t'>" +
      "    <task description='screen #{customer} credit rating' />" +
      "  </task-node>" +
      "</process-definition>"
    );
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.getContextInstance().setVariable("customer", new Customer());
    processInstance.signal();
    TaskInstance taskInstance = (TaskInstance) processInstance.getTaskMgmtInstance().getTaskInstances().iterator().next();
    assertEquals("screen coca cola credit rating", taskInstance.getDescription());
  }

  public void testTaskActorIdExpression() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <start-state>" +
      "    <transition to='t'/>" +
      "  </start-state>" +
      "  <task-node name='t'>" +
      "    <task>" +
      "      <assignment actor-id='#{customer.salesRepresentative}' />" +
      "    </task>" +
      "  </task-node>" +
      "</process-definition>"
    );
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.getContextInstance().setVariable("customer", new Customer("jerry"));
    processInstance.signal();
    TaskInstance taskInstance = (TaskInstance) processInstance.getTaskMgmtInstance().getTaskInstances().iterator().next();
    assertEquals("jerry", taskInstance.getActorId());
  }


  public void testPooledActorsExpression() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
      "<process-definition>" +
      "  <start-state>" +
      "    <transition to='t'/>" +
      "  </start-state>" +
      "  <task-node name='t'>" +
      "    <task>" +
      "      <assignment pooled-actors='#{sales}, #{marketing}, maggie' />" +
      "    </task>" +
      "  </task-node>" +
      "</process-definition>"
    );
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance.getContextInstance().setVariable("sales", "homer, marge");
    processInstance.getContextInstance().setVariable("marketing", "bart, lisa");
    processInstance.signal();
    TaskInstance taskInstance = (TaskInstance) processInstance.getTaskMgmtInstance().getTaskInstances().iterator().next();
    
    Set expected = new HashSet();
    expected.add("homer");
    expected.add("marge");
    expected.add("bart");
    expected.add("lisa");
    expected.add("maggie");
    
    Set pooledActorIds = new HashSet();
    Iterator iter = taskInstance.getPooledActors().iterator();
    while (iter.hasNext()) {
      pooledActorIds.add(((PooledActor)iter.next()).getActorId());
    }
    
    assertEquals(expected, pooledActorIds);
  }

}
