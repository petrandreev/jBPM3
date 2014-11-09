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
package org.jbpm.jbpm3884;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.jbpm.AbstractJbpmTestCase;
import org.jbpm.JbpmException;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;

@SuppressWarnings({
  "rawtypes", "unchecked"
})
public class JBPM3884Test extends AbstractJbpmTestCase {
  
  public void testAcccessToExternalEntitiesInJpdl() {
    ProcessDefinition processDefinition = ProcessDefinition.parseXmlResource("org/jbpm/jbpm3884/processdefinition.xml");
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    
    boolean processFailed  = false;
    // start process
    try { 
      processInstance.signal();
    } catch( JbpmException je ) { 
      processFailed = true;
     
      // Extract outside info
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      PrintStream out = new PrintStream(baos);
      je.printStackTrace(out);
      String stackTrace = new String(baos.toByteArray());
      int index = stackTrace.indexOf("root:x:0:0");
      assertFalse( "/etc/passwd file in jpdl process!", index >= 0 );
    }
    
    assertTrue( "Process startup did not fail as it should have.", processFailed);
  }

}
