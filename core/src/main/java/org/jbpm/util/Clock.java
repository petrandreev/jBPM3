package org.jbpm.util;

import java.util.Date;

import org.jbpm.JbpmConfiguration.Configs;

public class Clock {

  private Clock() {
    // hide default constructor to prevent instantiation
  }

  /**
   * @deprecated set configuration entry <code>jbpm.date.generator</code>
   * instead
   */
  public static DateGenerator dateGenerator;

  public interface DateGenerator {
    Date getCurrentTime();
  }

  /**
   * @deprecated leave configuration entry <code>jbpm.date.generator</code>
   * unset instead
   */
  public static class DefaultDateGenerator implements DateGenerator {
    public Date getCurrentTime() {
      return new Date();
    }
  }

  public static Date getCurrentTime() {
    if (dateGenerator != null) return dateGenerator.getCurrentTime();

    if (Configs.hasObject("jbpm.date.generator")) {
      DateGenerator generator = (DateGenerator) Configs.getObject("jbpm.date.generator");
      return generator.getCurrentTime();
    }

    return new Date();
  }
}
