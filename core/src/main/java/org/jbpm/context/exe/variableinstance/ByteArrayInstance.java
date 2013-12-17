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

import org.hibernate.LockMode;
import org.hibernate.Session;
import org.jbpm.JbpmContext;
import org.jbpm.bytes.ByteArray;
import org.jbpm.context.exe.VariableInstance;
import org.jbpm.context.log.variableinstance.ByteArrayUpdateLog;

public class ByteArrayInstance extends VariableInstance {

  private static final long serialVersionUID = 1L;

  protected ByteArray value;

  public boolean isStorable(Object value) {
    return value instanceof ByteArray || value == null;
  }

  protected Object getObject() {
    return value;
  }

  protected void setObject(Object value) {
    if (token != null) {
      token.addLog(new ByteArrayUpdateLog(this, this.value, (ByteArray) value));
    }
    if (this.value != null) {
      JbpmContext jbpmContext = JbpmContext.getCurrentJbpmContext();
      ByteArray valueToUpdate = null;
      if (jbpmContext != null) {
        Session session = jbpmContext.getSession();
        if (session != null) { 
          valueToUpdate = (ByteArray) session.get(ByteArray.class, new Long(this.value.getId()), LockMode.UPGRADE);
          if( valueToUpdate != null ) { 
            valueToUpdate.update((ByteArray) value);
          }
        }
      }
     
      // if for some reason, this.value is NOT in the (hibernate) session, update it anyway. 
      if( valueToUpdate == null ) { 
        this.value.update((ByteArray) value);
      }
    }
    else { 
      this.value = (ByteArray) value;
    }
  }
}
