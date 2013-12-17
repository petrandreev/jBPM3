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
package org.jbpm.context.exe.variableinstance;

import org.jbpm.context.exe.VariableInstance;
import org.jbpm.context.exe.matcher.Ejb3Matcher;

public class Ejb3Instance extends VariableInstance {

  private static final long serialVersionUID = 1L;

  static Ejb3Matcher ejb3Matcher = new Ejb3Matcher();
  
  // use whatever member variables to persist 
  // the object id of the ejb3 object.
  // note that the object id member variables 
  // have to be persistable with hibernate
  protected Long id = null;
  protected String className = null;

  public boolean isStorable(Object value) {
    if (value==null) return true;
   return ejb3Matcher.matches(value.getClass());
  }

  protected Object getObject() {
    // TODO get the object from the stored identity in  
    // the member fields
    return null;
  }

  protected void setObject(Object value) {
    // TODO store the object identity into the member 
    // fields for persistence
  }
}
