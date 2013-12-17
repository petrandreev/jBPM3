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

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.zip.ZipInputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.jpdl.JpdlException;
import org.jbpm.jpdl.xml.Problem;
import org.jbpm.util.IoUtil;

/**
 * <p>
 * Deploys process definitions zipped and posted as multipart requests.
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
 * @author Koen Aers
 * @author Alejandro Guizar
 */
public class ProcessUploadServlet extends javax.servlet.http.HttpServlet {

  private static final long serialVersionUID = 1L;

  private JbpmConfiguration jbpmConfiguration;

  public void init() throws ServletException {
    String jbpmCfgResource = getServletContext().getInitParameter("jbpm.configuration.resource");
    jbpmConfiguration = JbpmConfiguration.getInstance(jbpmCfgResource);
  }

  protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
    PrintWriter out = response.getWriter();
    writeHeader(out);
    out.println("<p>GPD deployer is operational</p>");
    writeTrailer(out);
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
    try {
      InputStream processStream = parseRequest(request, response);
      JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();
      try {
        ProcessDefinition processDefinition = parseProcessArchive(processStream);
        deployProcessDefinition(processDefinition, response);
      }
      catch (JpdlException e) {
        List problems = e.getProblems();
        StringBuffer message = new StringBuffer();
        for (int i = 0, n = problems.size(); i < n; i++) {
          Problem problem = (Problem) problems.get(i);
          message.append(problem).append(IoUtil.lineSeparator);
        }
        response.sendError(HttpServletResponse.SC_BAD_REQUEST, message.toString());
      }
      finally {
        jbpmContext.close();
      }
    }
    catch (FileUploadException e) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
    }
  }

  private void writeHeader(PrintWriter out) {
    out.println("<html>");
    out.println("<head>");
    out.println("<title>Process Deployment</title>");
    out.print("<link rel='stylesheet' type='text/css' href='");
    out.print(getServletContext().getContextPath());
    out.println("/jbpm.css'/>");
    out.println("</head>");
    out.println("<body>");
  }

  private void writeTrailer(PrintWriter out) {
    out.println("</body>");
    out.println("</html>");
  }

  private InputStream parseRequest(HttpServletRequest request, HttpServletResponse response)
    throws IOException, FileUploadException {
    // check if request is multipart content
    if (!ServletFileUpload.isMultipartContent(request)) {
      throw new FileUploadException("request does not carry multipart content");
    }
    // Create a new file upload handler
    ServletFileUpload upload = new ServletFileUpload();
    // Parse the request
    for (FileItemIterator i = upload.getItemIterator(request); i.hasNext();) {
      FileItemStream part = i.next();
      if (!part.isFormField()) {
        if (log.isTraceEnabled()) log.trace("retrieved file " + part.getName());
        return part.openStream();
      }
    }
    // file not found
    throw new FileUploadException("request contains no file");
  }

  private ProcessDefinition parseProcessArchive(InputStream processStream) throws IOException {
    ZipInputStream processArchiveStream = new ZipInputStream(processStream);
    try {
      ProcessDefinition processDefinition = ProcessDefinition.parseParZipInputStream(processArchiveStream);
      if (log.isTraceEnabled()) log.trace("parsed " + processDefinition);
      return processDefinition;
    }
    finally {
      processArchiveStream.close();
    }
  }

  private void deployProcessDefinition(ProcessDefinition processDefinition,
    HttpServletResponse response) throws IOException {
    JbpmContext jbpmContext = jbpmConfiguration.getCurrentJbpmContext();
    try {
      jbpmContext.deployProcessDefinition(processDefinition);
      if (log.isTraceEnabled()) log.trace("deployed " + processDefinition);

      PrintWriter out = response.getWriter();
      writeHeader(out);
      out.println("<h3>Deployment report</h3>");
      out.print("<p>Process <em>");
      out.print(processDefinition.getName());
      out.print("</em> v");
      out.print(processDefinition.getVersion());
      out.println(" deployed successfully</p>");
      out.print("<p><a href='");
      out.print(getServletContext().getContextPath());
      out.println("/index.html'>Deploy another process</a></p>");
      writeTrailer(out);
    }
    catch (RuntimeException e) {
      jbpmContext.setRollbackOnly();

      log.error("failed to deploy " + processDefinition, e);
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to deploy process");
    }
  }

  private static final Log log = LogFactory.getLog(ProcessUploadServlet.class);
}