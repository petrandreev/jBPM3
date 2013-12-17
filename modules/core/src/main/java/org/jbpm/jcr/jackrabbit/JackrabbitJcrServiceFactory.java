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
package org.jbpm.jcr.jackrabbit;

import java.io.InputStream;

import javax.jcr.Repository;

import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.jbpm.JbpmException;
import org.jbpm.jcr.impl.AbstractJcrServiceFactory;
import org.jbpm.util.ClassLoaderUtil;

public class JackrabbitJcrServiceFactory extends AbstractJcrServiceFactory {

  private static final long serialVersionUID = 1L;
  
  String configuration = null;
  String directory = null;
  
  // cached repository
  transient Repository repository = null;

  protected synchronized Repository getRepository() {
    if (repository==null) {
      try {
        InputStream stream = ClassLoaderUtil.getStream(configuration);
        RepositoryConfig config = RepositoryConfig.create(stream, directory);
        repository = RepositoryImpl.create(config);
      } catch (Exception e) {
        // NOTE that Error's are not caught because that might halt the JVM and mask the original Error.
        throw new JbpmException("couldn't create new jackrabbit repository with configResource '"+configuration+"' and directory '"+directory+"'", e);
      }
    }
    return repository;
  }
}
