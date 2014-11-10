package org.jbpm.web;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.jbpm.JbpmConfiguration;

/**
 * <p>
 * Closes the jBPM configuration on destruction.
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
 * @deprecated Replaced by {@link org.jbpm.web.JbpmConfigurationCloser}
 */
public class CloseJbpmConfigurationServlet extends HttpServlet {

  private static final long serialVersionUID = 1L;

  private String configurationName;

  public void init() throws ServletException {
    configurationName = getServletContext().getInitParameter("jbpm.configuration.resource");
  }

  public void destroy() {
    JbpmConfiguration jbpmConfiguration = JbpmConfiguration.getInstance(configurationName);
    jbpmConfiguration.close();
  }
}
