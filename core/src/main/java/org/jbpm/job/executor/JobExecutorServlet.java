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
package org.jbpm.job.executor;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Iterator;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jbpm.JbpmConfiguration;

/**
 * <p>
 * Starts the job executor on initialization and closes the jBPM configuration on destruction.
 * Closing the jBPM configuration also stops the job executor.
 * </p>
 * <h3>Servlet context parameters</h3>
 * <table border="1">
 * <tr>
 * <th>Name</th>
 * <th>Description</th>
 * <th>Default value</th>
 * </tr>
 * <tr>
 * <td>jbpm.configuration.resource</td>
 * <td>classpath resource containing the jBPM configuration</td>
 * <td>jbpm.cfg.xml</td>
 * </tr>
 * </table>
 * 
 * @deprecated Replaced by {@link org.jbpm.web.JobExecutorLauncher}
 */
public class JobExecutorServlet extends HttpServlet {

  private static final long serialVersionUID = 1L;

  private JbpmConfiguration jbpmConfiguration;

  public void init() throws ServletException {
    String configurationName = getServletContext().getInitParameter("jbpm.configuration.resource");
    jbpmConfiguration = JbpmConfiguration.getInstance(configurationName);
    jbpmConfiguration.startJobExecutor();
  }

  protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
    PrintWriter out = response.getWriter();
    out.println("<html>");
    out.println("<head><title>jBPM Job Executor</title></head>");
    out.println("<body>");
    out.println("<h3>jBPM Job Executor</h3><hr />");
    Collection threads = jbpmConfiguration.getJobExecutor().getThreads().values();
    for (Iterator iter = threads.iterator(); iter.hasNext();) {
      Thread thread = (Thread) iter.next();
      out.println("<p>" + thread.getName() + "</p>");
    }
    out.println("</body>");
    out.println("</html>");
  }

  public void destroy() {
    jbpmConfiguration.close();
  }
}
