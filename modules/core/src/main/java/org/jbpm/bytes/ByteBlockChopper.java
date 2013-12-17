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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jbpm.JbpmConfiguration;

/**
 * is used by {@link org.jbpm.bytes.ByteArray} to chop a 
 * byte arrays into a list of chunks and glue them back together. 
 */
public class ByteBlockChopper {

  private static final Log log = LogFactory.getLog(ByteBlockChopper.class);

  private ByteBlockChopper() {
    // hide default constructor to prevent instantiation
  }

  public static List chopItUp(byte[] byteArray) {
    List bytes = null;
    if (byteArray != null) {
      int blockSize = JbpmConfiguration.Configs.getInt("jbpm.byte.block.size");
      int byteCount = byteArray.length;
      if (byteCount > blockSize) {
        log.debug("chopping " + byteCount + " bytes");
        bytes = new ArrayList();
        int offset;
        for (offset = 0; byteCount - offset > blockSize; offset += blockSize) {
          bytes.add(subArray(byteArray, offset, blockSize));
        }
        bytes.add(subArray(byteArray, offset, byteCount - offset));
      }
      else if (byteCount > 0) {
        log.debug("no need to chop " + byteCount + " bytes");
        bytes = Collections.singletonList(byteArray);
      }
    }
    return bytes;
  }

  private static byte[] subArray(byte[] array, int offset, int length) {
    byte[] subArray = new byte[length];
    System.arraycopy(array, offset, subArray, 0, length);
    log.debug("chopped " + length + " bytes beggining at " + offset);
    return subArray;
  }

  public static byte[] glueChopsBackTogether(List byteBlocks) {
    byte[] byteArray = null;
    
    if (byteBlocks != null) {
      int blockCount = byteBlocks.size();
      switch (blockCount) {
      case 0:
        break;
      case 1:
        byteArray = (byte[]) byteBlocks.get(0);
        log.debug("no need to glue " + byteArray.length + " bytes");
        break;
      default:
        int blockSize = JbpmConfiguration.Configs.getInt("jbpm.byte.block.size");
        byte[] lastBlock = (byte[]) byteBlocks.get(blockCount - 1);
        int byteCount = blockSize * (blockCount - 1) + lastBlock.length;
        log.debug("gluing " + byteCount + " bytes");

        byteArray = new byte[byteCount];
        int offset = 0;
        for (int i = 0, n = blockCount; i < n; i++) {
          byte[] block = (byte[]) byteBlocks.get(i);
          int length = block.length;
          System.arraycopy(block, 0, byteArray, offset, length);
          log.debug("glued " + length + " bytes beggining at " + offset);
          // JBPM-702 sybase truncates trailing zeros
          if (length < blockSize && i < n-1) {
            Arrays.fill(byteArray, offset + length, offset + blockSize, (byte) 0);
            log.debug("zero filled " + (blockSize - length) + " trailing bytes");
            offset += blockSize;
          }
          else {
            offset += length;            
          }
        }
      }
    }
    return byteArray;
  }
}
