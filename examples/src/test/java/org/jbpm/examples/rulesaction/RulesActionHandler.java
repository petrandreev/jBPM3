package org.jbpm.examples.rulesaction;

import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.drools.RuleBase;
import org.drools.RuleBaseFactory;
import org.drools.WorkingMemory;
import org.drools.compiler.PackageBuilder;

import org.jbpm.context.exe.ContextInstance;
import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;

public class RulesActionHandler implements ActionHandler {

  protected List objectNames;

  protected String ruleFile;

  protected List queryStrings;

  private static final long serialVersionUID = 1L;
  private static final Log log = LogFactory.getLog(RulesActionHandler.class.getName());

  /**
   * The RulesActionHandler gets variables from the ContextInstance, and asserts them into the
   * Rules Engine and invokes the rules.
   */
  public void execute(ExecutionContext executionContext) throws Exception {
    // load up the rulebase
    RuleBase ruleBase = readRule(ruleFile);
    WorkingMemory workingMemory = ruleBase.newStatefulSession();

    // read variables
    boolean debug = log.isDebugEnabled();
    ContextInstance ci = executionContext.getContextInstance();
    for (Iterator iter = objectNames.iterator(); iter.hasNext();) {
      String objectName = (String) iter.next();
      Object object = ci.getVariable(objectName);

      if (debug) log.debug("variable '" + objectName + "': " + object);
      workingMemory.insert(object);
    }

    // now assert the context instance as a global,
    // so that the rules can update the process
    workingMemory.setGlobal("ci", ci);
    workingMemory.fireAllRules();
    workingMemory.clearAgenda();

    // if this action is the behavior of a node, continue execution
    if (executionContext.getEvent() == null) executionContext.leaveNode();
  }

  /**
   * Please note that this is the "low level" rule assembly API.
   */
  private static RuleBase readRule(String ruleFileName) throws Exception {
    PackageBuilder builder = new PackageBuilder();
    builder.addPackageFromDrl(new InputStreamReader(
      RulesActionHandler.class.getResourceAsStream(ruleFileName)));

    RuleBase ruleBase = RuleBaseFactory.newRuleBase();
    ruleBase.addPackage(builder.getPackage());
    return ruleBase;
  }

}
