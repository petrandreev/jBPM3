/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
package org.jbpm.enterprise.console;

import junit.framework.TestCase;

/**
 * @author Heiko.Braun <heiko.braun@jboss.com>
 */
public class ConsoleAvailabilityTest extends TestCase
{

  /**
   * Verify if the console has been deployed successfully
   * @throws Exception
   */
  public void testConsoleDeployment()
      throws Exception
  {
    String bindAddress = System.getProperty("jboss.bind.address");
    String host = bindAddress !=null ? bindAddress : "localhost:8080";
    if(host.indexOf(":")==-1) host = (host+":8080"); // default port

    System.out.println("Console URL: " + host);
    String response = HTTP.get("http://"+host +"/jbpm-console", null, true); // no auth
  }

}
