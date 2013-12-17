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
import org.hibernate.Query;
import org.hibernate.Session;
import org.jbpm.context.exe.ContextInstance;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.identity.Entity;
import org.jbpm.taskmgmt.def.AssignmentHandler;
import org.jbpm.taskmgmt.exe.Assignable;

public class RulesAssignmentHandler implements AssignmentHandler
{
  private static final long serialVersionUID = 1L;

  private static Log log = LogFactory.getLog(RulesAssignmentHandler.class);
  
  public String group;
  public String ruleFile;
  public List objectNames;

  public void assign(Assignable assignable, ExecutionContext executionContext) throws Exception
  {
    // load up the rulebase
    RuleBase ruleBase = readRule(ruleFile);
    WorkingMemory workingMemory = ruleBase.newStatefulSession();

    // load the data
    Session s = executionContext.getJbpmContext().getSession();
    log.debug("************** Session is :" + s.toString());

    assertObjects(getUsers(s), workingMemory);
    assertObjects(getGroupByName(s, group), workingMemory);
    assertObjects(getMemberships(s), workingMemory);

    Object object = null;
    log.debug(objectNames.toString());
    Iterator iter = objectNames.iterator();
    String objectName = "";
    ContextInstance ci = executionContext.getContextInstance();
    while (iter.hasNext())
    {
      objectName = (String)iter.next();
      object = ci.getVariable(objectName);

      log.debug("object name is: " + objectName);
      // assert the object into the rules engine
      workingMemory.insert(object);
    }

    // assert the assignable so that it may be used to set results
    log.debug("assignable is: " + assignable);

    workingMemory.insert(assignable);
    log.debug("fire all rules: ");
    workingMemory.fireAllRules();
  }

  /**
   * Please note that this is the "low level" rule assembly API.
   */
  private static RuleBase readRule(String ruleFileName) throws Exception
  {
    InputStream resStream = RulesAssignmentHandler.class.getResourceAsStream(ruleFileName);
    if (resStream == null)
      throw new IllegalStateException("Cannot obtain rules from: " + ruleFileName);

    PackageBuilder builder = new PackageBuilder();
    builder.addPackageFromDrl(new InputStreamReader(resStream));

    RuleBase ruleBase = RuleBaseFactory.newRuleBase();
    ruleBase.addPackage(builder.getPackage());
    return ruleBase;
  }

  private List getUsers(Session session)
  {
    Query query = session.createQuery("select u " + "from org.jbpm.identity.User as u");
    return query.list();
  }

  private List getGroupByName(Session session, String groupName)
  {
    Query query = session.createQuery("select g " + "from org.jbpm.identity.Group as g " + "where g.name = :groupName");
    log.debug("groupName is: " + groupName);
    query.setString("groupName", groupName);
    return query.list();
  }

  private List getMemberships(Session session)
  {
    Query query = session.createQuery("select m " + "from org.jbpm.identity.Membership as m");
    return query.list();
  }

  private void assertObjects(List objectList, WorkingMemory workingMemory)
  {
    Iterator iter = objectList.iterator();
    Entity entity = null;
    while (iter.hasNext())
    {
      entity = (Entity)iter.next();
      log.debug("object is: " + entity.getName());
      workingMemory.insert(entity);
    }

  }

}
