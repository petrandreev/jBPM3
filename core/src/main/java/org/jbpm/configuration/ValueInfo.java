package org.jbpm.configuration;

public class ValueInfo implements ObjectInfo {

  private static final long serialVersionUID = 1L;

  private final String name;
  private final Object value;

  public ValueInfo(String name, Object value) {
    this.name = name;
    this.value = value;
  }

  public Object createObject(ObjectFactoryImpl objectFactory) {
    return value;
  }

  public String getName() {
    return name;
  }

  public boolean hasName() {
    return name != null;
  }

  public boolean isSingleton() {
    return true;
  }
}
