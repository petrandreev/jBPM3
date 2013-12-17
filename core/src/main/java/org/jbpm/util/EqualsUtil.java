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
package org.jbpm.util;

import org.hibernate.proxy.HibernateProxy;

/** @deprecated not in use anymore */
public class EqualsUtil {

  private EqualsUtil() {
    // hide default constructor to prevent instantiation
  }

  /**
   * hack to support comparing hibernate proxies against the real objects.
   * since it falls back to ==, clients don't need to override hashcode.
   *
   * @deprecated hack does not work
   * @see <a href="https://jira.jboss.org/jira/browse/JBPM-2489">JBPM-2489</a>
   */
  public static boolean equals(Object thisObject, Object otherObject) {
    return thisObject == otherObject
      || otherObject instanceof HibernateProxy
      && otherObject.equals(thisObject);
  }

}
