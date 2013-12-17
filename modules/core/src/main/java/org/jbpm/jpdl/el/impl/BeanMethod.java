package org.jbpm.jpdl.el.impl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class BeanMethod {

  Method method;
  
  public BeanMethod(Method method) {
    this.method = method;
  }

  public Object invoke(Object object) throws InvocationTargetException, IllegalArgumentException, IllegalAccessException {
    log.debug("invoking expression method '"+method.getName()+"'");
    return method.invoke(object, (Object[]) null);
  }
  
  private static Log log = LogFactory.getLog(BeanMethod.class);
}
