package org.jbpm.examples.door;

import junit.framework.TestCase;

/**
 * This example shows the tradeoff between implementing the behaviour of a door in plain Java and
 * using a jPDL process. This is intended for developers to learn about the aspects that are handled
 * better in a process versus plain programming.
 */
public class DoorTest extends TestCase {

	public void testClosedOpen() {
		Door door = new Door();
		door.open();
		assertSame(Door.OPEN, door.state);
	}

	public void testClosedLock() {
		Door door = new Door();
		door.lock();
		assertSame(Door.LOCKED, door.state);
	}

	public void testClosedClose() {
		Door door = new Door();
		try {
			door.close();
			fail("expected exception");
		}
		catch (IllegalStateException e) {
		}
	}

	public void testClosedUnlock() {
		Door door = new Door();
		try {
			door.unlock();
			fail("expected exception");
		}
		catch (IllegalStateException e) {
		}
	}

	public void testOpenedOpen() {
		Door door = new Door();
		door.state = Door.OPEN;
		try {
			door.open();
			fail("expected exception");
		}
		catch (IllegalStateException e) {
		}
	}

	public void testOpenedLock() {
		Door door = new Door();
		door.state = Door.OPEN;
		door.lock();
		assertSame(Door.OPEN_LOCKED, door.state);
	}

	public void testOpenedClose() {
		Door door = new Door();
		door.state = Door.OPEN;
		door.close();
		assertSame(Door.CLOSED, door.state);
	}

	public void testOpenedUnlock() {
		Door door = new Door();
		door.state = Door.OPEN;
		try {
			door.unlock();
			fail("expected exception");
		}
		catch (IllegalStateException e) {
		}
	}

	public void testLockedOpen() {
		Door door = new Door();
		door.state = Door.LOCKED;
		try {
			door.open();
			fail("expected exception");
		}
		catch (IllegalStateException e) {
		}
	}

	public void testLockedLock() {
		Door door = new Door();
		door.state = Door.LOCKED;
		try {
			door.lock();
			fail("expected exception");
		}
		catch (IllegalStateException e) {
		}
	}

	public void testLockedClose() {
		Door door = new Door();
		door.state = Door.LOCKED;
		try {
			door.close();
			fail("expected exception");
		}
		catch (IllegalStateException e) {
		}
	}

	public void testLockedUnlock() {
		Door door = new Door();
		door.state = Door.LOCKED;
		door.unlock();
		assertSame(Door.CLOSED, door.state);
	}

	public void testOpenLockedOpen() {
		Door door = new Door();
		door.state = Door.OPEN_LOCKED;
		try {
			door.open();
			fail("expected exception");
		}
		catch (IllegalStateException e) {
		}
	}

	public void testOpenLockedLock() {
		Door door = new Door();
		door.state = Door.OPEN_LOCKED;
		try {
			door.lock();
			fail("expected exception");
		}
		catch (IllegalStateException e) {
		}
	}

	public void testOpenLockedClose() {
		Door door = new Door();
		door.state = Door.OPEN_LOCKED;
		try {
			door.close();
			fail("expected exception");
		}
		catch (IllegalStateException e) {
		}
	}

	public void testOpenLockedUnlock() {
		Door door = new Door();
		door.state = Door.OPEN_LOCKED;
		door.unlock();
		assertSame(Door.OPEN, door.state);
	}
}
