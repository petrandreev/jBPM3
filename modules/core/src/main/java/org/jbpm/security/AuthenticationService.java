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
package org.jbpm.security;

import org.jbpm.svc.Service;

/**
 * Responsible for knowing which user is currently logged in.
 * 
 * @author Original author n.n. (maybe Tom?), bernd.ruecker@camunda.com
 */
public interface AuthenticationService extends Service {

  /**
   * retrieve the currently authenticated actor
   */
  String getActorId();

  /**
   * set the currently authenticated actor. This method maybe ignored
   * by some implementations (e.g. when using JAAS it is not a good idea
   * to change the authenticated actor).
   */
  void setActorId(String actorId);
}
