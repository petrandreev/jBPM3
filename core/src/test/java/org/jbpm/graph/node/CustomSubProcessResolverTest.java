package org.jbpm.graph.node;

import java.util.HashMap;
import java.util.Map;

import org.dom4j.Element;
import org.jbpm.AbstractJbpmTestCase;
import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.graph.def.ProcessDefinition;

public class CustomSubProcessResolverTest extends AbstractJbpmTestCase {

  private JbpmContext jbpmContext;

  protected void setUp() throws Exception {
    super.setUp();
    JbpmConfiguration jbpmConfiguration = JbpmConfiguration.parseXmlString("<jbpm-configuration>"
      + "  <bean name='jbpm.sub.process.resolver' class='"
      + MapBasedProcessRepository.class.getName()
      + "' singleton='true' />"
      + "</jbpm-configuration>");
    jbpmContext = jbpmConfiguration.createJbpmContext();
  }

  protected void tearDown() throws Exception {
    jbpmContext.close();
    jbpmContext.getJbpmConfiguration().close();
    super.tearDown();
  }

  public static class MapBasedProcessRepository implements SubProcessResolver {
    private static final long serialVersionUID = 1L;

    private Map processes = new HashMap();

    public void add(ProcessDefinition processDefinition) {
      processes.put(processDefinition.getName(), processDefinition);
    }

    public ProcessDefinition findSubProcess(Element subProcessElement) {
      String processName = subProcessElement.attributeValue("name");
      return (ProcessDefinition) processes.get(processName);
    }
  }

  public void testMapBasedProcessResolving() {
    ProcessDefinition subDefinition = ProcessDefinition.parseXmlString("<process-definition name='sub'>"
      + "  <start-state>"
      + "    <transition to='end' />"
      + "  </start-state>"
      + "  <end-state name='end' />"
      + "</process-definition>");
    MapBasedProcessRepository processRepository = (MapBasedProcessRepository) jbpmContext
      .getObjectFactory()
      .createObject("jbpm.sub.process.resolver");
    processRepository.add(subDefinition);

    ProcessDefinition superDefinition = ProcessDefinition.parseXmlString("<process-definition name='super'>"
      + "  <start-state>"
      + "    <transition to='p' />"
      + "  </start-state>"
      + "  <process-state name='p'>"
      + "    <sub-process name='sub' />"
      + "    <transition to='end' />"
      + "  </process-state>"
      + "  <end-state name='end' />"
      + "</process-definition>");

    ProcessState processState = (ProcessState) superDefinition.getNode("p");
    assertSame(subDefinition, processState.getSubProcessDefinition());
  }
}
