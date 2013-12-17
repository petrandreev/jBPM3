package org.jbpm.jbpm3416;

import java.util.ArrayList;
import java.util.List;

import org.jbpm.AbstractJbpmTestCase;
import org.jbpm.context.exe.ContextInstance;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;

/**
 * 2 threads concurrently call Token#signal() and ContextThread#setVariable() on the same process instance.
 * THREAD_NUM is the number of threads which concurrently execute the test. Each thread spawns 2 threads as described above.
 * LOOP_NUM is the number of test iterations. You may increase it to reproduce the Exception for sure.
 * 
 * @see <a href="https://jira.jboss.org/jira/browse/JBPM-3416">JBPM-3416</a>
 */
public class JBPM3416Test extends AbstractJbpmTestCase {

  private static final int LOOP_NUM = 100;
  private static final int THREAD_NUM = 1000;

  private static List exceptionList = new ArrayList();

  public boolean doNotRunUnlessManually = true;
  
  public void testConcurrentAccessToLoggingInstance() {
    if( doNotRunUnlessManually ) { 
      return;
    }

    ProcessDefinition processDefinition = ProcessDefinition
        .parseXmlString(
              "<process-definition>"
            + "  <start-state name='start'>"
            + "    <transition to='s' />"
            + "  </start-state>"
            + "  <state name='s'>"
            + "    <transition to='end' />"
            + "  </state>"
            + "  <end-state name='end' />"
            + "</process-definition>");

    for (int i = 0; i < LOOP_NUM; i++) {
      ArrayList threads = new ArrayList();
      for (int idx = 0; idx < THREAD_NUM; idx++) {
        threads.add(new JobThread(processDefinition, idx));
      }

      for (int idx = 0; idx < THREAD_NUM; idx++) {
        ((Thread) threads.get(idx)).start();
      }

      for (int idx = 0; idx < THREAD_NUM; idx++) {
        try {
          ((Thread) threads.get(idx)).join();
        } catch (InterruptedException e) {
          // ignore
        }
      }
      if (exceptionList.size() > 0) {
        ((RuntimeException) exceptionList.get(exceptionList.size()-1)).printStackTrace();
        fail("[" + i + "] Found RuntimeException : exceptionList.size() = "
            + exceptionList.size() + ", exceptionList.get(0) = "
            + exceptionList.get(0));
      }
    }


  }

  private class JobThread extends Thread {
    ProcessDefinition processDefinition = null;
    int id = -1;

    public JobThread(ProcessDefinition processDefinition, int id) {
      this.processDefinition = processDefinition;
      this.id = id;
    }

    public void run() {
      ProcessInstance processInstance = new ProcessInstance(processDefinition);

      TokenThread tokenThread = new TokenThread(processInstance);
      ContextThread contextThread = new ContextThread(processInstance);
      tokenThread.start();
      contextThread.start();
      try {
        tokenThread.join();
      } catch (InterruptedException e) {
        // ignore;
      }
      try {
        contextThread.join();
      } catch (InterruptedException e) {
        // ignore
      }
      
      this.processDefinition = null;
    }
  }

  /*
   * Thread for Token#signal
   */
  private class TokenThread extends Thread {
    ProcessInstance processInstance = null;

    public TokenThread(ProcessInstance processInstance) {
      this.processInstance = processInstance;
    }

    public void run() {
      Token token = processInstance.getRootToken();
      try {
        token.signal();
        token.signal();
      } catch (RuntimeException e) {
        exceptionList.add(e);
      }
      this.processInstance = null;
    }
  }

  /*
   * Thread for ContextThread#setVariable
   */
  private class ContextThread extends Thread {
    ProcessInstance processInstance = null;

    public ContextThread(ProcessInstance processInstance ) {
      this.processInstance = processInstance;
    }

    public void run() {

      ContextInstance contextInstance = processInstance.getContextInstance();
      try {
        contextInstance.setVariable("RESULT", "OK");
      } catch (RuntimeException e) {
        exceptionList.add(e);
      }
      this.processInstance = null;

    }
  }
}
