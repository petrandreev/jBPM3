package org.jbpm.sim.tutorial;

import java.util.Date;

/**
 * This class represents the only entity we
 * need in this tutorial: the "return order", 
 * which has all information of what goods are returned from 
 * the customer
 * 
 * @author bernd.ruecker@camunda.com
 */
public class ReturnOrder {

  /**
   * the date the goods were originally bought
   */
  private Date dateOfPurchase;

  /**
   * the value of the returned goods for the customer,
   * the price he originally paid
   */
  private double goodsValue;
  
  /**
   * an estimation what resale value the goods have
   */
  private double estimatedResaleValue;

  public ReturnOrder(Date dateOfPurchase, double goodsValue, double estimatedResaleValue) {
    this.dateOfPurchase = dateOfPurchase;
    this.goodsValue = goodsValue;
    this.estimatedResaleValue = estimatedResaleValue;
  }
  
  public Date getDateOfPurchase() {
    return dateOfPurchase;
  }

  public void setDateOfPurchase(Date dateOfPurchase) {
    this.dateOfPurchase = dateOfPurchase;
  }

  public double getGoodsValue() {
    return goodsValue;
  }

  public void setGoodsValue(double goodsValue) {
    this.goodsValue = goodsValue;
  }

  public double getEstimatedResaleValue() {
    return estimatedResaleValue;
  }

  public void setEstimatedResaleValue(double estimatedResaleValue) {
    this.estimatedResaleValue = estimatedResaleValue;
  }
}
