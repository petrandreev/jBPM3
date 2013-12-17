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
package org.jbpm;

import java.io.Serializable;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.jbpm.configuration.ObjectFactory;
import org.jbpm.db.ContextSession;
import org.jbpm.db.GraphSession;
import org.jbpm.db.JobSession;
import org.jbpm.db.LoggingSession;
import org.jbpm.db.TaskMgmtSession;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.persistence.PersistenceService;
import org.jbpm.persistence.db.DbPersistenceService;
import org.jbpm.security.AuthenticationService;
import org.jbpm.svc.ServiceFactory;
import org.jbpm.svc.Services;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.jbpm.tx.TxService;

/**
 * is used to surround persistent operations to processes.
 * 
 * <p>
 * Obtain JbpmContext's via {@link org.jbpm.JbpmConfiguration#createJbpmContext()} and put it in a try-finally block like this:
 * </p>
 * 
 * <pre>
 * JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();
 * try {
 *   TaskInstance taskInstance = ...
 *   
 *   ...do your process operations...
 *   
 *   // in case you update a process object that was not fetched
 *   // with a ...ForUpdate method, you have to save it.
 *   jbpmContext.save(processInstance);
 * finally {
 *   jbpmContext.close();
 * }
 * </pre>
 * 
 * <p>
 * A JbpmContext separates jBPM from a sprecific environment. For each service that jBPM uses, there is an interface specified in the jBPM codebase. jBPM also includes
 * implementations that implement these services by using services in a specific environment. e.g. a hibernate session, a JMS asynchronous messaging system, ...
 * </p>
 * 
 * <p>
 * A JbpmContext can demarcate a transaction. When a PersistenceService is fetched from the JbpmContext, the default implementation for the persistence service will
 * create a hibernate session and start a transaction. So that transactions can be configured in the hibernate configuration.
 * </p>
 * 
 * <p>
 * A JbpmContext allows the user to overwrite (or make complete) the configuration by injecting objects programmatically. like e.g. a hibernate session factory or a
 * hibernate session or any other resource that can be fetched or created from the configuration.
 * </p>
 * 
 * <p>
 * Last but not least, JbpmContext provides convenient access to the most common operations such as {@link #getTaskList(String)}, {@link #newProcessInstance(String)}
 * {@link #loadTaskInstanceForUpdate(long)} and {@link #save(ProcessInstance)}.
 * </p>
 * 
 * <p>
 * All the <code>...ForUpdate(...)</code> methods will automatically save the loaded objects at <code>jbpmContext.close();</code>
 * </p>
 */
public class JbpmContext implements Serializable
{
  private static final long serialVersionUID = 1L;

  public static final String DEFAULT_JBPM_CONTEXT_NAME = "default.jbpm.context";

  ObjectFactory objectFactory = null;
  Services services = null;
  List<ProcessInstance> autoSaveProcessInstances = null;
  JbpmConfiguration jbpmConfiguration = null;

  /**
   * normally, JbpmContext object are created via a {@link JbpmConfiguration}.
   */
  public JbpmContext(Services services, ObjectFactory objectFactory)
  {
    log.debug("creating " + toString());
    this.services = services;
    this.objectFactory = objectFactory;
  }
  
  /**
   * make sure you close your JbpmContext in a finally block.
   */
  public void close()
  {
    log.debug("closing jbpmContext " + toString());
    try
    {
      if (services != null)
      {
        try
        {
          autoSave();
        }
        finally
        {
          services.close();
        }
      }
    }
    finally
    {
      if (jbpmConfiguration != null)
      {
        jbpmConfiguration.jbpmContextClosed(this);
      }
    }
  }

  /**
   * obtains the current JbpmContext from a thread local. The current JbpmContexts are maintained in a stack so that you can do nested context operations for different
   * jbpm configurations.
   * 
   * @deprecated method moved to {@link JbpmConfiguration}.
   */
  public static JbpmContext getCurrentJbpmContext()
  {
    JbpmContext currentJbpmContext = null;
    JbpmConfiguration currentJbpmConfiguration = JbpmConfiguration.getCurrentJbpmConfiguration();
    if (currentJbpmConfiguration != null)
    {
      currentJbpmContext = currentJbpmConfiguration.getCurrentJbpmContext();
    }
    return currentJbpmContext;
  }

  // convenience methods //////////////////////////////////////////////////////

  /**
   * deploys a process definition. For parsing process definitions from archives, see the static parseXxx methods on {@link ProcessDefinition}.
   */
  public void deployProcessDefinition(ProcessDefinition processDefinition)
  {
    getGraphSession().deployProcessDefinition(processDefinition);
  }

  /**
   * fetches the tasklist for the current authenticated actor. With the default configured authentication service, you can set the authenticated user with
   * {@link #setActorId(String)}, then all the subsequent operations will be performed on behalf of that actor.
   */
  public List<TaskInstance> getTaskList()
  {
    String actorId = getActorId();
    return getTaskMgmtSession().findTaskInstances(actorId);
  }

  /**
   * fetches the tasklist for the given actor.
   */
  public List<TaskInstance> getTaskList(String actorId)
  {
    return getTaskMgmtSession().findTaskInstances(actorId);
  }

  /**
   * fetches all the task instances for which at least one of the given actorIds is a candidate (pooled actor). Typically, for an actor, his/her personal actorId plus
   * all the actorIds representing the groups that person belongs to form the actorIds. Then the user interface should show only the option to take these tasks to the
   * actor's personal task list (with {@link TaskInstance#setActorId(String)}). Only task instances that are assigned to the actor directly should be offered the
   * possibility for performing the actual task.
   */
  public List<TaskInstance> getGroupTaskList(List<String> actorIds)
  {
    return getTaskMgmtSession().findPooledTaskInstances(actorIds);
  }

  /**
   * loads a task instance from the db.
   * 
   * @throws JbpmException in case no such task instance exists
   * @see #getTaskInstance(long)
   * @see #loadTaskInstanceForUpdate(long)
   * @see #getTaskInstanceForUpdate(long)
   */
  public TaskInstance loadTaskInstance(long taskInstanceId)
  {
    return getTaskMgmtSession().loadTaskInstance(taskInstanceId);
  }

  /**
   * gets a task instance from the db.
   * 
   * @return the task instance or null in case no such task instance exists.
   * @see #loadTaskInstance(long)
   * @see #loadTaskInstanceForUpdate(long)
   * @see #getTaskInstanceForUpdate(long)
   */
  public TaskInstance getTaskInstance(long taskInstanceId)
  {
    return getTaskMgmtSession().getTaskInstance(taskInstanceId);
  }

  /**
   * loads a task instance from the db and registers it for auto-save. The loaded task instance will be save automatically at the {@link #close()}. This is a
   * convenience method in case you plan to do update operations on this task instance.
   * 
   * @throws JbpmException in case no such task instance exists
   * @see #loadTaskInstance(long)
   * @see #getTaskInstance(long)
   * @see #getTaskInstanceForUpdate(long)
   */
  public TaskInstance loadTaskInstanceForUpdate(long taskInstanceId)
  {
    TaskInstance taskInstance = getTaskMgmtSession().loadTaskInstance(taskInstanceId);
    addAutoSaveTaskInstance(taskInstance);
    return taskInstance;
  }

  /**
   * gets a task instance from the db and registers it for auto-save. The loaded task instance will be save automatically at the {@link #close()}. This is a convenience
   * method in case you plan to do update operations on this task instance.
   * 
   * @return the task instance or null in case no such task instance exists.
   * @see #loadTaskInstance(long)
   * @see #getTaskInstance(long)
   * @see #loadTaskInstanceForUpdate(long)
   */
  public TaskInstance getTaskInstanceForUpdate(long taskInstanceId)
  {
    TaskInstance taskInstance = getTaskMgmtSession().getTaskInstance(taskInstanceId);
    if (taskInstance != null)
    {
      addAutoSaveTaskInstance(taskInstance);
    }
    return taskInstance;
  }

  /**
   * loads a token from the db.
   * 
   * @throws JbpmException in case no such token exists.
   * @see #getToken(long)
   * @see #loadTokenForUpdate(long)
   * @see #getTokenForUpdate(long)
   */
  public Token loadToken(long tokenId)
  {
    return getGraphSession().loadToken(tokenId);
  }

  /**
   * gets a token from the db.
   * 
   * @return the token or null in case no such token exists.
   * @see #loadToken(long)
   * @see #loadTokenForUpdate(long)
   * @see #getTokenForUpdate(long)
   */
  public Token getToken(long tokenId)
  {
    return getGraphSession().getToken(tokenId);
  }

  /**
   * loads a token from the db and registers it for auto-save. The loaded token will be {@link #save(Token)}d automatically at the {@link #close()}. This is a
   * convenience method in case you plan to do update operations on this token.
   * 
   * @throws JbpmException in case no such token exists.
   * @see #getToken(long)
   * @see #loadToken(long)
   * @see #getTokenForUpdate(long)
   */
  public Token loadTokenForUpdate(long tokenId)
  {
    Token token = getGraphSession().loadToken(tokenId);
    addAutoSaveToken(token);
    return token;
  }

  /**
   * get a token from the db and registers it for auto-save. The loaded token will be {@link #save(Token)}d automatically at the {@link #close()}. This is a convenience
   * method in case you plan to do update operations on this token.
   * 
   * @return the token or null in case no such token exists.
   * @see #getToken(long)
   * @see #loadToken(long)
   * @see #loadTokenForUpdate(long)
   */
  public Token getTokenForUpdate(long tokenId)
  {
    Token token = getGraphSession().getToken(tokenId);
    if (token != null)
    {
      addAutoSaveToken(token);
    }
    return token;
  }

  /**
   * loads a process instance from the db. Consider using {@link #loadProcessInstanceForUpdate(long)} if you plan to perform an update operation on the process
   * instance.
   * 
   * @throws JbpmException in case no such process instance exists.
   * @see #getProcessInstance(long)
   * @see #loadProcessInstanceForUpdate(long)
   * @see #getProcessInstanceForUpdate(long)
   */
  public ProcessInstance loadProcessInstance(long processInstanceId)
  {
    return getGraphSession().loadProcessInstance(processInstanceId);
  }

  /**
   * gets a process instance from the db. Consider using {@link #loadProcessInstanceForUpdate(long)} if you plan to perform an update operation on the process instance.
   * 
   * @return the token or null in case no such token exists.
   * @see #loadProcessInstance(long)
   * @see #loadProcessInstanceForUpdate(long)
   * @see #getProcessInstanceForUpdate(long)
   */
  public ProcessInstance getProcessInstance(long processInstanceId)
  {
    return getGraphSession().getProcessInstance(processInstanceId);
  }

  /**
   * loads a process instances from the db and registers it for auto-save. The loaded process instance will be {@link #save(ProcessInstance)}d automatically at the
   * {@link #close()}. This is a convenience method in case you plan to do update operations on this process instance.
   * 
   * @throws JbpmException in case no such process instance exists.
   * @see #loadProcessInstance(long)
   * @see #getProcessInstance(long)
   * @see #getProcessInstanceForUpdate(long)
   */
  public ProcessInstance loadProcessInstanceForUpdate(long processInstanceId)
  {
    ProcessInstance processInstance = getGraphSession().loadProcessInstance(processInstanceId);
    addAutoSaveProcessInstance(processInstance);
    return processInstance;
  }

  /**
   * gets a process instances from the db and registers it for auto-save. The loaded process instance will be {@link #save(ProcessInstance)}d automatically at the
   * {@link #close()}. This is a convenience method in case you plan to do update operations on this process instance.
   * 
   * @return the token or null in case no such token exists.
   * @see #loadProcessInstance(long)
   * @see #getProcessInstance(long)
   * @see #loadProcessInstanceForUpdate(long)
   */
  public ProcessInstance getProcessInstanceForUpdate(long processInstanceId)
  {
    ProcessInstance processInstance = getGraphSession().getProcessInstance(processInstanceId);
    if (processInstance != null)
    {
      addAutoSaveProcessInstance(processInstance);
    }
    return processInstance;
  }

  /**
   * returns the process instance with the given key or null if no such instance exists.
   */
  public ProcessInstance getProcessInstance(ProcessDefinition processDefinition, String key)
  {
    return getGraphSession().getProcessInstance(processDefinition, key);
  }

  /**
   * returns the process instance with the given key or throws an exception if no such instance exists.
   */
  public ProcessInstance loadProcessInstance(ProcessDefinition processDefinition, String key)
  {
    return getGraphSession().loadProcessInstance(processDefinition, key);
  }

  /**
   * returns the process instance with the given key or null if no such instance exists. Upon close of this jbpmContext, the fetched process instance will be
   * automatically saved.
   */
  public ProcessInstance getProcessInstanceForUpdate(ProcessDefinition processDefinition, String key)
  {
    ProcessInstance processInstance = getGraphSession().getProcessInstance(processDefinition, key);
    if (processInstance != null)
    {
      addAutoSaveProcessInstance(processInstance);
    }
    return processInstance;
  }

  /**
   * returns the process instance with the given key or throws an exception if no such instance exists. Upon close of this jbpmContext, the fetched process instance
   * will be automatically saved.
   */
  public ProcessInstance loadProcessInstanceForUpdate(ProcessDefinition processDefinition, String key)
  {
    ProcessInstance processInstance = getGraphSession().loadProcessInstance(processDefinition, key);
    if (processInstance != null)
    {
      addAutoSaveProcessInstance(processInstance);
    }
    return processInstance;
  }

  /**
   * creates a new process instance for the latest version of the process definition with the given name.
   * 
   * @throws JbpmException when no processDefinition with the given name is deployed.
   */
  public ProcessInstance newProcessInstance(String processDefinitionName)
  {
    ProcessDefinition processDefinition = getGraphSession().findLatestProcessDefinition(processDefinitionName);
    return new ProcessInstance(processDefinition);
  }

  /**
   * creates a new process instance for the latest version of the process definition with the given name and registers it for auto-save.
   * 
   * @throws JbpmException when no processDefinition with the given name is deployed.
   */
  public ProcessInstance newProcessInstanceForUpdate(String processDefinitionName)
  {
    ProcessDefinition processDefinition = getGraphSession().findLatestProcessDefinition(processDefinitionName);
    ProcessInstance processInstance = new ProcessInstance(processDefinition);
    addAutoSaveProcessInstance(processInstance);
    return processInstance;
  }

  /**
   * saves the process instance.
   */
  public void save(ProcessInstance processInstance)
  {
    if (services != null)
    {
      services.save(processInstance, this);
    }
  }

  /**
   * saves the complete process instance for this token.
   */
  public void save(Token token)
  {
    save(token.getProcessInstance());
  }

  /**
   * saves the complete process instance for this task instance.
   */
  public void save(TaskInstance taskInstance)
  {
    save(taskInstance.getTaskMgmtInstance().getProcessInstance());
  }

  /**
   * mark this transaction for rollback only in the persistence service. The {@link #close()} operation will then perform a rollback.
   */
  public void setRollbackOnly()
  {
    TxService txService = (services != null ? services.getTxService() : null);
    if (txService != null)
    {
      txService.setRollbackOnly();
    }
    else
    {
      throw new JbpmException("no transaction service configured");
    }
  }

  // services //////////////////////////////////////////////////////////

  /**
   * gives access to the services and service factories.
   */
  public Services getServices()
  {
    return services;
  }

  public ServiceFactory getServiceFactory(String name)
  {
    return services.getServiceFactory(name);
  }

  /**
   * gives access to the object factory containing the configuration for creating the service factories.
   */
  public ObjectFactory getObjectFactory()
  {
    return objectFactory;
  }

  // persistence methods //////////////////////////////////////////////////////

  /**
   * gets the hibernate session factory from the default configured persistence service.
   * @return the hibernate session factory, or <code>null</code> if a nonstandard
   * persistence service is configured
   */
  public SessionFactory getSessionFactory()
  {
    PersistenceService persistenceService = getPersistenceService();
    return persistenceService instanceof DbPersistenceService
      ? ((DbPersistenceService) persistenceService).getSessionFactory()
      : null;
  }

  /**
   * sets the hibernate session factory into the default configured persistence service, overwriting the configured session factory (if there is one configured).
   * if a nonstandard persistence service is configured, then this call has no effect.
   */
  public void setSessionFactory(SessionFactory sessionFactory)
  {
    PersistenceService persistenceService = getPersistenceService();
    if (persistenceService instanceof DbPersistenceService) {
      DbPersistenceService dbPersistenceService = (DbPersistenceService) persistenceService;
      dbPersistenceService.setSessionFactory(sessionFactory);
    }
  }

  /**
   * gets the hibernate session from the default configured persistence service.
   * @return the hibernate session, or <code>null</code> if a nonstandard
   * persistence service is configured.
   */
  public Session getSession()
  {
    PersistenceService persistenceService = getPersistenceService();
    return persistenceService instanceof DbPersistenceService
      ? ((DbPersistenceService) persistenceService).getSession()
      : null;
  }

  /**
   * sets the hibernate session into the default configured persistence service, preventing the creation of a session from the configured session factory (if there is
   * one configured).
   * if a nonstandard persistence service is configured, then this call has no effect.
   */
  public void setSession(Session session)
  {
    PersistenceService persistenceService = getPersistenceService();
    if (persistenceService instanceof DbPersistenceService) {
      DbPersistenceService dbPersistenceService = (DbPersistenceService) persistenceService;
      dbPersistenceService.setSession(session);
    }
  }

  /**
   * gets the jdbc connection from the default configured persistence service.
   * @return the jdbc connectoin, or <code>null</code> if a nonstandard
   * persistence service is configured.
   */
  public Connection getConnection()
  {
    PersistenceService persistenceService = getPersistenceService();
    return persistenceService instanceof DbPersistenceService
      ? ((DbPersistenceService) persistenceService).getConnection()
      : null;
  }

  /**
   * allows users to provide a jdbc connection to be used when the hibernate session is created.
   * if a nonstandard persistence service is configured, then this call has no effect.
   */
  public void setConnection(Connection connection)
  {
    PersistenceService persistenceService = getPersistenceService();
    if (persistenceService instanceof DbPersistenceService) {
      DbPersistenceService dbPersistenceService = (DbPersistenceService) persistenceService;
      dbPersistenceService.setConnection(connection);
    }
  }

  // jbpm database access sessions

  /**
   * more variables related database access.
   */
  public ContextSession getContextSession()
  {
    PersistenceService persistenceService = getPersistenceService();
    return persistenceService != null ? persistenceService.getContextSession() : null;
  }

  /**
   * more logging related database access.
   */
  public LoggingSession getLoggingSession()
  {
    PersistenceService persistenceService = getPersistenceService();
    return (persistenceService != null ? persistenceService.getLoggingSession() : null);
  }

  /**
   * more job related database access.
   */
  public JobSession getJobSession()
  {
    PersistenceService persistenceService = getPersistenceService();
    return (persistenceService != null ? persistenceService.getJobSession() : null);
  }

  /**
   * more graph (process) related database access.
   */
  public GraphSession getGraphSession()
  {
    PersistenceService persistenceService = getPersistenceService();
    return (persistenceService != null ? persistenceService.getGraphSession() : null);
  }

  /**
   * more task related database access.
   */
  public TaskMgmtSession getTaskMgmtSession()
  {
    PersistenceService persistenceService = getPersistenceService();
    return (persistenceService != null ? persistenceService.getTaskMgmtSession() : null);
  }

  // authentication methods ///////////////////////////////////////////////////

  /**
   * retrieves the current authenticated actor from the authentication service.
   */
  public String getActorId()
  {
    return services.getAuthenticationService().getActorId();
  }

  /**
   * sets the currently authenticated actorId.
   */
  public void setActorId(String actorId)
  {
    AuthenticationService authenticationService = services.getAuthenticationService();
    authenticationService.setActorId(actorId);
  }

  public void addAutoSaveProcessInstance(ProcessInstance processInstance)
  {
    if (autoSaveProcessInstances == null)
      autoSaveProcessInstances = new ArrayList<ProcessInstance>();
    autoSaveProcessInstances.add(processInstance);
  }

  public void addAutoSaveToken(Token token)
  {
    addAutoSaveProcessInstance(token.getProcessInstance());
  }

  public void addAutoSaveTaskInstance(TaskInstance taskInstance)
  {
    addAutoSaveProcessInstance(taskInstance.getTaskMgmtInstance().getProcessInstance());
  }
  
  // private methods //////////////////////////////////////////////////////////

  void autoSave()
  {
    if (autoSaveProcessInstances != null)
    {
      for (ProcessInstance processInstance : autoSaveProcessInstances) {
        save(processInstance);
      }
      autoSaveProcessInstances.clear();
    }
  }

  PersistenceService getPersistenceService()
  {
    return services != null ? services.getPersistenceService() : null;
  }

  public JbpmConfiguration getJbpmConfiguration()
  {
    return jbpmConfiguration;
  }

  private static Log log = LogFactory.getLog(JbpmContext.class);
}
