public interface ProcessArchiveParser {

  void writeToArchive(
		ProcessDefinition processDefinition, ProcessArchive archive);
	
  ProcessDefinition readFromArchive(
		ProcessArchive archive, ProcessDefinition processDefinition);

}