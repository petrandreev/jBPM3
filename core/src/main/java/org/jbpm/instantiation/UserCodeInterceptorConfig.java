package org.jbpm.instantiation;

import org.jbpm.JbpmConfiguration.Configs;

public class UserCodeInterceptorConfig {

  private static UserCodeInterceptor userCodeInterceptor;

  private UserCodeInterceptorConfig() {
    // prevent instantiation
  }

  public static UserCodeInterceptor getUserCodeInterceptor() {
    return userCodeInterceptor != null ? userCodeInterceptor
      : Configs.hasObject("jbpm.user.code.interceptor") ?
        (UserCodeInterceptor) Configs.getObject("jbpm.user.code.interceptor") : null;
  }

  /** @deprecated Use the configuration entry <code>jbpm.user.code.interceptor</code> instead */
  public static void setUserCodeInterceptor(UserCodeInterceptor interceptor) {
    userCodeInterceptor = interceptor;
  }
}
