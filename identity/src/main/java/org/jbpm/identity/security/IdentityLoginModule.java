/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jbpm.identity.security;

import java.io.IOException;
import java.util.*;
import javax.security.auth.*;
import javax.security.auth.callback.*;
import javax.security.auth.login.*;
import javax.security.auth.spi.*;
import org.jbpm.identity.*;

/**
 * jaas login module that, in case of successfull verification, adds the 
 * {@link org.jbpm.identity.User} as a principal to the subject.  In case 
 * of successfull verification, the {@link Username} and {@link Password}
 * will be associated as public and private credentials respectively.
 */
public class IdentityLoginModule implements LoginModule {
  
  Subject subject = null;
  CallbackHandler callbackHandler = null;
  Map sharedState = null;
  Map options = null;
  
  /**
   * @inject
   */
  IdentityService identityService = null;
  
  Object validatedUserId = null;
  String validatedPwd = null;

  public void initialize(Subject subject, CallbackHandler callbackHandler, Map sharedState, Map options) {
    this.subject = subject;
    this.callbackHandler = callbackHandler;
    this.sharedState = sharedState;
    this.options = options;
  }

  public boolean login() throws LoginException {

    // get userName and password
    NameCallback nameCallback = new NameCallback(null);
    PasswordCallback passwordCallback = new PasswordCallback(null,false);
    try {
      callbackHandler.handle(new Callback[]{nameCallback, passwordCallback});
    } catch (IOException e) {
      LoginException loginException = new LoginException("callback failed");
      loginException.initCause(e);
      throw loginException;
    }
    catch (UnsupportedCallbackException e) {
      // should not happen
      throw new AssertionError(e);
    }
    String userName = nameCallback.getName();
    String pwd = new String(passwordCallback.getPassword());
    
    // validate the userName and password with the injected identity session
    Object userId = identityService.verify(userName, pwd);

    boolean success = (userId!=null);
    // if userName matched the given password
    if (success) {
      // save the user id and the password in the 
      // private state of this loginmodule
      validatedUserId = userId;
      validatedPwd = pwd; 
    } else {
      validatedUserId = null;
      validatedPwd = null;
    }

    return success;
  }

  public boolean commit() throws LoginException {
    
    User user = identityService.getUserById(validatedUserId);
    if (user==null) {
      throw new LoginException("no user for validated user id '"+validatedUserId);
    }
    
    // update the subject
    subject.getPrincipals().add(user);
    subject.getPrivateCredentials().add(new Username(user.getName()));
    subject.getPrivateCredentials().add(new Password(validatedPwd));
    
    // and update the authenticated user
    AuthenticatedUser.setAuthenticatedUser(user);

    return true;
  }

  public boolean abort() throws LoginException {
    return logout();
  }

  public boolean logout() throws LoginException {
    if(subject!= null){
      // TODO can we clear all or should this login module only clear the user it
      // has added to the set of principals in the commit ?
      subject.getPrincipals().clear();
      subject.getPublicCredentials().clear();
      subject.getPrivateCredentials().clear();
    }

    // and update the authenticated user
    AuthenticatedUser.setAuthenticatedUser(null);
    
    callbackHandler = null;
    sharedState = null;
    options = null;
    validatedUserId = null;
    validatedPwd = null;
    return true;
  }
}
