package org.jbpm.job.executor;

import java.io.Serializable;
import java.util.Date;

/** @deprecated no use for this class */
public class JobHistoryEntry implements Serializable {

  private static final long serialVersionUID = 1L;

  Date executionTime;
  String jobDescription;
  String exception;
}
