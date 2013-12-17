package org.jbpm.examples.door;

/**
 * this shows a plain Java coding approach to implement the behaviour
 * of a door.
 */
public class Door {

  static String OPEN = "open";
  static String CLOSED = "closed";
  static String LOCKED = "locked";
  static String OPEN_LOCKED = "open-locked";
  
  String state = CLOSED;
  
  public void lock() {
    if ( (state==LOCKED) || (state==OPEN_LOCKED) ) {
      throw new IllegalStateException("door is already locked");
    }
    if (state==CLOSED) {
      state = LOCKED;
    } else if (state==OPEN) {
      state = OPEN_LOCKED;
    }
  }

  public void unlock() {
    if ( (state==OPEN) || (state==CLOSED) ) {
      throw new IllegalStateException("door is already unlocked");
    }
    if (state==LOCKED) {
      state = CLOSED;
    } else if (state==OPEN_LOCKED) {
      state = OPEN;
    }
  }

  public void close() {
    if (state==OPEN_LOCKED) {
      throw new IllegalStateException("can't close a locked door");
    }
    if (state!=OPEN) {
      throw new IllegalStateException("door is already closed");
    }
    state = CLOSED;
  }

  public void open() {
    if (state==LOCKED) {
      throw new IllegalStateException("can't open a locked door");
    }
    if (state!=CLOSED) {
      throw new IllegalStateException("door is already open");
    }
    state = OPEN;
  }
}
