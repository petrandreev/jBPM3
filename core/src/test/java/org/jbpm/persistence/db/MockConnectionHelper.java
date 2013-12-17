package org.jbpm.persistence.db;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;

public class MockConnectionHelper implements InvocationHandler{
	
  public Connection createMockConnection() {
	  return (Connection)Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[] { Connection.class }, this);
  }

  boolean wasClosed = false;
  boolean wasRolledBack = false;
  boolean wasCommitted = false;
	  
  public Object invoke(Object proxy, Method method, Object[] args)
      throws Throwable {
	if ("close".equals(method.getName())) {
	  wasClosed = true;
	  return null;
    } else if ("isClosed".equals(method.getName())) {
	  return new Boolean(wasClosed);
	} else if ("commit".equals(method.getName())) {
	  wasCommitted = true;
	  return null;
	} else if ("rollback".equals(method.getName())) {
	  wasRolledBack = true;
	  return null;
	} else if ("toString".equals(method.getName())) {
	  return toString();	
	} else throw new UnsupportedOperationException();
  }

}
