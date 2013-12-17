package org.jbpm.command;

import org.jbpm.util.ClassUtil;

/**
 * Base class for all internal commands. Implements some convenience methods for {@link Command}s
 * and generic toString method.
 * 
 * @author bernd.ruecker@camunda.com
 */
public abstract class AbstractBaseCommand implements Command {

  private static final long serialVersionUID = 1L;

  public String toString() {
    String className = ClassUtil.getSimpleName(getClass());
    String additionalInfo = getAdditionalToStringInformation();
    return additionalInfo != null ? className + '[' + additionalInfo + ']' : className;
  }

  protected String getAdditionalToStringInformation() {
    return null;
  }
}
