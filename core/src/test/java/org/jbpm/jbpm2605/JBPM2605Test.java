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
package org.jbpm.jbpm2605;

import org.hibernate.SessionFactory;

import org.jbpm.AbstractJbpmTestCase;
import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.persistence.db.DbPersistenceServiceFactory;
import org.jbpm.persistence.db.MockSessionFactory;

/**
 * {@link DbPersistenceServiceFactory} should not close {@link SessionFactory} if it did not
 * open it.
 * 
 * @see <a href="https://jira.jboss.org/browse/JBPM-2605">JBPM-2605</a>
 * @author Alejandro Guizar
 */
public class JBPM2605Test extends AbstractJbpmTestCase {

  public void testExternalSessionFactory() {
    SessionFactory sessionFactory = new MockSessionFactory();

    JbpmConfiguration jbpmConfiguration = JbpmConfiguration.parseResource("jbpm.cfg.xml");
    try {
      JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();
      jbpmContext.setSessionFactory(sessionFactory);
    }
    finally {
      jbpmConfiguration.close();
    }

    assert !sessionFactory.isClosed() : "expected external session factory to remain open";
  }
}
