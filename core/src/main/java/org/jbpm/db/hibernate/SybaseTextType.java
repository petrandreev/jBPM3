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
package org.jbpm.db.hibernate;

import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.type.Type;
import org.hibernate.usertype.EnhancedUserType;
import org.hibernate.util.EqualsHelper;
import org.hibernate.util.StringHelper;

/**
 * Replacement for {@link org.hibernate.type.TextType} made to work around a <em>feature</em> in the
 * jConnect driver when setting a text parameter to <code>null</code>. Specifically, the call:
 * 
 * <pre>
 * PreparedStatement st;
 * st.setNull(index, Types.CLOB);
 * </pre>
 * 
 * throws an SQLException with SQL state "JZ0SL" and reason "Unsupported SQL type".
 * 
 * @see <a href="https://jira.jboss.org/jira/browse/JBPM-1818">JBPM-1818</a>
 * @author Alejandro Guizar
 */
public class SybaseTextType implements EnhancedUserType, Serializable {

  private transient Log log;
  private static final boolean IS_VALUE_TRACING_ENABLED =
      LogFactory.getLog(StringHelper.qualifier(Type.class.getName())).isTraceEnabled();

  private static final long serialVersionUID = 1L;

  private Log log() {
    if (log == null) {
      log = LogFactory.getLog(getClass());
    }
    return log;
  }

  public Object assemble(Serializable cached, Object owner) throws HibernateException {
    return cached != null ? deepCopy(cached) : null;
  }

  public Object deepCopy(Object value) throws HibernateException {
    return value;
  }

  public Serializable disassemble(Object value) throws HibernateException {
    return value != null ? (Serializable) deepCopy(value) : null;
  }

  public boolean equals(Object x, Object y) throws HibernateException {
    return EqualsHelper.equals(x, y);
  }

  public int hashCode(Object x) throws HibernateException {
    return x.hashCode();
  }

  public boolean isMutable() {
    return false;
  }

  public Object nullSafeGet(ResultSet rs, String[] names, Object owner) throws HibernateException,
      SQLException {
    return nullSafeGet(rs, names[0]);
  }

  public Object nullSafeGet(ResultSet rs, String name) throws HibernateException, SQLException {
    try {
      Object value = get(rs, name);
      if (value == null) {
        if (IS_VALUE_TRACING_ENABLED) {
          log().trace("returning null as column: " + name);
        }
        return null;
      }
      else {
        if (IS_VALUE_TRACING_ENABLED) {
          log().trace("returning '" + toString(value) + "' as column: " + name);
        }
        return value;
      }
    }
    catch (RuntimeException re) {
      log().info("could not read column value from result set: " + name + "; " + re.getMessage());
      throw re;
    }
    catch (SQLException se) {
      log().info("could not read column value from result set: " + name + "; " + se.getMessage());
      throw se;
    }
  }

  public Object get(ResultSet rs, String name) throws HibernateException, SQLException {
    // retrieve the value of the designated column in the current row of the
    // result set as a character reader
    Reader charReader = rs.getCharacterStream(name);

    // if the corresponding SQL value is NULL, the reader we got is NULL as well
    if (charReader == null || rs.wasNull()) return null;

    // Fetch Reader content up to the end - and put characters in a StringBuffer
    StringBuffer sbuf = new StringBuffer();
    try {
      char[] cbuf = new char[1024];
      for (int amountRead; (amountRead = charReader.read(cbuf)) != -1;) {
        sbuf.append(cbuf, 0, amountRead);
      }
    }
    catch (IOException ioe) {
      throw new HibernateException("IOException occurred reading text", ioe);
    }
    finally {
      try {
        charReader.close();
      }
      catch (IOException e) {
        throw new HibernateException("IOException occurred closing stream", e);
      }
    }

    // Return StringBuffer content as a large String
    return sbuf.toString();
  }

  public void nullSafeSet(PreparedStatement st, Object value, int index) throws HibernateException,
      SQLException {
    try {
      if (value == null) {
        if (IS_VALUE_TRACING_ENABLED) {
          log().trace("binding null to parameter: " + index);
        }

        setNull(st, index);
      }
      else {
        if (IS_VALUE_TRACING_ENABLED) {
          log().trace("binding '" + toString(value) + "' to parameter: " + index);
        }

        set(st, value, index);
      }
    }
    catch (RuntimeException re) {
      log().info("could not bind value '" +
          nullSafeToString(value) +
          "' to parameter: " +
          index +
          "; " +
          re.getMessage());
      throw re;
    }
    catch (SQLException se) {
      log().info("could not bind value '" +
          nullSafeToString(value) +
          "' to parameter: " +
          index +
          "; " +
          se.getMessage());
      throw se;
    }
  }

  public void set(PreparedStatement st, Object value, int index) throws HibernateException,
      SQLException {
    String str = (String) value;
    st.setCharacterStream(index, new StringReader(str), str.length());
  }

  public void setNull(PreparedStatement st, int index) throws HibernateException, SQLException {
    // JBPM-1818: workaround for SQL state JZ0SL: "Unsupported SQL type" with jConnect
    st.setCharacterStream(index, null, 0);
  }

  public Object replace(Object original, Object target, Object owner) throws HibernateException {
    return original;
  }

  public Class returnedClass() {
    return String.class;
  }

  public int[] sqlTypes() {
    return new int[] { sqlType() };
  }

  public int sqlType() {
    return Types.CLOB;
  }

  public String objectToSQLString(Object value) {
    return '\'' + (String) value + '\'';
  }

  public Object fromXMLString(String xml) {
    return xml != null && xml.length() > 0 ? fromStringValue(xml) : null;
  }

  public String toXMLString(Object value) {
    return toString(value);
  }

  public String nullSafeToString(Object value) throws HibernateException {
    return value != null ? toString(value) : null;
  }

  public String toString(Object value) {
    return (String) value;
  }

  public Object fromStringValue(String xml) {
    return xml;
  }
}
