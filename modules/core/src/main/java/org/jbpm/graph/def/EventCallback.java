/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jbpm.graph.def;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.transaction.Status;
import javax.transaction.Synchronization;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;

public class EventCallback implements Serializable {

  public static final int DEFAULT_TIMEOUT = 5 * 60 * 1000;

  private static final long serialVersionUID = 1L;
  private static final Log log = LogFactory.getLog(EventCallback.class);

  private static final Map<String, Semaphore> eventSemaphores = new HashMap<String, Semaphore>();

  public void processStart() {
    registerNotification(Event.EVENTTYPE_PROCESS_START);
  }

  public void processEnd() {
    registerNotification(Event.EVENTTYPE_PROCESS_END);
  }

  public void nodeEnter() {
    registerNotification(Event.EVENTTYPE_NODE_ENTER);
  }

  public void nodeLeave() {
    registerNotification(Event.EVENTTYPE_NODE_LEAVE);
  }

  public void taskCreate() {
    registerNotification(Event.EVENTTYPE_TASK_CREATE);
  }

  public void taskEnd() {
    registerNotification(Event.EVENTTYPE_TASK_END);
  }

  public void timerCreate() {
    registerNotification(Event.EVENTTYPE_TIMER_CREATE);
  }

  public void timer() {
    registerNotification(Event.EVENTTYPE_TIMER);
  }

  public void transition() {
    registerNotification(Event.EVENTTYPE_TRANSITION);
  }

  private static void registerNotification(final String event) {
    Synchronization notification = new Synchronization() {

      public void beforeCompletion() {
      }

      public void afterCompletion(int status) {
        if (status == Status.STATUS_COMMITTED) {
          log.debug("sending '" + event + "' notification");
          Semaphore eventSemaphore = getEventSemaphore(event);
          eventSemaphore.release();
        }
        else {
          log.warn("not sending '" + event + "' notification, transaction is " + statusToString(status));
        }
      }
    };
    JbpmContext.getCurrentJbpmContext()
        .getSession()
        .getTransaction()
        .registerSynchronization(notification);
  }

  private static String statusToString(int status) {
    String text;
    switch (status) {
    case Status.STATUS_ACTIVE:
      text = "active";
      break;
    case Status.STATUS_COMMITTED:
      text = "committed";
      break;
    case Status.STATUS_COMMITTING:
      text = "committing";
      break;
    case Status.STATUS_MARKED_ROLLBACK:
      text = "marked for rollback";
      break;
    case Status.STATUS_NO_TRANSACTION:
      text = "absent";
      break;
    case Status.STATUS_PREPARED:
      text = "prepared";
      break;
    case Status.STATUS_PREPARING:
      text = "preparing";
      break;
    case Status.STATUS_ROLLEDBACK:
      text = "rolled back";
      break;
    case Status.STATUS_ROLLING_BACK:
      text = "rolling back";
      break;
    case Status.STATUS_UNKNOWN:
    default:
      text = "in unknown status";
      break;
    }
    return text;
  }

  public static void waitForEvent(String event) {
    waitForEvent(1, event, DEFAULT_TIMEOUT);
  }

  public static void waitForEvent(String event, long timeout) {
    waitForEvent(1, event, timeout);
  }

  public static void waitForEvent(int occurrences, String event) {
    waitForEvent(occurrences, event, DEFAULT_TIMEOUT);
  }

  public static void waitForEvent(int occurrences, String event, long timeout) {
    log.debug("waiting for " + event);
    Semaphore eventSemaphore = getEventSemaphore(event);
    try {
      if (eventSemaphore.tryAcquire(occurrences, timeout, TimeUnit.MILLISECONDS)) {
        log.debug("received '" + event + "' notification");
      }
      else {
        throw new JbpmException("event '" + event + "' did not occur within " + timeout + " ms");
      }
    }
    catch (InterruptedException e) {
      throw new JbpmException("wait for event '" + event + "' was interrupted", e);
    }
  }

  private static Semaphore getEventSemaphore(String event) {
    synchronized (eventSemaphores) {
      Semaphore semaphore = eventSemaphores.get(event);
      if (semaphore == null) {
        semaphore = new Semaphore(0);
        eventSemaphores.put(event, semaphore);
      }
      return semaphore;
    }
  }

  public static void clear() {
    synchronized (eventSemaphores) {
      for (Map.Entry<String, Semaphore> entry : eventSemaphores.entrySet()) {
        int permits = entry.getValue().drainPermits();
        if (permits != 0) {
          log.warn("event '" + entry.getKey() + "' has " + permits + " outstanding notifications");
        }
      }
      eventSemaphores.clear();
    }
  }
}
