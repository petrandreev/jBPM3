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
package org.jbpm.mock;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;

public class Jndi {
  
  static Map repository;
  
  public static void initialize() {
    System.setProperty("java.naming.factory.initial", "org.jbpm.mock.Jndi$MockInitialContextFactory");
    repository = new HashMap();
  }

  public static void reset() {
    System.getProperties().remove("java.naming.factory.initial");
    repository = null;
  }

  public static void putInJndi(String name, Object object) {
    repository.put(name, object);
  }

  public static class MockInitialContextFactory implements InitialContextFactory {
    public Context getInitialContext(Hashtable environment) throws NamingException {
      return new MockContext();
    }
  }

  public static class MockContext implements Context {
    public Object lookup(String name) throws NamingException {
      return repository.get(name);
    }
    public void unbind(String name) throws NamingException {
      repository.remove(name);
    }
    public void bind(String name, Object obj) throws NamingException {
      if (repository.containsKey(name)) {
        throw new NameAlreadyBoundException(name);
      }
      repository.put(name, obj);
    }
    public void rebind(String name, Object obj) throws NamingException {
      repository.put(name, obj);
    }
    public void close() throws NamingException {
      repository.clear();
    }

    public String getNameInNamespace() throws NamingException {
      throw new UnsupportedOperationException();
    }
    public void destroySubcontext(String name) throws NamingException {
      throw new UnsupportedOperationException();
    }
    public Hashtable getEnvironment() throws NamingException {
      throw new UnsupportedOperationException();
    }
    public void destroySubcontext(Name name) throws NamingException {
      throw new UnsupportedOperationException();
    }
    public void unbind(Name name) throws NamingException {
      throw new UnsupportedOperationException();
    }
    public Object lookupLink(String name) throws NamingException {
      throw new UnsupportedOperationException();
    }
    public Object removeFromEnvironment(String propName) throws NamingException {
      throw new UnsupportedOperationException();
    }
    public Object lookup(Name name) throws NamingException {
      throw new UnsupportedOperationException();
    }
    public Object lookupLink(Name name) throws NamingException {
      throw new UnsupportedOperationException();
    }
    public void bind(Name name, Object obj) throws NamingException {
      throw new UnsupportedOperationException();
    }
    public void rebind(Name name, Object obj) throws NamingException {
      throw new UnsupportedOperationException();
    }
    public void rename(String oldName, String newName) throws NamingException {
      throw new UnsupportedOperationException();
    }
    public Context createSubcontext(String name) throws NamingException {
      throw new UnsupportedOperationException();
    }
    public Context createSubcontext(Name name) throws NamingException {
      throw new UnsupportedOperationException();
    }
    public void rename(Name oldName, Name newName) throws NamingException {
      throw new UnsupportedOperationException();
    }
    public NameParser getNameParser(String name) throws NamingException {
      throw new UnsupportedOperationException();
    }
    public NameParser getNameParser(Name name) throws NamingException {
      throw new UnsupportedOperationException();
    }
    public NamingEnumeration list(String name) throws NamingException {
      throw new UnsupportedOperationException();
    }
    public NamingEnumeration listBindings(String name) throws NamingException {
      throw new UnsupportedOperationException();
    }
    public NamingEnumeration list(Name name) throws NamingException {
      throw new UnsupportedOperationException();
    }
    public NamingEnumeration listBindings(Name name) throws NamingException {
      throw new UnsupportedOperationException();
    }
    public Object addToEnvironment(String propName, Object propVal) throws NamingException {
      throw new UnsupportedOperationException();
    }
    public String composeName(String name, String prefix) throws NamingException {
      throw new UnsupportedOperationException();
    }
    public Name composeName(Name name, Name prefix) throws NamingException {
      throw new UnsupportedOperationException();
    }
  }
}
