package org.jbpm.web;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.jbpm.JbpmConfiguration;

/**
 * Closes the jBPM configuration on servlet context destruction.
 * <h1>Config parameters</h1>
 * <table border="1">
 *   <tr>
 *     <th>Name</th> 
 *     <th>Description</th> 
 *     <th>Type</th>
 *     <th>Default value</th>
 *   </tr> 
 *   <tr>
 *     <td>jbpm.configuration.resource</td>
 *     <td>resource location of the jbpm.cfg.xml</td>
 *     <td>String</td>
 *     <td>jbpm.cfg.xml</td>
 *   </tr> 
 * </table>
 * @deprecated Replaced by {@link org.jbpm.web.JbpmConfigurationCloser}
 */
public class CloseJbpmConfigurationServlet extends HttpServlet {

  private static final long serialVersionUID = 1L;

  String configurationName;
  
  public void init() throws ServletException {
    configurationName = getInitParameter("jbpm.configuration.resource", null);
  }
  
  public void destroy() {
    JbpmConfiguration jbpmConfiguration = JbpmConfiguration.getInstance(configurationName);
    jbpmConfiguration.close();
  }

  String getInitParameter(String name, String defaultValue) {
    String value = getInitParameter(name);
    if (value!=null) {
      return value;
    }
    return defaultValue;
  }
}
