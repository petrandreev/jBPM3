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
package org.jbpm.svc;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jbpm.JbpmContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.logging.LoggingService;
import org.jbpm.msg.MessageService;
import org.jbpm.persistence.JbpmPersistenceException;
import org.jbpm.persistence.PersistenceService;
import org.jbpm.persistence.db.DbPersistenceService;
import org.jbpm.persistence.db.StaleObjectLogConfigurer;
import org.jbpm.scheduler.SchedulerService;
import org.jbpm.security.AuthenticationService;
import org.jbpm.security.AuthorizationService;
import org.jbpm.svc.save.CascadeSaveOperation;
import org.jbpm.svc.save.CheckUnpersistableVariablesOperation;
import org.jbpm.svc.save.SaveLogsOperation;
import org.jbpm.svc.save.SaveOperation;
import org.jbpm.tx.TxService;

public class Services implements Serializable {

  private static final long serialVersionUID = 1L;

  public static final String SERVICENAME_AUTHENTICATION = "authentication";
  public static final String SERVICENAME_AUTHORIZATION = "authorization";
  public static final String SERVICENAME_TX = "tx";
  public static final String SERVICENAME_LOGGING = "logging";
  public static final String SERVICENAME_MESSAGE = "message";
  public static final String SERVICENAME_PERSISTENCE = "persistence";
  public static final String SERVICENAME_SCHEDULER = "scheduler";
  public static final String SERVICENAME_JCR = "jcr";
  public static final String SERVICENAME_ADDRESSRESOLVER = "addressresolver";

  private static final List defaultSaveOperations = createDefaultSaveOperations();

  private static List createDefaultSaveOperations() {
    SaveOperation[] operations = new SaveOperation[3];
    operations[0] = new CheckUnpersistableVariablesOperation();
    // insert the logs, which may have references to the execution data
    operations[1] = new SaveLogsOperation();
    // save subprocess instances in cascade
    operations[2] = new CascadeSaveOperation();
    return Arrays.asList(operations);
  }

  private final Map serviceFactories;
  private final Map services;
  private List saveOperations = defaultSaveOperations;

  public static Service getCurrentService(String name) {
    return getCurrentService(name, true);
  }

  public static Service getCurrentService(String name, boolean isRequired) {
    JbpmContext jbpmContext = JbpmContext.getCurrentJbpmContext();
    if (jbpmContext != null) {
      Service service = jbpmContext.getServices().getService(name);
      if (service != null) return service;
    }
    if (isRequired) {
      throw new JbpmServiceException("service unavailable: " + name);
    }
    return null;
  }

  public Services(Map serviceFactories) {
    this(serviceFactories, null);
  }

  public Services(Map serviceFactories, List saveOperations) {
    if (serviceFactories == null) {
      throw new IllegalArgumentException("null service factories");
    }
    this.serviceFactories = serviceFactories;
    this.services = new HashMap(serviceFactories.size());
    if (saveOperations != null) this.saveOperations = saveOperations;
  }

  /** @deprecated use {@link #Services(Map, List)} instead */
  public Services(Map serviceFactories, List serviceNames, List saveOperations) {
    this(orderedMap(serviceFactories, serviceNames), saveOperations);
  }

  private static Map orderedMap(Map map, List orderedKeys) {
    Map orderedMap = new LinkedHashMap(map.size());
    for (Iterator iter = orderedKeys.iterator(); iter.hasNext();) {
      Object key = iter.next();
      orderedMap.put(key, map.get(key));
    }
    return orderedMap;
  }

  public void setSaveOperations(List saveOperations) {
    if (saveOperations == null) {
      throw new IllegalArgumentException("save operations list is null");
    }
    this.saveOperations = saveOperations;
  }

  public void addSaveOperation(SaveOperation saveOperation) {
    if (saveOperation == null) {
      throw new IllegalArgumentException("save operation is null");
    }
    if (saveOperations == defaultSaveOperations) {
      saveOperations = new ArrayList(defaultSaveOperations);
    }
    saveOperations.add(saveOperation);
  }

  public Map getServiceFactories() {
    return serviceFactories;
  }

  public ServiceFactory getServiceFactory(String name) {
    return (ServiceFactory) getServiceFactories().get(name);
  }

  public boolean hasService(String name) {
    return services.containsKey(name);
  }

  public Service getService(String name) {
    Service service = (Service) services.get(name);
    if (service == null) {
      ServiceFactory serviceFactory = getServiceFactory(name);
      if (serviceFactory != null) {
        service = serviceFactory.openService();
        services.put(name, service);
      }
    }
    return service;
  }

  public void save(ProcessInstance processInstance, JbpmContext jbpmContext) {
    if (saveOperations != defaultSaveOperations && log.isDebugEnabled()) {
      log.debug("executing custom save operations: " + saveOperations);
    }

    for (Iterator iter = saveOperations.iterator(); iter.hasNext();) {
      SaveOperation saveOperation = (SaveOperation) iter.next();
      saveOperation.save(processInstance, jbpmContext);
    }
  }

  // services /////////////////////////////////////////////////////////////////

  public AuthenticationService getAuthenticationService() {
    return (AuthenticationService) getService(SERVICENAME_AUTHENTICATION);
  }

  public AuthorizationService getAuthorizationService() {
    return (AuthorizationService) getService(SERVICENAME_AUTHORIZATION);
  }

  public LoggingService getLoggingService() {
    return (LoggingService) getService(SERVICENAME_LOGGING);
  }

  public MessageService getMessageService() {
    return (MessageService) getService(SERVICENAME_MESSAGE);
  }

  public PersistenceService getPersistenceService() {
    return (PersistenceService) getService(SERVICENAME_PERSISTENCE);
  }

  public SchedulerService getSchedulerService() {
    return (SchedulerService) getService(SERVICENAME_SCHEDULER);
  }

  public TxService getTxService() {
    return (TxService) getService(SERVICENAME_TX);
  }

  public void setAuthenticationService(AuthenticationService authenticationService) {
    services.put(SERVICENAME_AUTHENTICATION, authenticationService);
  }

  public void setAuthorizationService(AuthorizationService authorizationService) {
    services.put(SERVICENAME_AUTHORIZATION, authorizationService);
  }

  public void setLoggingService(LoggingService loggingService) {
    services.put(SERVICENAME_LOGGING, loggingService);
  }

  public void setMessageService(MessageService messageService) {
    services.put(SERVICENAME_MESSAGE, messageService);
  }

  public void setPersistenceService(PersistenceService persistenceService) {
    services.put(SERVICENAME_PERSISTENCE, persistenceService);
  }

  public void setSchedulerService(SchedulerService schedulerService) {
    services.put(SERVICENAME_SCHEDULER, schedulerService);
  }

  public void setTxService(TxService txService) {
    services.put(SERVICENAME_TX, txService);
  }

  public void close() {
    RuntimeException firstException = null;
    for (Iterator iter = serviceFactories.keySet().iterator(); iter.hasNext();) {
      String serviceName = (String) iter.next();
      Service service = (Service) services.get(serviceName);
      if (service == null) continue;

      try {
        service.close();
      }
      catch (RuntimeException e) {
        if (firstException == null) {
          firstException = e;
        }
        else {
          // if this is a locking exception, keep it quiet
          if (DbPersistenceService.isLockingException(e)) {
            StaleObjectLogConfigurer.getStaleObjectExceptionsLog().error("problem closing '"
              + serviceName + "' service", e);
          }
          else {
            log.error("problem closing '" + serviceName + "' service", e);
          }
        }
      }
    }

    if (firstException != null) throw firstException;
  }

  /** @deprecated call {@link DbPersistenceService#isLockingException(Exception)} instead */
  public static boolean isCausedByStaleState(JbpmPersistenceException persistenceException) {
    return DbPersistenceService.isLockingException(persistenceException);
  }

  /** assigns an identifier to the given object */
  public static void assignId(Object object) {
    PersistenceService service =
      (PersistenceService) getCurrentService(SERVICENAME_PERSISTENCE, false);
    if (service != null) service.assignId(object);
  }

  private static final Log log = LogFactory.getLog(Services.class);
}
