package org.jbpm.context.exe;

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
    deployProcessDefinition(processDefinition);

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

    Set expectedPids = new HashSet();
    expectedPids.add(new Long(one.getId()));
    expectedPids.add(new Long(two.getId()));

    Query query = session.createQuery("select pi.id "
      + "from org.jbpm.context.exe.variableinstance.StringInstance si "
      + "join si.processInstance pi "
      + "where si.name = 'category'"
      + "  and si.value like 'overpaid'");
    Set retrievedPids = new HashSet(query.list());

    assertEquals(expectedPids, retrievedPids);

    newTransaction();

    expectedPids.clear();
    expectedPids.add(new Long(three.getId()));

    query = session.createQuery("select pi.id "
      + "from org.jbpm.context.exe.variableinstance.StringInstance si "
      + "join si.processInstance pi "
      + "where si.name = 'category'"
      + "  and si.value like 'underpaid'");
    retrievedPids.clear();
    retrievedPids.addAll(query.list());

    assertEquals(expectedPids, retrievedPids);
  }
}
