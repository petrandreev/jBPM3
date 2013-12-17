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
package org.jbpm.identity.assignment;

/**
 * chops an actor assignment expression into terms.
 */
public class TermTokenizer {

  private String expression = null;
  private int index = 0;
  
  public TermTokenizer(String expression) {
    this.expression = expression;
  }

  public boolean hasMoreTerms() {
    return (index!=-1);
  }
  
  public String nextTerm() {
    String term = null;
    int startIndex = index;
    index = expression.indexOf("-->", index);
    if (index!=-1) {
      term = expression.substring(startIndex, index).trim();
      index+=3;
    } else {
      term = expression.substring(startIndex).trim();
    }
    return term;
  }
}
