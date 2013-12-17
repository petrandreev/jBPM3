package org.jbpm.jpdl.el.impl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class BeanMethod {

  private Method method;

  public BeanMethod(Method method) {
    this.method = method;
  }

  public Object invoke(Object object) throws IllegalAccessException, InvocationTargetException {
    if (log.isDebugEnabled()) log.debug("invoking " + method);
    return method.invoke(object, (Object[]) null);
  }

  private static final Log log = LogFactory.getLog(BeanMethod.class);
}
