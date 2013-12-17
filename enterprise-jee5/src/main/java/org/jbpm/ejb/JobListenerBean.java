package org.jbpm.ejb;

import javax.jms.JMSException;
import javax.jms.Message;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbpm.command.Command;
import org.jbpm.jms.ExecuteJobCommand;

/**
 * Message-driven bean that listens for {@link Message messages} containing a reference to a
 * pending {@linkplain org.jbpm.job.Job job} for asynchronous continuations.
 * <p>
 * The message must have a <code>long</code> property called <code>jobId</code> which identifies
 * a job in the database. The message body, if any, is ignored.
 * </p>
 * <h3>Environment</h3>
 * <p>
 * This bean inherits its environment entries and resources available for customization from
 * {@link CommandListenerBean}.
 * </p>
 * 
 * @author Alejandro Guizar
 */
public class JobListenerBean extends CommandListenerBean {

  private static final long serialVersionUID = 1L;
  private static final Log log = LogFactory.getLog(JobListenerBean.class);

  @Override
  protected Command extractCommand(Message message) throws JMSException {
    // check for jobId property
    if (message.propertyExists("jobId")) {
      long jobId = message.getLongProperty("jobId");
      return new ExecuteJobCommand(jobId);
    }
    else {
      // check for command object
      Command command = super.extractCommand(message);
      if (command != null) {
        return command;
      }
      else {
        log.warn("neither property jobId nor command found");
      }
    }
    return null;
  }
}
