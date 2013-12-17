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
package org.jbpm.security.authentication;

import java.security.AccessController;
import java.security.Principal;
import java.util.Set;

import javax.security.auth.Subject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jbpm.JbpmConfiguration.Configs;
import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.jbpm.security.AuthenticationService;
import org.jbpm.util.ClassLoaderUtil;

/**
 * gets the authenticated actor id from the current Subject. This Authenticator is either
 * configured via the {@link SubjectAuthenticationServiceFactory} or it requires the two other
 * configuration parameter 'jbpm.authenticator.principal.classname' and
 * 'jbpm.authenticator.principal.allow.overwrite' This configuration property specifies the
 * class name of the principal that should be used from the current subject. This could be for
 * example org.jboss.security.CallerIdentity in an JBoss AS. If not actorId is set, the name of
 * that principal is used as the currently authenticated actorId. If an actorId!=null is set
 * (via setActorId) this one overwrites the principal. This behavior is configurable via the
 * 'jbpm.authenticator.principal.allow.overwrite' attribute. If this is set to false, setActorId
 * is simply ignored.
 */
public class SubjectAuthenticationService implements AuthenticationService {

  private static final long serialVersionUID = 1L;

  private static final Log log = LogFactory.getLog(JbpmContext.class);

  private Class principalClass;
  private String actorId;
  private boolean allowActorIdOverwrite;

  public SubjectAuthenticationService(String principalClassName, Boolean allowActorIdOverwrite) {
    if (principalClassName != null) {
      initPrincipalClass(principalClassName);
    }
    else {
      initPrincipalClass(Configs.getString("jbpm.authenticator.principal.classname"));
    }
    if (allowActorIdOverwrite != null) {
      this.allowActorIdOverwrite = allowActorIdOverwrite.booleanValue();
    }
    else {
      this.allowActorIdOverwrite = Configs.getBoolean("jbpm.authenticator.principal.allow.overwrite");
    }
  }

  public SubjectAuthenticationService() {
    initPrincipalClass(Configs.getString("jbpm.authenticator.principal.classname"));
    allowActorIdOverwrite = Configs.getBoolean("jbpm.authenticator.principal.allow.overwrite");
  }

  protected void initPrincipalClass(String principalClassName) {
    try {
      principalClass = ClassLoaderUtil.classForName(principalClassName);
    }
    catch (ClassNotFoundException e) {
      throw new JbpmException("principal class not found: " + principalClassName, e);
    }
  }

  public String getActorId() {
    if (actorId == null) {
      Subject subject = Subject.getSubject(AccessController.getContext());
      if (subject == null) {
        log.warn("no subject exists! cannot get actorId");
        return null;
      }

      Set principals = subject.getPrincipals(principalClass);
      if (principals != null && !principals.isEmpty()) {
        // always use the first one (so be patient what Principal classes are used)
        Principal principal = (Principal) principals.iterator().next();
        actorId = principal.getName();
      }
    }
    return actorId;
  }

  public void setActorId(String actorId) {
    if (allowActorIdOverwrite && actorId != null) {
      this.actorId = actorId;
    }
  }

  public void close() {
  }
}
