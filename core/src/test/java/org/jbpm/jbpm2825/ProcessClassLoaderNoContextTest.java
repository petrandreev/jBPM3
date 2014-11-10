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
package org.jbpm.jbpm2825;

import org.jbpm.JbpmConfiguration;
import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.file.def.FileDefinition;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.instantiation.ProcessClassLoader;

/**
 * {@link ProcessClassLoader} throws {@link NullPointerException} when no current context is
 * available.
 * 
 * @see <a href="https://jira.jboss.org/browse/JBPM-2825">JBPM-2825</a>
 * @author Alejandro Guizar
 */
public class ProcessClassLoaderNoContextTest extends AbstractDbTestCase {

  public void testProcessClassLoaderNoContext() {
    FileDefinition fileDefinition = new FileDefinition();
    byte[] magicNumber = {
      (byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE
    };
    fileDefinition.addFile("classes/org/example/Undef", magicNumber);

    ProcessDefinition processDefinition = new ProcessDefinition(getName());
    processDefinition.addDefinition(fileDefinition);
    jbpmContext.deployProcessDefinition(processDefinition);

    ClassLoader procClassLoader = JbpmConfiguration.getProcessClassLoader(processDefinition);
    String undefClassName = "org.example.Undef";

    closeJbpmContext();
    try {
      Class.forName(undefClassName, false, procClassLoader);
      fail("expected class " + undefClassName + " to not be found");
    }
    catch (ClassNotFoundException e) {
      assertEquals(undefClassName, e.getMessage());
    }
    finally {
      createJbpmContext();
    }
  }
}
