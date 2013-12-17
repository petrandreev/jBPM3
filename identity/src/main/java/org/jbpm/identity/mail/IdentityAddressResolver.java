package org.jbpm.identity.mail;

import org.jbpm.identity.User;
import org.jbpm.identity.hibernate.IdentitySession;
import org.jbpm.mail.AddressResolver;
import org.jbpm.svc.Service;

/**
 * translates actorIds into email addresses with the jBPM identity module. Only
 * user actorIds are resolved to their email addresses. Group actorIds return
 * null.
 */
public class IdentityAddressResolver implements AddressResolver {

  private static final long serialVersionUID = 1L;

  public Object resolveAddress(String actorId) {
    String emailAddress = null;
    IdentitySession identitySession = new IdentitySession();
    User user = identitySession.getUserByName(actorId);
    if (user != null) {
      emailAddress = user.getEmail();
    }
    return emailAddress;
  }

  /**
   * @deprecated this address resolver does not fit the {@linkplain Service
   * service} model.
   */
  public Service openService() {
    return null;
  }

  /**
   * @deprecated this address resolver does not fit the {@linkplain Service
   * service} model.
   */
  public void close() {
  }
}
