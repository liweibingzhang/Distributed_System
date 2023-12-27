package api;


public abstract class NameNodePOA extends org.omg.PortableServer.Servant
 implements api.NameNodeOperations, org.omg.CORBA.portable.InvokeHandler
{

  // Constructors

  private static java.util.Hashtable _methods = new java.util.Hashtable ();
  static
  {
    _methods.put ("file_open", new java.lang.Integer (0));
    _methods.put ("file_close", new java.lang.Integer (1));
    _methods.put ("getRandomDataNodeId", new java.lang.Integer (2));
    _methods.put ("getDataNodeCount", new java.lang.Integer (3));
    _methods.put ("getNextAndIncrementDataNodeId", new java.lang.Integer (4));
    _methods.put ("getNextAndIncrementBlockId", new java.lang.Integer (5));
    _methods.put ("getNextBlockId", new java.lang.Integer (6));
    _methods.put ("getLatestMetaData", new java.lang.Integer (7));
  }

  public org.omg.CORBA.portable.OutputStream _invoke (String $method,
                                org.omg.CORBA.portable.InputStream in,
                                org.omg.CORBA.portable.ResponseHandler $rh)
  {
    org.omg.CORBA.portable.OutputStream out = null;
    java.lang.Integer __method = (java.lang.Integer)_methods.get ($method);
    if (__method == null)
      throw new org.omg.CORBA.BAD_OPERATION (0, org.omg.CORBA.CompletionStatus.COMPLETED_MAYBE);

    switch (__method.intValue ())
    {

  //TODO: complete the interface design
       case 0:  // api/NameNode/file_open
       {
         String filepath = in.read_string ();
         int mode = in.read_long ();
         String $result = null;
         $result = this.file_open (filepath, mode);
         out = $rh.createReply();
         out.write_string ($result);
         break;
       }

       case 1:  // api/NameNode/file_close
       {
         String filepath = in.read_string ();
         this.file_close (filepath);
         out = $rh.createReply();
         break;
       }

       case 2:  // api/NameNode/getRandomDataNodeId
       {
         int $result = (int)0;
         $result = this.getRandomDataNodeId ();
         out = $rh.createReply();
         out.write_long ($result);
         break;
       }

       case 3:  // api/NameNode/getDataNodeCount
       {
         int $result = (int)0;
         $result = this.getDataNodeCount ();
         out = $rh.createReply();
         out.write_long ($result);
         break;
       }

       case 4:  // api/NameNode/getNextAndIncrementDataNodeId
       {
         int $result = (int)0;
         $result = this.getNextAndIncrementDataNodeId ();
         out = $rh.createReply();
         out.write_long ($result);
         break;
       }

       case 5:  // api/NameNode/getNextAndIncrementBlockId
       {
         int DataNodeId = in.read_long ();
         int $result = (int)0;
         $result = this.getNextAndIncrementBlockId (DataNodeId);
         out = $rh.createReply();
         out.write_long ($result);
         break;
       }

       case 6:  // api/NameNode/getNextBlockId
       {
         int DataNodeId = in.read_long ();
         int $result = (int)0;
         $result = this.getNextBlockId (DataNodeId);
         out = $rh.createReply();
         out.write_long ($result);
         break;
       }

       case 7:  // api/NameNode/getLatestMetaData
       {
         String fileDescInfo = in.read_string ();
         String $result = null;
         $result = this.getLatestMetaData (fileDescInfo);
         out = $rh.createReply();
         out.write_string ($result);
         break;
       }

       default:
         throw new org.omg.CORBA.BAD_OPERATION (0, org.omg.CORBA.CompletionStatus.COMPLETED_MAYBE);
    }

    return out;
  } // _invoke

  // Type-specific CORBA::Object operations
  private static String[] __ids = {
    "IDL:api/NameNode:1.0"};

  public String[] _all_interfaces (org.omg.PortableServer.POA poa, byte[] objectId)
  {
    return (String[])__ids.clone ();
  }

  public NameNode _this() 
  {
    return NameNodeHelper.narrow(
    super._this_object());
  }

  public NameNode _this(org.omg.CORBA.ORB orb) 
  {
    return NameNodeHelper.narrow(
    super._this_object(orb));
  }


} // class NameNodePOA
