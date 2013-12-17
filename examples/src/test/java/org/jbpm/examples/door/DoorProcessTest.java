package org.jbpm.examples.door;

import org.jbpm.AbstractJbpmTestCase;
import org.jbpm.JbpmException;
import org.jbpm.graph.def.Node;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;

public class DoorProcessTest extends AbstractJbpmTestCase {
	/**
	 * The doorProcess is a reference to the single ProcessDefinition that will be used for all
	 * executions in all the tests of this test case.
	 * 
	 * In this case, we'll just store the process definition object in memory in a static member
	 * field. In practice, most often process definitions will be deployed to a database.
	 */
	static ProcessDefinition doorProcess;
	static Node locked, closed, open, openLocked;

	static {
		doorProcess = ProcessDefinition.parseXmlInputStream(DoorProcessTest.class.getResourceAsStream("/door/processdefinition.xml"));
		locked = doorProcess.getNode("Locked");
		closed = doorProcess.getNode("Closed");
		open = doorProcess.getNode("Open");
		openLocked = doorProcess.getNode("Open Locked");
	}

	/**
	 * This test shows how you can execute one scenario in a test method. Inside the test method, the
	 * external triggers (=signals) are provided to a process instance. Then you assert wether the
	 * process instance ends up in the expected state.
	 */
	public void testScenarioOne() {
		ProcessInstance processInstance = new ProcessInstance(doorProcess);
		Token token = processInstance.getRootToken();
		assertEquals("Closed", token.getNode().getName());
		token.signal("open");
		assertEquals("Open", token.getNode().getName());
		token.signal("close");
		assertEquals("Closed", token.getNode().getName());
		try {
			token.signal("unlock");
			fail("excepted exception");
		}
		catch (RuntimeException e) {
		}
	}

	// Below are all the simple state-change tests.
	// Note that you can actually put a token into a given state with
	// the setNode method.

	public void testClosedOpen() {
		ProcessInstance processInstance = new ProcessInstance(doorProcess);
		Token token = processInstance.getRootToken();
		token.signal("open");
		assertEquals("Open", token.getNode().getName());
	}

	public void testClosedLock() {
		ProcessInstance processInstance = new ProcessInstance(doorProcess);
		Token token = processInstance.getRootToken();
		token.signal("lock");
		assertEquals("Locked", token.getNode().getName());
	}

	public void testClosedClose() {
		ProcessInstance processInstance = new ProcessInstance(doorProcess);
		Token token = processInstance.getRootToken();
		try {
			token.signal("close");
			fail("expected exception");
		}
		catch (JbpmException e) {
		}
	}

	public void testClosedUnlock() {
		ProcessInstance processInstance = new ProcessInstance(doorProcess);
		Token token = processInstance.getRootToken();
		try {
			token.signal("unlock");
			fail("expected exception");
		}
		catch (JbpmException e) {
		}
	}

	public void testOpenedOpen() {
		ProcessInstance processInstance = new ProcessInstance(doorProcess);
		Token token = processInstance.getRootToken();
		token.setNode(open);
		try {
			token.signal("open");
			fail("expected exception");
		}
		catch (JbpmException e) {
		}
	}

	public void testOpenedLock() {
		ProcessInstance processInstance = new ProcessInstance(doorProcess);
		Token token = processInstance.getRootToken();
		token.setNode(open);
		token.signal("lock");
		assertEquals("Open Locked", token.getNode().getName());
	}

	public void testOpenedClose() {
		ProcessInstance processInstance = new ProcessInstance(doorProcess);
		Token token = processInstance.getRootToken();
		token.setNode(open);
		token.signal("close");
		assertEquals("Closed", token.getNode().getName());
	}

	public void testOpenedUnlock() {
		ProcessInstance processInstance = new ProcessInstance(doorProcess);
		Token token = processInstance.getRootToken();
		token.setNode(open);
		try {
			token.signal("unlock");
			fail("expected exception");
		}
		catch (JbpmException e) {
		}
	}

	public void testLockedOpen() {
		ProcessInstance processInstance = new ProcessInstance(doorProcess);
		Token token = processInstance.getRootToken();
		token.setNode(locked);
		try {
			token.signal("open");
			fail("expected exception");
		}
		catch (JbpmException e) {
		}
	}

	public void testLockedLock() {
		ProcessInstance processInstance = new ProcessInstance(doorProcess);
		Token token = processInstance.getRootToken();
		token.setNode(locked);
		try {
			token.signal("lock");
			fail("expected exception");
		}
		catch (JbpmException e) {
		}
	}

	public void testLockedClose() {
		ProcessInstance processInstance = new ProcessInstance(doorProcess);
		Token token = processInstance.getRootToken();
		token.setNode(locked);
		try {
			token.signal("close");
			fail("expected exception");
		}
		catch (JbpmException e) {
		}
	}

	public void testLockedUnlock() {
		ProcessInstance processInstance = new ProcessInstance(doorProcess);
		Token token = processInstance.getRootToken();
		token.setNode(locked);
		token.signal("unlock");
		assertEquals("Closed", token.getNode().getName());
	}

	public void testOpenLockedOpen() {
		ProcessInstance processInstance = new ProcessInstance(doorProcess);
		Token token = processInstance.getRootToken();
		token.setNode(openLocked);
		try {
			token.signal("open");
			fail("expected exception");
		}
		catch (JbpmException e) {
		}
	}

	public void testOpenLockedLock() {
		ProcessInstance processInstance = new ProcessInstance(doorProcess);
		Token token = processInstance.getRootToken();
		token.setNode(openLocked);
		try {
			token.signal("lock");
			fail("expected exception");
		}
		catch (JbpmException e) {
		}
	}

	public void testOpenLockedClose() {
		ProcessInstance processInstance = new ProcessInstance(doorProcess);
		Token token = processInstance.getRootToken();
		token.setNode(openLocked);
		try {
			token.signal("close");
			fail("expected exception");
		}
		catch (JbpmException e) {
		}
	}

	public void testOpenLockedUnlock() {
		ProcessInstance processInstance = new ProcessInstance(doorProcess);
		Token token = processInstance.getRootToken();
		token.setNode(openLocked);
		token.signal("unlock");
		assertEquals("Open", token.getNode().getName());
	}
}
