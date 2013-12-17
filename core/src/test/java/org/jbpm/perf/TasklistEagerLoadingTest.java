package org.jbpm.perf;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.taskmgmt.exe.TaskInstance;

public class TasklistEagerLoadingTest extends AbstractDbTestCase {

  private List taskInstanceIds = new ArrayList();

  public void testTasklistEagerLoading() {
    for (int i = 0; i < 20; i++) {
      TaskInstance taskInstance = new TaskInstance("task " + i);
      taskInstance.setActorId("johndoe");
      save(taskInstance);
    }
    newTransaction();
    try {
      assertEquals(20, jbpmContext.getTaskList("johndoe").size());
    }
    finally {
      deleteTaskInstances();
    }
  }

  public void testPooledTasklistEagerLoading() {
    for (int i = 0; i < 20; i++) {
      TaskInstance taskInstance = new TaskInstance("group task " + i);
      taskInstance.setPooledActors(new String[] { "group" + i });
      save(taskInstance);
    }
    for (int i = 0; i < 20; i++) {
      TaskInstance taskInstance = new TaskInstance("task " + i);
      taskInstance.setPooledActors(new String[] { "johndoe", "bachelors", "partyanimals",
          "wildwomen" });
      save(taskInstance);
    }
    newTransaction();
    try {
      assertEquals(20, jbpmContext.getGroupTaskList(Collections.singletonList("johndoe")).size());
    }
    finally {
      deleteTaskInstances();
    }
  }

  private void save(TaskInstance taskInstance) {
    Serializable id = session.save(taskInstance);
    taskInstanceIds.add(id);
  }

  private void deleteTaskInstances() {
    for (Iterator i = taskInstanceIds.iterator(); i.hasNext();) {
      Serializable id = (Serializable) i.next();
      Object taskInstance = session.load(TaskInstance.class, id);
      session.delete(taskInstance);
    }
  }
}
