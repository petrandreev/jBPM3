package org.jbpm.optimisticlocking;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Semaphore;

import org.hibernate.Query;
import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.Comment;
import org.jbpm.graph.exe.ProcessInstance;

/**
 * Possible problem in concurrent signalling from multiple threads
 * 
 * https://jira.jboss.org/jira/browse/JBPM-1071
 */
public class LockingTest extends AbstractDbTestCase {

  private long processDefinitionId;

  static final int nbrOfThreads = 5;
  static final int nbrOfIterations = 20;

  protected void setUp() throws Exception {
    super.setUp();
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlString("<process-definition name='lockprocess'>"
        + "  <start-state name='start'>"
        + "    <transition to='end'/>"
        + "  </start-state>"
        + "  <end-state name='end' />"
        + "</process-definition>");
    jbpmContext.deployProcessDefinition(processDefinition);
    newTransaction();
    processDefinitionId = processDefinition.getId();
  }

  protected void tearDown() throws Exception {
    graphSession.deleteProcessDefinition(processDefinitionId);
    super.tearDown();
  }

  public void testLocking() {
    // the process will be executed in 2 separate transactions:
    // Transaction 1 will create the process instance and position
    // the root token in the start state
    // Transaction 2 will signal the process instance while it is in the
    // start state, and that signal will bring the process to it's end state.
    // multiple competing threads will be set up for the second transaction

    ProcessDefinition processDefinition = graphSession.loadProcessDefinition(processDefinitionId);
    for (int i = 0; i < nbrOfIterations; i++) {
      ProcessInstance processInstance = new ProcessInstance(processDefinition);
      jbpmContext.save(processInstance);

      newTransaction();

      // create a bunch of threads that will all wait on the
      // semaphore before they will try to signal the same process instance
      long processInstanceId = processInstance.getId();
      Semaphore semaphore = new Semaphore(0);
      List threads = startThreads(semaphore, processInstanceId);

      // release all the threads
      semaphore.release(nbrOfThreads);

      // wait for all threads to finish
      joinAllThreads(threads);

      // check that only 1 of those threads committed
      Query query = session.createQuery("from org.jbpm.graph.exe.Comment");
      List results = query.list();
      assertEquals(results.toString(), 1, results.size());

      // delete the comment
      session.delete(results.get(0));

      // check that the process instance has ended
      processInstance = jbpmContext.loadProcessInstance(processInstanceId);
      assertTrue(processInstance.hasEnded());
    }
  }

  private List startThreads(Semaphore semaphore, long processInstanceId) {
    JbpmConfiguration jbpmConfiguration = getJbpmConfiguration();

    List threads = new ArrayList();
    for (int i = 0; i < nbrOfThreads; i++) {
      Thread thread = new LockThread(jbpmConfiguration, semaphore, processInstanceId);
      threads.add(thread);
      thread.start();
    }
    return threads;
  }

  private void joinAllThreads(List threads) {
    Iterator iter = threads.iterator();
    while (iter.hasNext()) {
      Thread thread = (Thread) iter.next();
      try {
        thread.join(10000);
      }
      catch (InterruptedException e) {
        fail("join interrupted");
      }
    }
  }

  static class LockThread extends Thread {

    Semaphore semaphore;
    long processInstanceId;
    JbpmConfiguration jbpmConfiguration;

    public LockThread(JbpmConfiguration jbpmConfiguration, Semaphore semaphore, long processInstanceId) {
      this.semaphore = semaphore;
      this.processInstanceId = processInstanceId;
      this.jbpmConfiguration = jbpmConfiguration;
    }

    public void run() {
      try {
        // first wait until the all threads are released at once in the
        // method testLocking
        semaphore.acquire();

        // after a thread is released (=notified), it will try to load the
        // process instance,
        // signal it and then commit the transaction
        JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();
        try {
          ProcessInstance processInstance = jbpmContext.loadProcessInstance(processInstanceId);
          processInstance.signal();
          jbpmContext.save(processInstance);

          // add a comment in the same transaction so that we can see which
          // thread won
          Comment comment = new Comment(getName() + " committed");
          jbpmContext.getSession().save(comment);
        }
        catch (RuntimeException e) {
          jbpmContext.setRollbackOnly();
        }
        finally {
          jbpmContext.close();
        }
      }
      catch (InterruptedException e) {
        fail("semaphore waiting got interrupted");
      }
      catch (RuntimeException e) {
        // ignore other exceptions
      }
    }
  }
}
