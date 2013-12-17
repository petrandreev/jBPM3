package org.jbpm.job;

import org.jbpm.JbpmContext;
import org.jbpm.graph.exe.Token;

/**
 * Signals a token asynchronously.
 *
 * @see <a href="https://jira.jboss.org/browse/JBPM-2948">JBPM-2948</a>
 * @author Brad Davis
 */
public class SignalTokenJob extends Job {

	private static final long serialVersionUID = 1L;

  public SignalTokenJob() {
    // default constructor
	}

	public SignalTokenJob(Token token) {
		super(token);
	}

	public boolean execute(JbpmContext jbpmContext) throws Exception {
		getToken().signal();
		return true;
	}

	public String toString() {
		return "SignalTokenJob(" + getId() + ',' + getToken() + ')';
	}

}
