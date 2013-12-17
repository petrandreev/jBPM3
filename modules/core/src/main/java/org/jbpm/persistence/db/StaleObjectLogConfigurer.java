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
package org.jbpm.persistence.db;

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class StaleObjectLogConfigurer {

  private static final Log log = LogFactory.getLog(StaleObjectLogConfigurer.class);
  private static Log staleObjectExceptionsLog = LogFactory.getLog(StaleObjectLogConfigurer.class.getName()+".staleObjectExceptions");

  private StaleObjectLogConfigurer() {
    // hide default constructor to prevent instantiation
  }

  public static Log getStaleObjectExceptionsLog() {
    return staleObjectExceptionsLog;
  }

  public static void hideStaleObjectExceptions() {
    if (staleObjectExceptionsLog instanceof LogWrapper) {
      log.debug("stale object exceptions are already hidden from logging");
      return;
    }

    try {
      staleObjectExceptionsLog = new LogWrapper(staleObjectExceptionsLog);
      log.info("stale object exceptions will be hidden from logging");
    } catch (Exception e) {
      log.info("couldn't hide stale object exceptions from logging");
    }
  }

  static class LogWrapper implements Log, Serializable {
    Log delegate;
    private static final long serialVersionUID = 1L;
    LogWrapper(Log delegate) {
      this.delegate = delegate;
    }
    public void debug(Object arg0, Throwable arg1) {
    }
    public void debug(Object arg0) {
    }
    public void error(Object arg0, Throwable arg1) {
    }
    public void error(Object arg0) {
    }
    public void info(Object arg0, Throwable arg1) {
    }
    public void info(Object arg0) {
    }
    public void trace(Object arg0, Throwable arg1) {
    }
    public void trace(Object arg0) {
    }
    public void warn(Object arg0, Throwable arg1) {
    }
    public void warn(Object arg0) {
    }
    public void fatal(Object arg0, Throwable arg1) {
      delegate.fatal(arg0, arg1);
    }
    public void fatal(Object arg0) {
      delegate.fatal(arg0);
    }
    public boolean isDebugEnabled() {
      return delegate.isDebugEnabled();
    }
    public boolean isErrorEnabled() {
      return false;
    }
    public boolean isFatalEnabled() {
      return delegate.isFatalEnabled();
    }
    public boolean isInfoEnabled() {
      return false;
    }
    public boolean isTraceEnabled() {
      return false;
    }
    public boolean isWarnEnabled() {
      return false;
    }
  }
}
