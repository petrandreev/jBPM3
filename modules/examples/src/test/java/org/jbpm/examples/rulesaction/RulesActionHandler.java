package org.jbpm.examples.rulesaction;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.List;

import org.drools.PackageIntegrationException;
import org.drools.RuleBase;
import org.drools.RuleBaseFactory;
import org.drools.RuleIntegrationException;
import org.drools.WorkingMemory;
import org.drools.compiler.DroolsParserException;
import org.drools.compiler.PackageBuilder;
import org.drools.rule.InvalidPatternException;
import org.jbpm.context.exe.ContextInstance;
import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;

public class RulesActionHandler implements ActionHandler
{

  private static final long serialVersionUID = 1L;

  public List objectNames;

  public String ruleFile;

  public List queryStrings;

  /**
   * The RulesActionHandler gets variables from the ContextInstance, and asserts them into the Rules Engine and invokes the rules.
   */
  public void execute(ExecutionContext executionContext) throws Exception
  {

    // load up the rulebase
    RuleBase ruleBase = readRule(ruleFile);
    WorkingMemory workingMemory = ruleBase.newStatefulSession();

    // final WorkingMemoryFileLogger logger = new WorkingMemoryFileLogger(
    // workingMemory );
    // logger.setFileName( "jpdl/examples/rulesActionHandler/log/rulesAction" );

    // get an iterator of fully qualified object names
    Iterator iter = objectNames.iterator();
    String objectName = "";
    ContextInstance ci = executionContext.getContextInstance();

    while (iter.hasNext())
    {
      objectName = (String)iter.next();

      // assume the objects are stored as process variables
      // workingMemory.assertObject(ci.getVariable(objectName));
      Object object = ci.getVariable(objectName);
      workingMemory.insert(object);

      // alternately the objects could be returned with a query
      // to Hibernate or an EJB3 entity manager

    }

    // now assert the context instance as a global, so that the rules
    // can update the process, and fire the rules
    workingMemory.setGlobal("ci", ci);

    workingMemory.fireAllRules();

    workingMemory.clearAgenda();

    // logger.writeToDisk();

    // propagate the token so that the process continues
    executionContext.getToken().signal();

  }

  /**
   * Please note that this is the "low level" rule assembly API.
   */
  private static RuleBase readRule(String ruleFileName) throws IOException, DroolsParserException, RuleIntegrationException, PackageIntegrationException,
      InvalidPatternException, Exception
  {

    PackageBuilder builder = new PackageBuilder();
    builder.addPackageFromDrl(new InputStreamReader(RulesActionHandler.class.getResourceAsStream(ruleFileName)));

    RuleBase ruleBase = RuleBaseFactory.newRuleBase();
    ruleBase.addPackage(builder.getPackage());
    return ruleBase;
  }

}
