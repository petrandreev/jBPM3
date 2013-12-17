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
package org.jbpm.bytes;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jbpm.util.ArrayUtil;

/**
 * is a persistable array of bytes. While there is no generic way of storing blobs that is
 * supported by many databases, all databases are able to handle small chunks of bytes properly.
 * It is the responsibility of this class to chop the large byte array into small chunks of 1K
 * (and combine the chunks again in the reverse way). Hibernate will persist the list of
 * byte-chunks in the database.
 * 
 * ByteArray is used in process variableInstances and in the file module (that stores the
 * non-parsed process archive files).
 */
public class ByteArray implements Serializable {

  private static final long serialVersionUID = 1L;

  private long id;
  protected String name;
  protected List byteBlocks;

  public ByteArray() {
  }

  public ByteArray(byte[] bytes) {
    this.byteBlocks = ByteBlockChopper.chopItUp(bytes);
  }

  public ByteArray(String name, byte[] bytes) {
    this(bytes);
    this.name = name;
  }

  public ByteArray(ByteArray other) {
    List otherByteBlocks = other.getByteBlocks();
    if (otherByteBlocks != null) {
      this.byteBlocks = new ArrayList(otherByteBlocks);
    }
    this.name = other.name;
  }

  public void update(ByteArray value) {
    List otherByteBlocks = (value != null ? value.getByteBlocks() : null);
    // Different than constructor:
    // 1. clear to empty list. 
    this.byteBlocks.clear();
    if (otherByteBlocks != null) {
      // 2. use addAll in order to work as much as possible with JPA/ORM 
      this.byteBlocks.addAll(otherByteBlocks);
    }
    this.name = (value != null ? value.name : null );
  }
  
  public byte[] getBytes() {
    return ByteBlockChopper.glueChopsBackTogether(byteBlocks);
  }

  public long getId() {
    return id;
  }

  public boolean equals(Object o) {
    if (o == this) return true;
    if (!(o instanceof ByteArray)) return false;

    ByteArray other = (ByteArray) o;
    if (id != 0 && id == other.getId()) return true;

    List byteBlocks = this.byteBlocks;
    List otherByteBlocks = other.getByteBlocks();
    int n = byteBlocks.size();
    if (n != otherByteBlocks.size()) return false;

    for (int i = 0; i < n; i++) {
      byte[] byteBlock = (byte[]) byteBlocks.get(i);
      byte[] otherByteBlock = (byte[]) otherByteBlocks.get(i);
      if (!Arrays.equals(byteBlock, otherByteBlock)) return false;
    }
    return true;
  }

  public int hashCode() {
    if (byteBlocks == null) return 0;

    int result = 1;
    for (int i = 0, n = byteBlocks.size(); i < n; i++) {
      byte[] byteBlock = (byte[]) byteBlocks.get(i);
      result = 31 * result + ArrayUtil.hashCode(byteBlock);
    }
    return result;
  }

  public List getByteBlocks() {
    return byteBlocks;
  }


}
