package org.jbpm.command;

import org.jbpm.graph.exe.Token;

/**
 * Unlocks the given token. Either the correct lockOwner has 
 * to be provided (otherwise an exception is thrown) or
 * NO lockOwner is provided at all, then the lock is 
 * removed "with force". 
 * 
 * @author bernd.ruecker@camunda.com
 */
public class UnlockTokenCommand extends AbstractTokenBaseCommand {
	
	private static final long serialVersionUID = 1L;
	
	private String lockOwner;

	public UnlockTokenCommand() {		
	}

  public Object execute(Token token)
  {
		if (lockOwner!=null) {
			token.unlock(lockOwner);
		}
		else {
			// requires newer jbpm version, see https://jira.jboss.org/jira/browse/JBPM-1888
			token.forceUnlock();
		}
		return token;
	}
	
	public String getAdditionalToStringInformation() {
		return (lockOwner!=null ? ";lockOwner=" + lockOwner : ""); 
	}
	
	public String getLockOwner() {
		return lockOwner;
	}

	public void setLockOwner(String lockOwner) {
		this.lockOwner = lockOwner;
	}

  // methods for fluent programming

  public UnlockTokenCommand lockOwner(String lockOwner) {
    setLockOwner(lockOwner);
    return this;
  }

}
