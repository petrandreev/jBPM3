package org.jbpm.db;

import org.jbpm.context.exe.ContextInstance;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;

public class DeleteProcessInstanceDbTest extends AbstractDbTestCase {

  public void testDeleteProcessInstance() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition name='make fondue'>"
        + "  <start-state>"
        + "    <transition to='buy cheese' />"
        + "  </start-state>"
        + "  <state name='buy cheese' />"
        + "</process-definition>");
    deployProcessDefinition(processDefinition);

    ProcessInstance processInstance = jbpmContext.newProcessInstance("make fondue");
    processInstance.signal();

    processInstance = saveAndReload(processInstance);
    jbpmContext.getGraphSession().deleteProcessInstance(processInstance);

    newTransaction();
    assertDeleted(processInstance);
  }

  public void testDeleteProcessInstanceWithTask() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition name='make fondue'>"
        + "  <start-state>"
        + "    <transition to='buy cheese' />"
        + "  </start-state>"
        + "  <task-node name='buy cheese'>"
        + "    <task />"
        + "  </task-node>"
        + "</process-definition>");
    deployProcessDefinition(processDefinition);

    ProcessInstance processInstance = jbpmContext.newProcessInstance("make fondue");
    processInstance.signal();

    processInstance = saveAndReload(processInstance);
    jbpmContext.getGraphSession().deleteProcessInstance(processInstance);

    newTransaction();
    assertDeleted(processInstance);
  }

  public void testDeleteProcessInstanceWithSubProcessInstance() {
    ProcessDefinition buyCheese = ProcessDefinition.parseXmlString("<process-definition name='buy cheese'>"
        + "  <start-state>"
        + "    <transition to='find shop' />"
        + "  </start-state>"
        + "  <state name='find shop' />"
        + "</process-definition>");
    deployProcessDefinition(buyCheese);

    ProcessDefinition makeFondue = ProcessDefinition.parseXmlString("<process-definition name='make fondue'>"
        + "  <start-state>"
        + "    <transition to='buy cheese' />"
        + "  </start-state>"
        + "  <process-state name='buy cheese'>"
        + "    <sub-process name='buy cheese' />"
        + "  </process-state>"
        + "</process-definition>");
    deployProcessDefinition(makeFondue);

    ProcessInstance processInstance = jbpmContext.newProcessInstance("make fondue");
    processInstance.signal();

    processInstance = saveAndReload(processInstance);
    jbpmContext.getGraphSession().deleteProcessInstance(processInstance);

    newTransaction();
    assertDeleted(processInstance.getRootToken().getProcessInstance());
    assertDeleted(processInstance);
  }

  public void testDeleteProcessInstanceWithConcurrentPathsOfExecution() {
    ProcessDefinition makeFondue = ProcessDefinition.parseXmlString("<process-definition name='make fondue'>"
        + "  <start-state>"
        + "    <transition to='fork' />"
        + "  </start-state>"
        + "  <fork name='fork'>"
        + "    <transition name='cheese' to='buy cheese' />"
        + "    <transition name='bread' to='bake bread' />"
        + "  </fork>"
        + "  <state name='buy cheese' />"
        + "  <state name='bake bread' />"
        + "</process-definition>");
    deployProcessDefinition(makeFondue);

    ProcessInstance processInstance = jbpmContext.newProcessInstance("make fondue");
    ContextInstance contextInstance = processInstance.getContextInstance();
    contextInstance.setVariable("a", "asterix");
    contextInstance.setVariable("b", "obelix");

    processInstance.signal();
    Token cheese = processInstance.getRootToken().getChild("cheese");
    contextInstance.setVariable("a", "mik", cheese);
    contextInstance.setVariable("b", "mak", cheese);
    contextInstance.setVariable("c", "mon", cheese);

    Token bread = processInstance.getRootToken().getChild("bread");
    contextInstance.setVariable("a", "jip", bread);
    contextInstance.setVariable("b", "janneke", bread);

    processInstance = saveAndReload(processInstance);
    jbpmContext.getGraphSession().deleteProcessInstance(processInstance);

    newTransaction();
    assertDeleted(processInstance);
  }
  
  // Test related to JBPM-3171: if the ProcessInstance.hashCode function contains the process instance key,
  // the variable handling is broken, which results in a Integrity constraint violation FK_TKVARMAP_CTXT table: JBPM_TOKENVARIABLEMAP
  // at process instance deletion.
  public void testDeleteProcessInstanceWithSubProcessInstanceAndVariableAndSetKey() {
    ProcessDefinition buyCheese = ProcessDefinition.parseXmlString("<process-definition name='buy cheese'>"
        + "  <start-state>"
        + "    <transition to='find shop' />"
        + "      <event type='node-leave'>"
        + "        <script name='set_key'>"
        + "          executionContext.getProcessInstance().setKey(\"fondue_purchase\");"
        + "        </script>"
        + "      </event>"      
        + "  </start-state>"
        + "  <state name='find shop' >"
        + "      <event type='node-enter'>"
        + "        <script name='change_variable'>"
        + "          executionContext.setVariable(\"cheese\", \"comte\");"
        + "        </script>"
        + "      </event>"      
        + " </state>"
        + "</process-definition>");
    deployProcessDefinition(buyCheese);

    ProcessDefinition makeFondue = ProcessDefinition.parseXmlString("<process-definition name='make fondue'>"
        + "  <start-state>"
        + "    <transition to='buy cheese' />"
        + "      <event type='node-leave'>"
        + "        <script name='add_variable'>"
        + "          executionContext.setVariable(\"cheese\", \"vacherin\");"
        + "        </script>"
        + "      </event>"
        + "  </start-state>"
        + "  <process-state name='buy cheese'>"
        + "    <sub-process name='buy cheese'/>"
        + "    <variable name='cheese' access='read,write' mapped-name='cheese'/>"
        + "  </process-state>"
        + "</process-definition>");
    deployProcessDefinition(makeFondue);

    ProcessInstance processInstance = jbpmContext.newProcessInstance("make fondue");
    processInstance.signal();

    processInstance = saveAndReload(processInstance);
    jbpmContext.getGraphSession().deleteProcessInstance(processInstance);

    newTransaction();
    assertDeleted(processInstance.getRootToken().getProcessInstance());
    assertDeleted(processInstance);
  }

  
  private void assertDeleted(ProcessInstance processInstance) {
    long processInstanceId = processInstance.getId();
    assertNull("process instance not deleted: " + processInstanceId,
        graphSession.getProcessInstance(processInstanceId));
  }
}
