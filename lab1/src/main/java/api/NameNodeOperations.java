package api;


public interface NameNodeOperations 
{

  //TODO: complete the interface design
  String file_open (String filepath, int mode);
  void file_close (String filepath);
  int getRandomDataNodeId ();
  int getDataNodeCount ();
  int getNextAndIncrementDataNodeId ();
  int getNextAndIncrementBlockId (int DataNodeId);
  int getNextBlockId (int DataNodeId);
  String getLatestMetaData (String fileDescInfo);
} // interface NameNodeOperations
