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
package org.jbpm.graph.exe;

import org.jbpm.AbstractJbpmTestCase;
import org.jbpm.graph.def.ProcessDefinition;

public class TokenNameTest extends AbstractJbpmTestCase {
  
  public void testFindRoot() {
    ProcessDefinition pd = new ProcessDefinition(
      new String[]{"start-state start",
                   "fork f1",
                   "state a",
                   "fork f2",
                   "state b",
                   "state c"}, 
      new String[]{"start --> f1",
                   "f1 --a--> a",
                   "f1 --f2--> f2",
                   "f2 --b--> b",
                   "f2 --c--> c"});
    
    ProcessInstance pi = new ProcessInstance(pd);
    pi.signal();

    // now we have the following tree of tokens
    // at the right, the full name of the token is presented.
    //
    // root-token   --> /
    //  +- a        --> /a
    //  +- f2       --> /f2
    //      +- b    --> /f2/b
    //      +- c    --> /f2/c
    
    Token root = pi.getRootToken();
    Token tokenA = root.getChild("a");
    Token tokenF2 = root.getChild("f2");
    Token tokenF2B = tokenF2.getChild("b");
    Token tokenF2C = tokenF2.getChild("c");
    
    assertEquals("/", root.getFullName());
    assertEquals("/a", tokenA.getFullName());
    assertEquals("/f2", tokenF2.getFullName());
    assertEquals("/f2/b", tokenF2B.getFullName());
    assertEquals("/f2/c", tokenF2C.getFullName());
    
    assertSame( root, pi.findToken( "/" ) );
    assertSame( root, pi.findToken( "" ) );
    assertSame( root, pi.findToken( "." ) );
    assertSame( root, tokenA.findToken( ".." ) );
    assertSame( root, tokenA.findToken( "../." ) );

    assertSame( tokenA, pi.findToken( "/a" ) );
    assertSame( tokenA, tokenF2C.findToken( "/a" ) );
    assertSame( tokenA, pi.findToken( "a" ) );

    assertSame( tokenF2, pi.findToken( "f2" ) );
    assertSame( tokenF2, pi.findToken( "/f2" ) );
    assertSame( tokenF2, tokenF2C.findToken( ".." ) );

    assertSame( tokenF2B, pi.findToken( "f2/b" ) );
    assertSame( tokenF2B, pi.findToken( "/f2/b" ) );

    assertNull( pi.findToken( null ) );
    assertNull( pi.findToken( "non-existing-token-name" ) );
    assertNull( pi.findToken( "/a/non-existing-token-name" ) );
    assertNull( pi.findToken( ".." ) );
    assertNull( pi.findToken( "/.." ) );
  }
}
