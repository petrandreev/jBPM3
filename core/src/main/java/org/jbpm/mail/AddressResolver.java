package org.jbpm.mail;

import java.io.Serializable;

/**
 * translates actorId's into email addresses.
 */
public interface AddressResolver extends Serializable {

  /**
   * calculates the email address(es) for a given actorId.
   * This method is allowed to return a String or a Collection of Strings 
   * representing email addresses.
   */
  Object resolveAddress(String actorId);
}
