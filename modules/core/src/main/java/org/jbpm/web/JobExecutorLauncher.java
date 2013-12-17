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
package org.jbpm.web;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.jbpm.JbpmConfiguration;
import org.jbpm.job.executor.JobExecutor;

/**
 * Starts the job executor on servlet context initialization and stops it on
 * servlet context destruction.
 * <h3>Configuration</h3>
 * Servlet context init parameters.
 * <table border="1">
 * <tr>
 * <th>Name</th>
 * <th>Description</th>
 * <th>Default value</th>
 * </tr>
 * <tr>
 * <td>jbpm.configuration.resource</td>
 * <td>name of the resource that defines the jBPM configuration</td>
 * <td>jbpm.cfg.xml</td>
 * </tr>
 * </table>
 * @author Alejandro Guizar
 */
public class JobExecutorLauncher implements ServletContextListener {

  private JbpmConfiguration jbpmConfiguration;

  public void contextInitialized(ServletContextEvent event) {
    String jbpmCfgResource = event.getServletContext().getInitParameter("jbpm.configuration.resource");
    jbpmConfiguration = JbpmConfiguration.getInstance(jbpmCfgResource);
    JobExecutor jobExecutor = jbpmConfiguration.getJobExecutor();
    if (jobExecutor != null)
      jobExecutor.start();
  }

  public void contextDestroyed(ServletContextEvent event) {
    JobExecutor jobExecutor = jbpmConfiguration.getJobExecutor();
    if (jobExecutor != null)
      jobExecutor.stop();
  }

}
