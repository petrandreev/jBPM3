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
package org.jbpm.jbpm2017;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;

import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;

/**
 * STRINGVALUE_ column in JBPM_VARIABLEINSTANCE table should be text, not
 * varchar(255)
 * 
 * @see <a href="https://jira.jboss.org/jira/browse/JBPM-2017">JBPM-2017</a>
 * @author Alejandro Guizar
 */
public class JBPM2017Test extends AbstractDbTestCase {

  public void testTextVariable() throws IOException {
    final String processName = "jbpm2017";
    ProcessDefinition processDefinition = ProcessDefinition.createNewProcessDefinition();
    processDefinition.setName(processName);
    deployProcessDefinition(processDefinition);

    ProcessInstance processInstance = jbpmContext.newProcessInstance(processName);
    String quote = readString(getClass().getResourceAsStream("quote.txt"));
    processInstance.getContextInstance().setVariable("quote", quote);

    processInstance = saveAndReload(processInstance);
    assertEquals(quote, processInstance.getContextInstance().getVariable("quote"));
  }

  private static String readString(InputStream in) throws IOException {
    StringWriter writer = new StringWriter();
    Reader reader = new InputStreamReader(in);
    for (int ch; (ch = reader.read()) != -1;) {
      writer.write(ch);
    }
    in.close();
    return writer.toString();
  }
}
