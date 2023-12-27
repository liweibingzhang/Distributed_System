package impl;
//TODO: your implementation
import api.*;
import org.omg.CORBA.ORB;
import org.omg.CosNaming.NameComponent;
import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextExtHelper;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.locks.ReadWriteLock;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import utils.DataNodeInfo;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DataNodeImpl extends DataNodePOA {

    private Map<Integer, byte[]> blockStorage;

    transient ReadWriteLock readwritelock = new ReentrantReadWriteLock();
    
    private NameNode namenode;

    private int DataNodeIndex;

    public DataNodeImpl() {
        try{
            String[] args = {};
            Properties properties = new Properties();
            properties.put("org.omg.CORBA.ORBInitialHost", "127.0.0.1");
            properties.put("org.omg.CORBA.ORBInitialPort", "1050");
            ORB orb = ORB.init(args, properties);
            org.omg.CORBA.Object ref = orb.resolve_initial_references("NameService");
            NamingContextExt nRef = NamingContextExtHelper.narrow(ref);
            namenode = NameNodeHelper.narrow(nRef.resolve_str("NameNode"));
            this.DataNodeIndex = namenode.getNextAndIncrementDataNodeId();
            System.out.println("NameNode " + this.DataNodeIndex + " is obtained");
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    public DataNodeImpl(String []args){
        try{
            Properties properties = new Properties();
            properties.put("org.omg.CORBA.ORBInitialHost", "127.0.0.1");
            properties.put("org.omg.CORBA.ORBInitialPort", "1050");
            
            ORB orb = ORB.init(args, properties);
            POA rootpoa = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
            rootpoa.the_POAManager().activate();

            org.omg.CORBA.Object ref = orb.resolve_initial_references("NameService");
            NamingContextExt nRef = NamingContextExtHelper.narrow(ref);

            namenode = NameNodeHelper.narrow(nRef.resolve_str("NameNode"));
            this.DataNodeIndex = namenode.getNextAndIncrementDataNodeId();

            org.omg.CORBA.Object ref1 = rootpoa.servant_to_reference(this);
            DataNode dref = DataNodeHelper.narrow(ref1);

            NameComponent[] path = nRef.to_name("DataNode"+this.DataNodeIndex);
            nRef.rebind(path, dref);
            System.out.println("DataNode " + this.DataNodeIndex + " is obtained");
            orb.run();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
    /**
     * Read specified block and return bytes
     * @param block_id 要读取的数据块标识符
     * @return bytes that are read from the block
     */
    @Override
    public byte[] read(int block_id) {
        //Lock it
        readwritelock.readLock().lock();
        String filepath = "./data/" + DataNodeIndex + "/" + block_id;
        try (FileInputStream fileInputStream = new FileInputStream(filepath);
            //Create memory output stream
            ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            int read_info;
            byte[] buf = new byte[DataNodeInfo.BLOCK_SIZE];
            // Write in the output stream
            while ((read_info = fileInputStream.read(buf)) != -1) {
                output.write(buf, 0, read_info);
            }
            readwritelock.readLock().unlock();
            return output.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        readwritelock.readLock().unlock();
        return new byte[0];
    }

    /**
     * Append bytes to the end of the specified block.
     * @param block_id 要追加数据的数据块标识符
     * @param bytes 要追加到数据块的字节数组
     */
    @Override
    public void append(int block_id, byte[] bytes) {
        //Lock it
        readwritelock.writeLock().lock();
        String filePath = "./data/"+ DataNodeIndex + "/" + block_id;
        try {
            Path path = Paths.get("./data/" + DataNodeIndex);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
            try (FileOutputStream output = new FileOutputStream(filePath, true)) {
                // append bytes to the file
                output.write(bytes);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        readwritelock.writeLock().unlock();
    }


    @Override
    public int randomBlockId() { 
        // Create a random blockID
        // Random rand = new Random();
        // int blockId;
        // do {
        //     blockId = rand.nextInt(Integer.MAX_VALUE);
        // } while (blockStorage.containsKey(blockId));
        // return blockId;
        return namenode.getNextAndIncrementBlockId(this.DataNodeIndex);
    }

}
