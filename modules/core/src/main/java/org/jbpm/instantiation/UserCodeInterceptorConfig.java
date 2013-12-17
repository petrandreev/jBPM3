package org.jbpm.instantiation;

public abstract class UserCodeInterceptorConfig {

  public static UserCodeInterceptor userCodeInterceptor = null;

  public static void setUserCodeInterceptor(UserCodeInterceptor interceptor) {
    userCodeInterceptor = interceptor;
  }
}
