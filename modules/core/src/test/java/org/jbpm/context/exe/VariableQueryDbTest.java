package org.jbpm.context.exe;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.Query;
import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;

public class VariableQueryDbTest extends AbstractDbTestCase {

  public void testStringVariableQuery() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition name='variables'>"
        + "  <start-state name='start'/>"
        + "</process-definition>");
    jbpmContext.deployProcessDefinition(processDefinition);
    try {
      newTransaction();

      ProcessInstance one = jbpmContext.newProcessInstanceForUpdate("variables");
      one.getContextInstance().setVariable("category", "overpaid");
      one.getContextInstance().setVariable("duedate", "tomorrow");

      ProcessInstance two = jbpmContext.newProcessInstanceForUpdate("variables");
      two.getContextInstance().setVariable("category", "overpaid");
      two.getContextInstance().setVariable("duedate", "yesterday");

      ProcessInstance three = jbpmContext.newProcessInstanceForUpdate("variables");
      three.getContextInstance().setVariable("category", "underpaid");
      three.getContextInstance().setVariable("duedate", "today");

      newTransaction();

      Query query = session.createQuery("select pi.id "
          + "from org.jbpm.context.exe.variableinstance.StringInstance si "
          + "join si.processInstance pi "
          + "where si.name = 'category'"
          + "  and si.value like 'overpaid'");

      Set<Long> expectedPids = new HashSet<Long>();
      expectedPids.add(one.getId());
      expectedPids.add(two.getId());

      Set<Long> retrievedPids = new HashSet<Long>();
      for (Object result : query.list()) {
        retrievedPids.add((Long) result);
      }

      assertEquals(expectedPids, retrievedPids);

      newTransaction();

      query = session.createQuery("select pi.id "
          + "from org.jbpm.context.exe.variableinstance.StringInstance si "
          + "join si.processInstance pi "
          + "where si.name = 'category'"
          + "  and si.value like 'underpaid'");

      expectedPids = Collections.singleton(three.getId());

      retrievedPids = new HashSet<Long>();
      for (Object result : query.list()) {
        retrievedPids.add((Long) result);
      }

      assertEquals(expectedPids, retrievedPids);
    }
    finally {
      jbpmContext.getGraphSession().deleteProcessDefinition(processDefinition.getId());
    }
  }
}
