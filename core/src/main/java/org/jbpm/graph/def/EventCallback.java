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
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.transaction.Status;
import javax.transaction.Synchronization;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.jbpm.util.Semaphore;

public class EventCallback implements Serializable {

  public static final int DEFAULT_TIMEOUT = 5 * 60 * 1000;

  private static final long serialVersionUID = 1L;
  static final Log log = LogFactory.getLog(EventCallback.class);

  private static Map eventSemaphores = new HashMap();

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
          if (log.isDebugEnabled()) log.debug("sending '" + event + "' notification");
          Semaphore eventSemaphore = getEventSemaphore(event);
          eventSemaphore.release();
        }
      }
    };
    JbpmContext.getCurrentJbpmContext()
      .getSession()
      .getTransaction()
      .registerSynchronization(notification);
  }

  public static void waitForEvent(String event) {
    waitForEvent(event, DEFAULT_TIMEOUT);
  }

  public static void waitForEvent(String event, long timeout) {
    waitForEvent(1, event, timeout);
  }

  public static void waitForEvent(int occurrences, String event) {
    waitForEvent(occurrences, event, DEFAULT_TIMEOUT);
  }

  public static void waitForEvent(int occurrences, String event, long timeout) {
    boolean debug = log.isDebugEnabled();
    if (debug) log.debug("waiting for " + event);
    Semaphore eventSemaphore = getEventSemaphore(event);
    try {
      if (!eventSemaphore.tryAcquire(occurrences, timeout)) {
        throw new JbpmException("event '" + event + "' did not occur within " + timeout + " ms");
      }
      if (debug) log.debug("received '" + event + "' notification");
    }
    catch (InterruptedException e) {
      throw new JbpmException("wait for event '" + event + "' was interrupted", e);
    }
  }

  static Semaphore getEventSemaphore(String event) {
    synchronized (eventSemaphores) {
      Semaphore semaphore = (Semaphore) eventSemaphores.get(event);
      if (semaphore == null) {
        // request fail semaphore to support atomic multi-acquire
        semaphore = new Semaphore(0);
        eventSemaphores.put(event, semaphore);
      }
      return semaphore;
    }
  }

  public static void clear() {
    for (Iterator i = eventSemaphores.entrySet().iterator(); i.hasNext();) {
      Map.Entry entry = (Entry) i.next();
      Semaphore semaphore = (Semaphore) entry.getValue();
      int permits = semaphore.drainPermits();
      if (permits != 0) {
        log.warn("event '" + entry.getKey() + "' has " + permits + " outstanding notifications");
      }
    }
  }
}