public interface TaskControllerHandler extends Serializable {
  void initializeTaskVariables(TaskInstance taskInstance, ContextInstance contextInstance, Token token);
  void submitTaskVariables(TaskInstance taskInstance, ContextInstance contextInstance, Token token);
}