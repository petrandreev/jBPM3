package org.jbpm.util;

import java.util.Date;

public abstract class Clock {
  
  public static DateGenerator dateGenerator = new DefaultDateGenerator();
  
  public interface DateGenerator {
    Date getCurrentTime();
  }
  
  public static class DefaultDateGenerator implements DateGenerator {
    public Date getCurrentTime() {
      return new Date();
    }
  }
  
  public static Date getCurrentTime() {
    return dateGenerator.getCurrentTime();
  }
}
