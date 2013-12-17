package org.jbpm.db.hibernate;

// $Id: StringMax.java 3992 2009-02-21 13:48:56Z thomas.diesler@jboss.com $

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;

import org.hibernate.type.StringType;
import org.hibernate.usertype.ParameterizedType;
import org.jbpm.JbpmException;

/**
 * A custom type that truncates string values
 */
public class StringMax extends StringType implements ParameterizedType
{
  private static final long serialVersionUID = 1L;

  int length = 4000;

  public void set(PreparedStatement st, Object value, int index) throws SQLException
  {
    String string = (String)value;
    if (string != null && string.length() > length)
    {
      string = string.substring(0, length);
    }
    super.set(st, string, index);
  }

  public void setParameterValues(Properties parameters)
  {
    if (parameters != null && parameters.containsKey("length"))
    {
      String propval = parameters.getProperty("length");
      try
      {
        length = Integer.parseInt(propval);
      }
      catch (NumberFormatException e)
      {
        throw new JbpmException("hibernate column type 'string_max' can't parse value '" + propval + "' as a max length.  default is 4000.", e);
      }
    }
  }
}
