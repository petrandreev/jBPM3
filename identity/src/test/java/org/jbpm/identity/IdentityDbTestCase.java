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
package org.jbpm.identity;

import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.identity.hibernate.IdentitySession;

public abstract class IdentityDbTestCase extends AbstractDbTestCase {

  protected IdentitySession identitySession;

  protected void initializeMembers() {
    super.initializeMembers();
    identitySession = (IdentitySession) jbpmContext.getServices()
      .getPersistenceService()
      .getCustomSession(IdentitySession.class);
  }

  protected void resetMembers() {
    super.resetMembers();
    identitySession = null;
  }

  protected User saveAndReload(User user) {
    identitySession.saveEntity(user);
    newTransaction();
    return identitySession.loadUser(user.getId());
  }

  protected Group saveAndReload(Group group) {
    identitySession.saveEntity(group);
    newTransaction();
    return identitySession.loadGroup(group.getId());
  }
}
