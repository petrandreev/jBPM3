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
package org.jbpm.examples.mail;

import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.mail.AddressResolver;

public class MailTest
{
  public static class MyAddressResolver implements AddressResolver
  {
    private static final long serialVersionUID = 1L;

    public Object resolveAddress(String actorId)
    {
      return actorId + "@dalton.com";
    }
  }

  public void testSimpleProcess() throws Exception
  {
    JbpmConfiguration jbpmConfiguration = JbpmConfiguration.parseXmlString(
        "<jbpm-configuration>" + 
        "  <bean name='jbpm.mail.address.resolver' class='" + MyAddressResolver.class.getName() + "' singleton='true' />" + 
        "</jbpm-configuration>");

    JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();
    try
    {
      ProcessDefinition processDefinition = ProcessDefinition.parseXmlString(
          "<process-definition name='mailtest'>" + 
          "  <start-state name='start'>" + 
          "    <transition to='start toothpick line' />" + 
          "  </start-state>" + 
          "  <task-node name='start toothpick line'>" + 
          "    <task  notify='yes'>" + 
          "      <assignment actor-id='grandma' />" + 
          "    </task>" + 
          "    <transition to='end' />" + 
          "  </task-node>" + 
          "  <end-state name='end' />" + 
          "</process-definition>");
      ProcessInstance processInstance = new ProcessInstance(processDefinition);
      processInstance.signal();

    }
    finally
    {
      jbpmContext.close();
    }
  }
}
