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

import org.jbpm.svc.Service;
import org.jbpm.svc.ServiceFactory;

/**
 * <p>
 * Factory to create a {@link SubjectAuthenticationService}.
 * </p>
 * <p>
 * Two properties can be set: allowActorIdOverwrite & principalClassName
 * </p>
 * <p>
 * principalClassName configuration property specifies the class name of the principal that should
 * be used from the current subject. This could be for example org.jboss.security.CallerIdentity in
 * an JBoss AS.
 * </p>
 * <p>
 * If not actorId is set, the name of that principal is used as the currently authenticated actorId.
 * If an actorId!=null is set (via setActorId) this one overwrites the principal. This behavior is
 * configurable via the allowActorIdOverwrite attribute. If this is set to false, setActorId is
 * simply ignored.
 * <p>
 * <p>
 * Example:
 * </p>
 * 
 * <pre>
 * &lt;service name=&quot;authentication&quot;&gt;
 *   &lt;factory&gt;
 *      &lt;bean class=&quot;org.jbpm.security.authentication.SubjectAuthenticationServiceFactory&quot;&gt;
 *         &lt;field name=&quot;principalClassName&quot;&gt; &lt;string value=&quot;org.jboss.security.CallerIdentity&quot; /&gt; &lt;/field&gt;
 *         &lt;field name=&quot;allowActorIdOverwrite&quot;&gt; &lt;boolean value=&quot;true&quot; /&gt; &lt;/field&gt;
 *      &lt;/bean&gt;
 *   &lt;/factory&gt;
 * &lt;/service&gt;
 * </pre>
 * 
 * @author bernd.ruecker@camunda.com
 */
public class SubjectAuthenticationServiceFactory implements ServiceFactory {

  private static final long serialVersionUID = 1L;

  private Boolean allowActorIdOverwrite;

  private String principalClassName;

  public Service openService() {
    return new SubjectAuthenticationService(principalClassName, allowActorIdOverwrite);
  }

  public void close() {
  }

  public Boolean isAllowActorIdOverwrite() {
    return allowActorIdOverwrite;
  }

  public void setAllowActorIdOverwrite(Boolean allowActorIdOverwrite) {
    this.allowActorIdOverwrite = allowActorIdOverwrite;
  }

  public String getPrincipalClassName() {
    return principalClassName;
  }

  public void setPrincipalClassName(String principalClassName) {
    this.principalClassName = principalClassName;
  }
}
