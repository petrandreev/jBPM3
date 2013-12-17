package org.jbpm.command;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.jbpm.util.ClassUtil;

public class CompositeCommand implements Command {

  private static final long serialVersionUID = 1L;

  List commands = null;

  public CompositeCommand(List commands) {
    this.commands = commands;
  }

  public Object execute(JbpmContext jbpmContext) throws Exception {
    List results = null;
    if (commands != null) {
      Object lastResult = null;
      results = new ArrayList(commands.size());
      Iterator iter = commands.iterator();
      while (iter.hasNext()) {
        Command command = (Command) iter.next();
        if (lastResult != null) {
          tryToInject(lastResult, command);
        }
        lastResult = command.execute(jbpmContext);
        results.add(lastResult);
      }
    }
    return results;
  }

  protected void tryToInject(Object lastResult, Command command) {
    Field field = findField(lastResult.getClass());
    if (field != null) {
      field.setAccessible(true);
      try {
        field.set(command, lastResult);
      }
      catch (Exception e) {
        throw new JbpmException("couldn't propagate composite command context", e);
      }
    }
  }

  protected Field findField(Class clazz) {
    Field field = null;
    int i = 0;
    Field[] fields = clazz.getDeclaredFields();
    while ((i < fields.length) && (field == null)) {
      Field candidate = fields[i];
      if ((candidate.getType().isAssignableFrom(clazz))
          && (candidate.getName().startsWith("previous"))) {
        field = candidate;
      }
      i++;
    }
    return field;
  }

  public String toString() {
    return ClassUtil.getSimpleName(getClass()) + commands;
  }

}
