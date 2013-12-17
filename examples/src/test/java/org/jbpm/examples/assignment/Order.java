/*
 * JBoss, Home of Professional Open Source
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jbpm.examples.assignment;

import java.io.Serializable;

public class Order implements Serializable {

  private long id;
  private Long totalAmount;

  private static final long serialVersionUID = 1L;

  public Order() {
  }

  public Order(long totalAmount) {
    setTotalAmount(new Long(totalAmount));
  }

  public long getId() {
    return id;
  }

  public Long getTotalAmount() {
    return totalAmount;
  }

  public void setTotalAmount(Long totalAmount) {
    this.totalAmount = totalAmount;
  }

}
