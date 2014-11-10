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
package org.jbpm.jbpm1024;

import java.io.IOException;
import java.io.Serializable;

import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.instantiation.ProcessClassLoader;

/**
 * Serializable variables are not being deserialized when retrieved from
 * process. Tests a serializable variable whose class file is stored in the
 * process definition.
 * 
 * @see <a href="https://jira.jboss.org/jira/browse/JBPM-1024">JBPM-1024</a>
 * @author Alejandro Guizar
 */
public class JBPM1024Test extends AbstractDbTestCase {

  public void testCustomSerializableVariableClass() throws IOException {
    // create and deploy process definition
    ProcessDefinition processDefinition = ProcessDefinition.parseParResource("org/jbpm/context/exe/CustomSerializable.zip");
    deployProcessDefinition(processDefinition);

    // create process instance
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    processInstance = saveAndReload(processInstance);

    // get custom object from context instance
    Object customSerializable = processInstance.getContextInstance()
      .getVariable("custom serializable");
    assertTrue(customSerializable instanceof Serializable);
    assertSame(ProcessClassLoader.class, customSerializable.getClass()
      .getClassLoader()
      .getClass());
    assertEquals("1984", customSerializable.toString());
  }
}
