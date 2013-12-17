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
package org.jbpm.ejb;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.annotation.Resources;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.jms.ConnectionFactory;
import javax.jms.Queue;
import javax.sql.DataSource;

import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.jbpm.command.Command;
import org.jbpm.jms.JmsConnectorServiceFactory;
import org.jbpm.persistence.jta.JtaDbPersistenceServiceFactory;

/**
 * Stateless session bean that executes {@linkplain Command commands} by calling their
 * {@link Command#execute(JbpmContext) execute} method on a separate {@link JbpmContext jBPM
 * context}.
 * 
 * <h3>Environment</h3>
 * 
 * <p>
 * The environment entries and resources available for customization are summarized in the table
 * below.
 * </p>
 * 
 * <table border="1">
 * <tr>
 * <th>Name</th>
 * <th>Type</th>
 * <th>Description</th>
 * </tr>
 * <tr>
 * <td><code>JbpmCfgResource</code></td>
 * <td>Environment Entry</td>
 * <td>The classpath resource from which to read the {@linkplain JbpmConfiguration jBPM
 * configuration}. Optional, defaults to <code>
 * jbpm.cfg.xml</code>.</td>
 * </tr>
 * <tr>
 * <td><code>jdbc/JbpmDataSource</code></td>
 * <td>Resource Manager Reference</td>
 * <td>Logical name of the data source that provides JDBC connections to the
 * {@linkplain JtaDbPersistenceServiceFactory persistence service}. Must match the
 * <code>hibernate.connection.datasource</code> property in the Hibernate configuration file.</td>
 * </tr>
 * <tr>
 * <td><code>jms/JbpmConnectionFactory</code></td>
 * <td>Resource Manager Reference</td>
 * <td>Logical name of the factory that provides JMS connections to the
 * {@linkplain JmsConnectorServiceFactory JMS connector service}. Required for processes that
 * contain asynchronous continuations.</td>
 * </tr>
 * <tr>
 * <td><code>jms/JobQueue</code></td>
 * <td>Message Destination Reference</td>
 * <td>The message service sends job messages to the queue referenced here. Must be
 * the same queue from which the {@linkplain JobListenerBean job listener bean} receives
 * messages.</td>
 * </tr>
 * </table>
 * 
 * @author Alejandro Guizar
 */
@Stateless
@Resources(value = {
  @Resource(name = "jdbc/JbpmDataSource", type = DataSource.class, shareable = true),
  @Resource(name = "jms/JbpmConnectionFactory", type = ConnectionFactory.class, shareable = true),
  @Resource(name = "jms/JobQueue", type = Queue.class)
})
public class CommandServiceBean implements LocalCommandService {

  private static final long serialVersionUID = 1L;

  @Resource
  private SessionContext sessionContext;
  @Resource(name = "JbpmCfgResource")
  private String jbpmCfgResource;

  private JbpmConfiguration jbpmConfiguration;

  /**
   * Creates the {@link JbpmConfiguration} to be used by this command service. In case the
   * environment key <code>JbpmCfgResource</code> is specified, that value is interpreted as the
   * name of the configuration resource to load from the classpath. If that key is absent, the
   * default configuration file will be used (jbpm.cfg.xml).
   */
  @PostConstruct
  void createConfiguration() {
    jbpmConfiguration = JbpmConfiguration.getInstance(jbpmCfgResource);
  }

  public Object execute(Command command) {
    JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();
    try {
      Object result = command.execute(jbpmContext);
      // check whether command requested a rollback
      if (jbpmContext.getServices().getTxService().isRollbackOnly()) {
        sessionContext.setRollbackOnly();
      }
      return result;
    }
    catch (RuntimeException e) {
      throw e;
    }
    catch (Exception e) {
      throw new JbpmException("failed to execute " + command, e);
    }
    finally {
      jbpmContext.close();
    }
  }
}
