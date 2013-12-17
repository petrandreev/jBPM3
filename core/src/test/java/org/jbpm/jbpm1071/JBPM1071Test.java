package org.jbpm.jbpm1071;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.def.Node.NodeType;
import org.jbpm.graph.exe.Comment;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;

import edu.emory.mathcs.backport.java.util.concurrent.Semaphore;

/**
 * Possible problem in concurrent signaling from multiple threads.
 * 
 * @see <a href='https://jira.jboss.org/jira/browse/JBPM-1071'>JBPM-1071</a>
 */
public class JBPM1071Test extends AbstractDbTestCase {

  static final int nbrOfThreads = 4;
  static final int nbrOfIterations = 10;

  protected void setUp() throws Exception {
    super.setUp();
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition name='jbpm1071'>"
      + "  <start-state name='start'>"
      + "    <transition to='end'/>"
      + "  </start-state>"
      + "  <end-state name='end' />"
      + "</process-definition>");
    deployProcessDefinition(processDefinition);
  }

  public void testLocking() {
    // the process will be executed in 2 separate transactions:
    // Transaction 1 will create the process instance and position
    // the root token in the start state
    // Transaction 2 will signal the process instance while it is in the
    // start state, and that signal will bring the process to it's end state.
    // multiple competing threads will be set up for the second transaction
    for (int i = 0; i < nbrOfIterations; i++) {
      long processInstanceId = jbpmContext.newProcessInstanceForUpdate("jbpm1071").getId();
      newTransaction();

      // create a bunch of threads that will all wait on the
      // semaphore before they will try to signal the same process instance
      Semaphore semaphore = new Semaphore(0);
      List threads = startThreads(semaphore, processInstanceId);

      // release all the threads
      semaphore.release(nbrOfThreads);

      // wait for all threads to finish
      joinAllThreads(threads);

      // check that only 1 of those threads committed
      List results = session.createCriteria(Comment.class).list();
      assertEquals(results.toString(), 1, results.size());

      // delete the comment
      session.delete(results.get(0));

      // check that the process instance has ended
      ProcessInstance processInstance = jbpmContext.loadProcessInstance(processInstanceId);
      assertTrue("expected " + processInstance + " to have ended", processInstance.hasEnded());
    }
  }

  private List startThreads(Semaphore semaphore, long processInstanceId) {
    List threads = new ArrayList();
    for (int i = 0; i < nbrOfThreads; i++) {
      Thread thread = new Thread(new Signaller(semaphore, jbpmConfiguration, processInstanceId));
      thread.start();
      threads.add(thread);
    }
    return threads;
  }

  private void joinAllThreads(List threads) {
    for (Iterator i = threads.iterator(); i.hasNext();) {
      Thread thread = (Thread) i.next();
      try {
        thread.join(10000);
      }
      catch (InterruptedException e) {
        fail("join interrupted");
      }
    }
  }

  static class Signaller implements Runnable {

    private final Semaphore semaphore;
    private final JbpmConfiguration jbpmConfiguration;
    private final long processInstanceId;

    Signaller(Semaphore semaphore, JbpmConfiguration jbpmConfiguration, long processInstanceId) {
      this.semaphore = semaphore;
      this.jbpmConfiguration = jbpmConfiguration;
      this.processInstanceId = processInstanceId;
    }

    public void run() {
      try {
        // first wait until the all threads are released at once in the
        // method testLocking
        semaphore.acquire();
      }
      catch (InterruptedException e) {
        fail("semaphore waiting got interrupted");
      }

      // after a thread is released (=notified), it will try to load the process
      // instance,
      // signal it and then commit the transaction
      String threadName = Thread.currentThread().getName();
      Log log = LogFactory.getLog(JBPM1071Test.class);
      JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();
      try {
        ProcessInstance processInstance = jbpmContext.loadProcessInstance(processInstanceId);
        Token rootToken = processInstance.getRootToken();

        // check whether root token is still in start state
        if (rootToken.getNode().getNodeType() != NodeType.StartState) {
          jbpmContext.setRollbackOnly();
          return;
        }

        // move to end state
        processInstance.signal();

        // add a comment to see which thread won
        Comment comment = new Comment(threadName + " committed");
        rootToken.addComment(comment);

        jbpmContext.save(processInstance);
      }
      catch (RuntimeException e) {
        jbpmContext.setRollbackOnly();
        log.debug(threadName + " rolled back", e);
      }
      finally {
        try {
          jbpmContext.close();
        }
        catch (RuntimeException e) {
          log.debug(threadName + " failed to close jbpm context", e);
        }
      }
    }
  }
}
