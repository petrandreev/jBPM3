/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, JBoss Inc., and individual contributors as indicated
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
package org.jbpm.jbpm3234;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import junit.framework.TestCase;

import org.jbpm.mail.Mail;

/**
 * Potential API breaks in JBPM: Mail methods.
 * 
 * @see <a href="https://jira.jboss.org/jira/browse/JBPM-3234">JBPM-3234</a>
 */
public class JBPM3234Test extends TestCase {

  protected void setUp() throws Exception {
  }

  private static String SEND_METHOD_NAME = "send";
  private static String GET_RECIPIENTS_METHOD_NAME = "getRecipients";
  private static String GET_CC_RECIPIENTS_METHOD_NAME = "getCcRecipients";
  private static String GET_BCC_RECIPIENTS_METHOD_NAME = "getBccRecipients";
  
  public void testMailRecipientMethods() {
    Method [] methods = Mail.class.getDeclaredMethods();
   
    HashMap nameMethodMap = new HashMap();
    nameMethodMap.put(GET_RECIPIENTS_METHOD_NAME, null);
    nameMethodMap.put(GET_CC_RECIPIENTS_METHOD_NAME, null);
    nameMethodMap.put(GET_BCC_RECIPIENTS_METHOD_NAME, null);
   
    Class [] classes = null;
    
    for( int i = 0; i < methods.length; ++i ) { 
      if( nameMethodMap.containsKey(methods[i].getName()) ) { 
        classes = methods[i].getParameterTypes();
        assertTrue("Unexpected parameters for [" + methods[i].getName() + "] :" + classes.length + " parameters.", 
          classes == null || classes.length == 0);
        Class returnTypeClass = methods[i].getReturnType();
        assertTrue("Unexpected return type for [" + methods[i].getName() + "] :" + returnTypeClass.getName() + ", etc.", 
          returnTypeClass != null && List.class.equals(returnTypeClass));
        nameMethodMap.put(methods[i].getName(), methods[i]);
      } 
    }

    Iterator iter = nameMethodMap.keySet().iterator();
    while(iter.hasNext()) { 
      String methodName = (String) iter.next();
      assertTrue("Method [" + methodName + "] not found in class" + Mail.class.getName(), nameMethodMap.get(methodName) != null);
    }
    
  }
  
  public void testMailSendMethods() { 
    Method [] methods = Mail.class.getDeclaredMethods();
    
    HashSet sendMethods = new HashSet();
    for( int i = 0; i < methods.length; ++i ) { 
      if( SEND_METHOD_NAME.equals(methods[i].getName()) ) { 
        sendMethods.add(methods[i]);
      }
    }
   
    // The following are the correct parameter signatures for the expected send() methods. 
    HashSet sendMethodParameters = new HashSet();
    
    Class [] methodParams = {Properties.class,String.class,List.class,String.class,String.class};
    sendMethodParameters.add(methodParams);
    Class [] methodParams2 = {Properties.class,String.class,List.class,List.class,String.class,String.class};
    sendMethodParameters.add(methodParams2);

    HashSet correctMethods = new HashSet();
    Iterator iter = sendMethods.iterator();
    int countCorrectSendMethods = 0;
    while( iter.hasNext()) { 
      Method sendMethod = (Method) iter.next();
      Class [] methodParamClasses = sendMethod.getParameterTypes();
     
      Iterator smpIter = sendMethodParameters.iterator();
      while(smpIter.hasNext()) { 
        Object og = smpIter.next();
        Class [] correctParamClasses = (Class []) og;
        if( Arrays.equals(correctParamClasses, methodParamClasses) ) { 
          countCorrectSendMethods++;
          smpIter.remove();
          correctMethods.add(sendMethod);
        }
      }
    }
    
    assertTrue(countCorrectSendMethods + " correct send methods found, instead of 2", countCorrectSendMethods == 2);
    
   
    iter = correctMethods.iterator();
    while(iter.hasNext()) { 
      Method sendMethod = (Method) iter.next();
      int methodModifiers = sendMethod.getModifiers();
      assertTrue("Method is not static.", Modifier.isStatic(methodModifiers));
      assertTrue("Method is not public.", Modifier.isPublic(methodModifiers));
      Class returnTypeClass = sendMethod.getReturnType();
     
      // Java bug!! 
      String voidClassname = returnTypeClass.getName();
      if( voidClassname.lastIndexOf('.') >= 0 ) { 
        voidClassname = voidClassname.substring(voidClassname.lastIndexOf('.'));
      }
      assertTrue("Method does not return void.", "Void".equalsIgnoreCase(voidClassname));
    }
  }
  
  
}