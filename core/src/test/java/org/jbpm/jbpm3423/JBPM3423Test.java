package org.jbpm.jbpm3423;

import java.util.Timer;
import java.util.TimerTask;

import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.ProcessDefinition;

public class JBPM3423Test extends AbstractDbTestCase {

  private static final String PROCESS_DEFINITION_MULTI_NODE = 
      "<process-definition  xmlns=''  name='SimpleProcess'>" + 
      "  <start-state name='start-state1'>" + 
      "    <transition to='SAME'></transition>" + 
      "  </start-state>" + 
      "  <node name='SAME'>" + 
      "    <action class='com.sample.action.MessageActionHandler' name='Node1Action'>" + 
      "      <message>NODE-1</message>" + 
      "    </action>" + 
      "    <transition to='node'></transition>" + 
      "  </node>" + 
      "    <!--  Because this is ALSO 'SAME', it can cause recursion in the GraphElement.equals(...) method  -->" + 
      "  <node name='SAME'>" + 
      "    <action class='com.sample.action.MessageActionHandler' name='Node2Action'>" + 
      "      <message>NODE-2</message>" + 
      "    </action>" + 
      "    <transition to='end-state1'></transition>" + 
      "  </node>" + 
      "  <end-state name='end-state1'></end-state>" + 
      "</process-definition>";
      

  private boolean stackOverflow = false;
  private boolean exceptionWasThrown = false;
  private boolean threadHadToBeKilled = false;
  
  public void testNoRecursivenessInParsingNodesWithDuplicateNames() throws Exception { 
    // Thread to deploy process that might stack overflow
    final Thread deployThread = new Thread() { 
      public void run() { 
        try { 
          ProcessDefinition.parseXmlString(PROCESS_DEFINITION_MULTI_NODE);
        }
        catch( Throwable t ) { 
          if( t instanceof StackOverflowError ) { 
            stackOverflow = true;
          }
          if( t.getMessage() != null && t.getMessage().contains("contains two nodes") ) { 
            exceptionWasThrown = true;
          }
        }
      }
    };
    
    // Timer task to stop above thread if necessary.
    TimerTask stopThreadTask = new TimerTask() {
      private Thread threadToStop = deployThread;
      public void run() {
        if( threadToStop.isAlive() ) { 
          threadToStop.stop();
        }
      }
    };
    
    // Setup
    Timer stopThreadTimer = new Timer();
    long delay = 2*1000;
    
    // Start deploy
    deployThread.start();
    
    // Kill thread if necessary after delay
    stopThreadTimer.schedule(stopThreadTask, delay);
    Thread.sleep(delay + 100);
    
    assertFalse("Stack overflow occurred!", stackOverflow);
    assertFalse("Deploy thread had to be killed!", threadHadToBeKilled);
    assertTrue("No exception was thrown during deployment.", exceptionWasThrown);
  }
  
  
}
