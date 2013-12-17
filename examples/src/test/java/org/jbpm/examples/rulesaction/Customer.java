/*
 * JBoss, Home of Professional Open Source
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jbpm.examples.rulesaction;

import java.io.Serializable;

public class Customer implements Serializable {

  long id;

  String name;
  Integer region;
  Integer age;
  Long income;

  private static final long serialVersionUID = 1L;

  public Customer() {
  }

  public Customer(String name, Integer region, Integer age, Long income) {
    setName(name);
    setRegion(region);
    setAge(age);
    setIncome(income);
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Integer getRegion() {
    return region;
  }

  public void setRegion(Integer region) {
    this.region = region;
  }

  public Integer getAge() {
    return age;
  }

  public void setAge(Integer age) {
    this.age = age;
  }

  public Long getIncome() {
    return income;
  }

  public void setIncome(Long income) {
    this.income = income;
  }

}
