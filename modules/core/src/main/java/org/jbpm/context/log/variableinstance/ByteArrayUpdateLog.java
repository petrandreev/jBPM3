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
package org.jbpm.context.log.variableinstance;

import org.jbpm.bytes.ByteArray;
import org.jbpm.context.exe.VariableInstance;
import org.jbpm.context.log.VariableUpdateLog;

public class ByteArrayUpdateLog extends VariableUpdateLog {
  
  private static final long serialVersionUID = 1L;
  
  ByteArray oldValue = null;
  ByteArray newValue = null;

  public ByteArrayUpdateLog() {
  }

  public ByteArrayUpdateLog(VariableInstance variableInstance, ByteArray oldValue, ByteArray newValue) {
    super(variableInstance);
    this.oldValue = (oldValue!=null ? new ByteArray(oldValue) : null );
    this.newValue = (newValue!=null ? new ByteArray(newValue) : null );
  }

  public Object getOldValue() {
    return oldValue;
  }

  public Object getNewValue() {
    return newValue;
  }

  public String toString() {
    String toString = null;
    if ( (oldValue==null)
         && (newValue==null) ) {
      toString = variableInstance+" remained null";
    } else if ( (oldValue!=null)
                && (oldValue.equals(newValue) )
              ) {
      toString = variableInstance+" unchanged";
    } else {
      toString = variableInstance+" binary content differs";
    }
    return toString;
  }
}
