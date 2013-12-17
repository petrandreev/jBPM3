package org.jbpm.sim.jpdl;

import org.dom4j.Element;
import org.jbpm.graph.action.Script;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.jpdl.xml.JpdlXmlReader;

/**
 * The SimScript class adds behavior to check, if the script should be executed in a simulation run.
 * 
 * Default behavior is: not execute.
 * 
 * To execute a script during a simulation run, you have to configure either
 * the attribute <b>simulation</b> with the value 'execute' or to add a 
 * <simulation-expression> element.
 * 
 *  Another way is to define not a <script>, but a <simulation-script>, which is only
 *  executed in simulation runs
 * 
 * @author bernd.ruecker@camunda.com
 */
public class SimScript extends Script {
  
  private static final long serialVersionUID = 1L;
  
  boolean simulate = false;

  public void read(Element scriptElement, JpdlXmlReader jpdlReader) {
    super.read(scriptElement, jpdlReader);

    if (!scriptElement.isTextOnly()) {
      String simulation = scriptElement.attributeValue("simulation");
      if ( "execute".equals(simulation)
           || "simulation-script".equals(scriptElement.getQName().getName())
         ) {
        simulate = true;
      }
      
      String simulationExpression = scriptElement.elementText("simulation-expression");
      if (simulationExpression!=null) {
        simulate = true;
        setExpression( simulationExpression );
      }
    }
  }

  public void execute(ExecutionContext executionContext) throws Exception {
    if (simulate) {
      super.execute(executionContext);
    }
  }
}
