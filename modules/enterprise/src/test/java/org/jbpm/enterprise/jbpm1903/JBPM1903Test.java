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
package org.jbpm.enterprise.jbpm1903;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.jms.JMSException;
import javax.jms.Queue;

import junit.framework.Test;

import org.jboss.bpm.api.test.IntegrationTestSetup;
import org.jbpm.enterprise.AbstractEnterpriseTestCase;
import org.jbpm.util.IoUtil;

/**
 * ENC is not reachable from within action handlers.
 * 
 * https://jira.jboss.org/jira/browse/JBPM-1903
 * 
 * @author Alejandro Guizar
 */
public class JBPM1903Test extends AbstractEnterpriseTestCase {

  public static Test suite() throws Exception {
    return new IntegrationTestSetup(JBPM1903Test.class, "enterprise-test.war");
  }

  public void testENCInActionHandler() throws IOException, JMSException {
    byte[] processArchive = createProcessArchive();
    deployProcessDefinition(processArchive);
    long processInstanceId = startProcessInstance("jbpm-1903").getId();
    Queue queue = (Queue) getVariable(processInstanceId, "queue");
    assertEquals("JbpmJobQueue", queue.getQueueName());
  }

  private byte[] createProcessArchive() throws IOException {
    ByteArrayOutputStream memoryOut = new ByteArrayOutputStream();
    ZipOutputStream zipOut = new ZipOutputStream(memoryOut);

    zipOut.putNextEntry(new ZipEntry("processdefinition.xml"));
    InputStream resourceIn = getClass().getResourceAsStream("processdefinition.xml");
    IoUtil.transfer(resourceIn, zipOut);
    resourceIn.close();

    zipOut.putNextEntry(new ZipEntry("classes/org/jbpm/enterprise/jbpm1903/ENCAction.class"));
    resourceIn = getClass().getResourceAsStream("ENCAction.class");
    IoUtil.transfer(resourceIn, zipOut);
    resourceIn.close();

    zipOut.close();
    return memoryOut.toByteArray();
  }
}
