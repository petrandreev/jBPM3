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
package org.jbpm.identity.ant;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.ant.AntHelper;
import org.jbpm.identity.Entity;
import org.jbpm.identity.hibernate.IdentitySession;
import org.jbpm.identity.xml.IdentityXmlParser;

public class LoadIdentitiesTask extends Task {

  private File file;
  private String jbpmCfg;

  public void execute() throws BuildException {
    // get the JbpmSessionFactory
    JbpmConfiguration jbpmConfiguration = AntHelper.getJbpmConfiguration(jbpmCfg);

    // if attribute file is set, deploy that file file
    if (file == null) throw new BuildException("no file specified in the loadidentities task");

    log("loading identities from " + file + " ...");
    FileInputStream fileInputStream;
    try {
      fileInputStream = new FileInputStream(file);
    }
    catch (FileNotFoundException e) {
      throw new BuildException("identities file '" + file + "' not found");
    }
    Entity[] entities = IdentityXmlParser.parseEntitiesResource(fileInputStream);

    JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();
    try {
      IdentitySession identitySession = (IdentitySession) jbpmContext.getServices()
        .getPersistenceService()
        .getCustomSession(IdentitySession.class);
      for (int i = 0; i < entities.length; i++) {
        identitySession.saveEntity(entities[i]);
      }
    }
    finally {
      jbpmContext.close();
    }
  }

  public void setFile(File file) {
    this.file = file;
  }

  public void setJbpmCfg(String jbpmCfg) {
    this.jbpmCfg = jbpmCfg;
  }
}
