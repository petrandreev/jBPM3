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

import org.hibernate.Session;
import org.jbpm.JbpmContext;
import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.identity.hibernate.IdentitySession;

public abstract class IdentityDbTestCase extends AbstractDbTestCase
{
  protected IdentitySession identitySession;

  protected void tearDown() throws Exception
  {
    super.tearDown();
    if (hasLeftOverRecords())
    {
      // We exit because subsequent tests start in an undefined state
      System.exit(1);
    }
  }

  private boolean hasLeftOverRecords()
  {
    boolean foundLeftOvers = false;
    JbpmContext jbpmContext = getJbpmConfiguration().createJbpmContext();
    Session session = jbpmContext.getSession();
    try
    {
      if (session.createQuery("from " + Group.class.getName()).list().size() > 0)
      {
        System.err.println("FIXME: left over Group after: " + getShortName());
        foundLeftOvers = true;
      }
      if (session.createQuery("from " + User.class.getName()).list().size() > 0)
      {
        System.err.println("FIXME: left over User after: " + getShortName());
        foundLeftOvers = true;
      }
    }
    catch (Exception ex)
    {
      System.err.println("FIXME: cannot query left overs: " + ex);
    }
    finally
    {
      jbpmContext.close();
    }
    return foundLeftOvers;
  }
  
  protected void initializeMembers()
  {
    super.initializeMembers();
    identitySession = new IdentitySession(session);
  }

  protected User saveAndReload(User user)
  {
    identitySession.saveUser(user);
    newTransaction();
    return identitySession.loadUser(user.getId());
  }

  protected Group saveAndReload(Group group)
  {
    identitySession.saveGroup(group);
    newTransaction();
    return identitySession.loadGroup(group.getId());
  }
  
  protected void deleteGroup(long groupId)
  {
    Group group = identitySession.loadGroup(groupId);
    identitySession.deleteGroup(group);
  }
  
  protected void deleteUser(long userId)
  {
    User user = identitySession.loadUser(userId);
    identitySession.deleteUser(user);
  }
}
