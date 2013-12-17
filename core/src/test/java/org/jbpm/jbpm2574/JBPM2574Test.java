package org.jbpm.jbpm2574;

import org.jbpm.JbpmException;
import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.file.def.FileDefinition;
import org.jbpm.graph.def.DelegationException;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;

/**
 * Attempt to load inexistent class throws {@link JbpmException} instead of
 * {@link ClassNotFoundException}
 * 
 * @see <a href="https://jira.jboss.org/jira/browse/JBPM-2574">JBPM-2574</a>
 * @author Alejandro Guizar
 */
public class JBPM2574Test extends AbstractDbTestCase {

  protected void setUp() throws Exception {
    super.setUp();

    ProcessDefinition processDefinition = ProcessDefinition.parseXmlResource("org/jbpm/jbpm2574/processdefinition.xml");
    processDefinition.addDefinition(new FileDefinition());
    deployProcessDefinition(processDefinition);
  }

  public void testLoadInexistentClass() {
    try {
      ProcessInstance processInstance = jbpmContext.newProcessInstance("scripted");
      processInstance.signal();
    }
    catch (DelegationException e) {
      assert e.getCause() instanceof ClassNotFoundException : e.getCause();
    }
  }
}
