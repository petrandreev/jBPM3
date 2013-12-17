package org.jbpm.examples.assignment;

import java.io.InputStream;
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
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.identity.Group;
import org.jbpm.identity.Membership;
import org.jbpm.identity.User;
import org.jbpm.identity.hibernate.IdentitySession;
import org.jbpm.taskmgmt.def.AssignmentHandler;
import org.jbpm.taskmgmt.exe.Assignable;

public class RulesAssignmentHandler implements AssignmentHandler {

  protected String group;
  protected String ruleFile;
  protected List objectNames;

  private static final long serialVersionUID = 1L;
  private static final Log log = LogFactory.getLog(RulesAssignmentHandler.class);

  public void assign(Assignable assignable, ExecutionContext executionContext) throws Exception {
    // load up the rule base
    RuleBase ruleBase = readRule(ruleFile);
    WorkingMemory workingMemory = ruleBase.newStatefulSession();

    // load identity data
    IdentitySession identitySession = (IdentitySession) executionContext.getJbpmContext()
      .getServices()
      .getPersistenceService()
      .getCustomSession(IdentitySession.class);
    // users
    boolean debug = log.isDebugEnabled();
    for (Iterator iter = identitySession.getUsers().iterator(); iter.hasNext();) {
      User user = (User) iter.next();
      if (debug) log.debug("user: " + user.getName());
      workingMemory.insert(user);
    }
    // group
    Group group = identitySession.getGroupByName(this.group);
    if (debug) log.debug("group: " + group.getName());
    workingMemory.insert(group);
    // memberships
    for (Iterator iter = group.getMemberships().iterator(); iter.hasNext();) {
      Membership membership = (Membership) iter.next();
      if (debug) log.debug("membership: " + membership.getName());
      workingMemory.insert(membership);
    }

    // read variables
    ContextInstance ci = executionContext.getContextInstance();
    for (Iterator iter = objectNames.iterator(); iter.hasNext();) {
      String objectName = (String) iter.next();
      Object object = ci.getVariable(objectName);

      if (debug) log.debug("variable '" + objectName + "': " + object);
      workingMemory.insert(object);
    }

    // insert the assignable so that it may be used to set results
    if (debug) log.debug("assignable: " + assignable);
    workingMemory.insert(assignable);

    workingMemory.fireAllRules();
  }

  /**
   * Please note that this is the "low level" rule assembly API.
   */
  private static RuleBase readRule(String ruleFileName) throws Exception {
    InputStream resStream = RulesAssignmentHandler.class.getResourceAsStream(ruleFileName);
    if (resStream == null) {
      throw new IllegalStateException("Cannot obtain rules from: " + ruleFileName);
    }

    PackageBuilder builder = new PackageBuilder();
    builder.addPackageFromDrl(new InputStreamReader(resStream));

    RuleBase ruleBase = RuleBaseFactory.newRuleBase();
    ruleBase.addPackage(builder.getPackage());
    return ruleBase;
  }

}
