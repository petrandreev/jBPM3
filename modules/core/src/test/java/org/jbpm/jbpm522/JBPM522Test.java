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
package org.jbpm.jbpm522;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tools.ant.Main;
import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.graph.def.ProcessDefinition;

/**
 * Failed attempts to deploy a process leave a process definition record
 * 
 * https://jira.jboss.org/jira/browse/JBPM-522
 * 
 * @author Alejandro Guizar
 */
public class JBPM522Test extends AbstractDbTestCase
{
  private static final Log log = LogFactory.getLog(JBPM522Test.class);

  public void testDeployProcess()
  {
    runTarget("deploy.process");
    List processDefinitions = graphSession.findAllProcessDefinitionVersions("timerProcess");
    assertEquals(1, processDefinitions.size());
    ProcessDefinition processDefinition = (ProcessDefinition)processDefinitions.get(0);
    assertEquals("timerProcess", processDefinition.getName());
    graphSession.deleteProcessDefinition(processDefinition);
  }

  public void testDeployBadProcess()
  {
    runTarget("deploy.bad.process");
    List processDefinitions = graphSession.findAllProcessDefinitionVersions("sequential process");
    assertEquals(0, processDefinitions.size());
  }

  public void testDeployProcesses()
  {
    runTarget("deploy.processes");
    List processDefinitions = graphSession.findAllProcessDefinitionVersions("classLoadingProcess");
    assertEquals(1, processDefinitions.size());
    ProcessDefinition processDefinition = (ProcessDefinition)processDefinitions.get(0);
    assertEquals("classLoadingProcess", processDefinition.getName());
    graphSession.deleteProcessDefinition(processDefinition);

    processDefinitions = graphSession.findAllProcessDefinitionVersions("timerProcess");
    assertEquals(1, processDefinitions.size());
    processDefinition = (ProcessDefinition)processDefinitions.get(0);
    assertEquals("timerProcess", processDefinition.getName());
    graphSession.deleteProcessDefinition(processDefinition);
  }

  public void testDeployProcessesIncludingBad()
  {
    runTarget("deploy.processes.including.bad");
    List processDefinitions = graphSession.findAllProcessDefinitionVersions("timerProcess");
    assertEquals(0, processDefinitions.size());

    processDefinitions = graphSession.findAllProcessDefinitionVersions("sequential process");
    assertEquals(0, processDefinitions.size());

    processDefinitions = graphSession.findAllProcessDefinitionVersions("classLoadingProces");
    assertEquals(0, processDefinitions.size());
  }

  private static void runTarget(String target)
  {
    String[] args = { "-buildfile", JBPM522Test.class.getResource("build.xml").getPath(), target };

    PrintStream out = System.out;
    PrintStream err = System.err;
    
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintStream tmp = new PrintStream(baos);
    
    try
    {
      System.setOut(tmp);
      System.setErr(tmp);
      
      Main antMain = new Main()
      {
        protected void exit(int exitCode)
        {
          // prevent ant from terminating the VM
        }
      };
      antMain.startAnt(args, System.getProperties(), Thread.currentThread().getContextClassLoader());
      
    }
    finally
    {
      System.setOut(out);
      System.setErr(err);
      
      log.info("\n" + new String(baos.toByteArray()));
    }
  }
}
