package org.jbpm.ejb.impl;

import javax.jms.JMSException;
import javax.jms.Message;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbpm.command.Command;

/**
 * Message-driven bean that listens for {@link Message messages} containing
 * a reference to a pending {@linkplain Job job. to support asynchronous 
 * continuations.
 * 
 * The message must have a property called <code>jobId</code> of type 
 * <code>long</code> which references a pending <literal>Job</literal> 
 * in the database. The message body, if any, is ignored.
 * 
 * <h3>Environment</h3>
 * 
 * This bean extends the {@link CommandListenerBean} and inherits its
 * environment entries and resources available for customization.
 * 
 * @author Tom Baeyens
 * @author Alejandro Guizar
 */
public class JobListenerBean extends CommandListenerBean {

  private static final long serialVersionUID = 1L;

  protected Command extractCommand(Message message) throws JMSException {
    Command command = null;
    // checking for availability of the jobId property
    log.debug("getting job id from jms message...");
    Long jobId = (Long) message.getObjectProperty("jobId");
    if (jobId != null) {
      log.debug("retrieved jobId '"+jobId+"' via jms message");
      command = new ExecuteJobCommand(jobId.longValue());
    } else {
      log.warn("ignoring message '"+message+"' cause it doesn't have property jobId");
    }
    return command;
  }

  private static Log log = LogFactory.getLog(JobListenerBean.class);
}
