package org.jbpm.sim.tutorial;

import java.util.Date;
import java.util.Random;

import org.hsqldb.Server;
import org.hsqldb.ServerConfiguration;
import org.hsqldb.persist.HsqlProperties;
import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.sim.bam.BamSimulationProposal;
import org.jbpm.sim.bam.GetSimulationInputCommand;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.jbpm.util.Clock;
import org.jbpm.util.Clock.DateGenerator;

/**
 * This class fakes "historical" process runs by hard coded
 * process executions. It's purpose is to generate log data
 * which can be used as simulation input in this tutorial
 * 
 * @author bernd.ruecker@camunda.com
 */
public class CreateHistoricalData {
  
  private Server hsqlServer;
  
  private Random r = new Random(10000);
  
  private long currentTime = 0;
  
  private class FakeDateGenerator implements DateGenerator {
    public Date getCurrentTime() {
      return new Date(currentTime);
    }    
  }

  public static void main(String[] args) throws Exception {
      CreateHistoricalData d = new CreateHistoricalData();
      d.runMySQL();          
  }
  
  public void runHSQL() {
    String[] options = new String[] { "-database.0 mydb", "-dbname.0", "xdb" };
    
    HsqlProperties hsqlproperties = HsqlProperties.argArrayToProps(
        options,
        "server");
    ServerConfiguration.translateDefaultDatabaseProperty(hsqlproperties);
    ServerConfiguration.translateDefaultNoSystemExitProperty(hsqlproperties);
    
    hsqlServer = new Server();
    hsqlServer.setProperties(hsqlproperties);

    hsqlServer.start();

    // do it!
    
    hsqlServer.stop();   
  }
  
  public void runMySQL() throws Exception {
    Clock.dateGenerator = new FakeDateGenerator();
    
    JbpmConfiguration configuration = JbpmConfiguration.getInstance("org/jbpm/sim/tutorial/jbpm.mysql.cfg.xml");
//    {
//      configuration.createSchema();
//    }
//    {
//      JbpmContext ctx = configuration.createJbpmContext();
//
//      ProcessDefinition pd = ProcessDefinition.parseXmlInputStream( this.getClass().getResourceAsStream("/org/jbpm/sim/tutorial/business/ReturnDefectiveGoods/processdefinition.xml") );
//      ctx.deployProcessDefinition(pd);
//
//      for (int i = 0; i < 30; i++) {
//        long time = currentTime;
//        runVariantQuickTestInFirstTwoWeeks(ctx, pd);
//        simulateRandomTime(time, 3);
//      }
//      for (int i = 0; i < 70; i++) {
//        long time = currentTime;
//        runVariantExtendedTestInFirstTwoWeeks(ctx, pd);
//        simulateRandomTime(time, 3);
//      }
//      for (int i = 0; i < 20; i++) {
//        long time = currentTime;
//        runVariantNoDefect(ctx, pd);
//        simulateRandomTime(time, 3);
//      }
//      for (int i = 0; i < 75; i++) {
//        long time = currentTime;
//        runVariantExtendedTest(ctx, pd);
//        simulateRandomTime(time, 3);
//      }
//      //commit this transaction and start a new one
//      ctx.close();
//    }
    {   
      JbpmContext ctx = configuration.createJbpmContext();
      GetSimulationInputCommand cmd = new GetSimulationInputCommand("ReturnDefectiveGoods");
      BamSimulationProposal result = (BamSimulationProposal) cmd.execute(ctx);
      ctx.close();

      BamSimulationProposal.print(result.createScenarioConfigurationXml());
      BamSimulationProposal.print(result);
    }
  }

  private void runVariantQuickTestInFirstTwoWeeks(JbpmContext ctx, ProcessDefinition pd) {
    ProcessInstance pi = pd.createProcessInstance();
    pi.getContextInstance().setVariable("decisionOne", "YES");
    pi.getRootToken().signal(); // start state
    simulateRandomTime(1);
    TaskInstance ti1 = (TaskInstance) pi.getTaskMgmtInstance().getTaskInstances().iterator().next();
    assertString(ti1.getTask().getName(), "transfer shipping costs");
    ti1.start("me");
    simulateRandomTime(5);
    ti1.end(); // task "transfer shipping costs"
    simulateRandomTime(1);
    assertString(pi.getRootToken().getNode().getName(), "wait for parcel");
    pi.getRootToken().signal(); // state "wait for parcel"
    simulateRandomTime(5);
    
    TaskInstance ti2 = (TaskInstance) pi.getTaskMgmtInstance().getUnfinishedTasks(pi.getRootToken()).iterator().next();
    assertString(ti2.getTask().getName(), "quick test");
    ti2.start("me");
    simulateRandomTime(5);
    ti2.end("defect approved"); // task "quick test"
    simulateRandomTime(1);

    TaskInstance ti3 = (TaskInstance) pi.getTaskMgmtInstance().getUnfinishedTasks(pi.getRootToken()).iterator().next();
    assertString(ti3.getTask().getName(), "refund");
    ti3.start("me");
    simulateRandomTime(5);
    ti3.end();

    assertTrue(pi.hasEnded());
    ctx.save(pi);
  } 

  private void runVariantExtendedTestInFirstTwoWeeks(JbpmContext ctx, ProcessDefinition pd) {
    ProcessInstance pi = pd.createProcessInstance();
    pi.getContextInstance().setVariable("decisionOne", "YES");
    pi.getRootToken().signal(); // start state
    simulateRandomTime(1);
    TaskInstance ti1 = (TaskInstance) pi.getTaskMgmtInstance().getUnfinishedTasks(pi.getRootToken()).iterator().next();
    ti1.start("me");
    simulateRandomTime(5);
    ti1.end(); // task "transfer shipping costs"
    simulateRandomTime(1);
    
    pi.getRootToken().signal(); // state "wait for parcel"
    simulateRandomTime(1);

    TaskInstance ti2 = (TaskInstance) pi.getTaskMgmtInstance().getUnfinishedTasks(pi.getRootToken()).iterator().next();
    assertString(ti2.getTask().getName(), "quick test");
    ti2.start("me");
    simulateRandomTime(5);
    ti2.end("no defect"); // task "quick test"
    simulateRandomTime(1);

    TaskInstance ti3 = (TaskInstance) pi.getTaskMgmtInstance().getUnfinishedTasks(pi.getRootToken()).iterator().next();
    assertString(ti3.getTask().getName(), "extended technical test");
    ti3.start("me");
    simulateRandomTime(25);
    ti3.end("defect approved"); // task "quick test"

    TaskInstance ti4 = (TaskInstance) pi.getTaskMgmtInstance().getUnfinishedTasks(pi.getRootToken()).iterator().next();
    assertString(ti4.getTask().getName(), "refund");
    ti4.start("me");
    simulateRandomTime(5);
    ti4.end();
    
    assertTrue(pi.hasEnded());
    ctx.save(pi);    
  }  

  private void runVariantExtendedTest(JbpmContext ctx, ProcessDefinition pd) {
    ProcessInstance pi = pd.createProcessInstance();
    pi.getContextInstance().setVariable("decisionOne", "NO");
    pi.getRootToken().signal(); // start state
    simulateRandomTime(1);
    pi.getRootToken().signal(); // state "wait for parcel"
    simulateRandomTime(1);

    TaskInstance ti2 = (TaskInstance) pi.getTaskMgmtInstance().getUnfinishedTasks(pi.getRootToken()).iterator().next();
    assertString(ti2.getTask().getName(), "quick test");
    ti2.start("me");
    simulateRandomTime(5);
    ti2.end("no defect"); 
    simulateRandomTime(1);

    TaskInstance ti3 = (TaskInstance) pi.getTaskMgmtInstance().getUnfinishedTasks(pi.getRootToken()).iterator().next();
    assertString(ti3.getTask().getName(), "extended technical test");
    ti3.start("me");
    simulateRandomTime(25);
    ti3.end("defect approved"); 

    TaskInstance ti4 = (TaskInstance) pi.getTaskMgmtInstance().getUnfinishedTasks(pi.getRootToken()).iterator().next();
    assertString(ti4.getTask().getName(), "refund");
    ti4.start("me");
    simulateRandomTime(5);
    ti4.end();
    
    assertTrue(pi.hasEnded());
    ctx.save(pi);    
  }  
  
  private void runVariantNoDefect(JbpmContext ctx, ProcessDefinition pd) {
    ProcessInstance pi = pd.createProcessInstance();
    pi.getContextInstance().setVariable("decisionOne", "NO");
    pi.getRootToken().signal(); // start state
    simulateRandomTime(1);
    
    pi.getRootToken().signal(); // state "wait for parcel"
    simulateRandomTime(1);

    TaskInstance ti2 = (TaskInstance) pi.getTaskMgmtInstance().getUnfinishedTasks(pi.getRootToken()).iterator().next();
    ti2.start();
    simulateRandomTime(5);
    ti2.end("no defect"); // task "quick test"

    TaskInstance ti3 = (TaskInstance) pi.getTaskMgmtInstance().getUnfinishedTasks(pi.getRootToken()).iterator().next();
    ti3.start();
    simulateRandomTime(25);
    ti3.end("no defect"); // task "extended technical test"

    TaskInstance ti4 = (TaskInstance) pi.getTaskMgmtInstance().getUnfinishedTasks(pi.getRootToken()).iterator().next();
    assertString(ti4.getTask().getName(), "send back goods");
    ti4.start();
    simulateRandomTime(10);
    ti4.end(); // task "send back goods"

    assertTrue(pi.hasEnded());
    ctx.save(pi);    
  }

  private void simulateRandomTime(int muliplier) {
      simulateRandomTime(currentTime, muliplier);
  }
  
  private void simulateRandomTime(long time, int muliplier) {
    // wait 1 minute times multiplier (max), and at least 1 second
    long result = (1000 + Math.round( r.nextFloat() * 60 * 1000 )) * muliplier;
    // what we get is a distribution [1 second | 60 seconds] * multiplier
    currentTime = time + result;
}  

  private void assertString(String one, String two) {
    if (!one.equals(two))
      throw new RuntimeException(one + " != " + two);
  }

  private void assertTrue(boolean check) {
    if (!check)
      throw new RuntimeException("assertion failed");
  }
}
