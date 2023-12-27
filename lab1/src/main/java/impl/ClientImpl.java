package impl;
//TODO: your implementation
import api.*;
import org.omg.CORBA.ORB;
import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextExtHelper;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import utils.BlockInfo;
import utils.DataNodeInfo;
import utils.FileDesc;
import utils.MetaDataInfo;
import java.util.Properties;
import api.Client;
public class ClientImpl implements Client{
    private NameNode namenode;
    private DataNode[] datanodes = new DataNode[DataNodeInfo.MAX_DATA_NODE];

    private List<FileDesc> file_list = new ArrayList<>();
    public ClientImpl(){
        try
        {
            String[] args = {};
            Properties properties = new Properties();
            properties.put("org.omg.CORBA.ORBInitialHost", "127.0.0.1");
            properties.put("org.omg.CORBA.ORBInitialPort", "1050");
            ORB orb = ORB.init(args, properties);
            org.omg.CORBA.Object ref = orb.resolve_initial_references("NameService");
            NamingContextExt nref = NamingContextExtHelper.narrow(ref);
            namenode = NameNodeHelper.narrow(nref.resolve_str("NameNode"));
            System.out.println("NameNode is obtained");
            for (int i = 0; i < namenode.getDataNodeCount(); i++)
            {
                datanodes[i] = DataNodeHelper.narrow(nref.resolve_str("DataNode" + i));
                System.out.println("DataNode" + i + " is obtained");
            }
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }
    public ClientImpl(String[] args){
        try
        {
            Properties properties = new Properties();
            properties.put("org.omg.CORBA.ORBInitialHost", "127.0.0.1");
            properties.put("org.omg.CORBA.ORBInitialPort", "1050");
            ORB orb = ORB.init(args, properties);
            org.omg.CORBA.Object ref = orb.resolve_initial_references("NameService");
            NamingContextExt nref = NamingContextExtHelper.narrow(ref);
            namenode = NameNodeHelper.narrow(nref.resolve_str("NameNode"));
            System.out.println("NameNode is obtained");
            for (int i = 0; i < namenode.getDataNodeCount(); i++)
            {
                datanodes[i] = DataNodeHelper.narrow(nref.resolve_str("DataNode" + i));
                System.out.println("DataNode" + i + " is obtained");
            }
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }
    private boolean canRead(FileDesc fileDesc){
        return (fileDesc.getMode() & 0b01) != 0;
    }
    private boolean canWrite(FileDesc fileDesc){
        return (fileDesc.getMode() & 0b10) != 0;
    }
    /**
     * 打开文件并获取元数据。
     * @param filepath 文件路径
     * @param mode 文件打开模式
     * @return 如果打开成功，返回文件描述符的 ID；否则返回 -1
     */
    @Override
    public int open(String filepath, int mode) {
        String fileInfo = namenode.file_open(filepath, mode);
        if (fileInfo.isEmpty())
        {
            return -1;
        }
        FileDesc fileDesc = FileDesc.Convert_fromString(fileInfo);
        this.file_list.add(fileDesc);
        return fileDesc.getId();
    }
    /**
     * 在指定文件描述符的文件末尾追加字节数组。
     * @param fd 文件描述符
     * @param bytes 要追加的字节数组
     */
    @Override
    public void append(int fd, byte[] bytes)
    {
        FileDesc fileDesc = getFileDesc(fd);
        if (fileDesc == null)
        {
            System.out.println("fd is null");
            return;
        }
        if (!canWrite(fileDesc))
        {
            System.out.println("fd cannot be written");
            return;
        }
        MetaDataInfo metadata = appendBytes(bytes, fileDesc);
        //更新metadata的time和size
        metadata.setLastModifiedTime(LocalDateTime.now());
        metadata.setLastAccessTime(LocalDateTime.now());
        metadata.setSize(metadata.getSize() + bytes.length);
    }
    /**
     * 在文件描述符对应的文件末尾追加字节数组，并更新元数据信息。
     * @param bytes 要追加的字节数组
     * @param fileDesc 文件描述符
     * @return 更新后的元数据信息
     */
    private MetaDataInfo appendBytes(byte[] bytes, FileDesc fileDesc)
    {
        MetaDataInfo metaData = fileDesc.getMetaDataInfo();
        ArrayList<BlockInfo> blockInfos = metaData.getBlockInfos();
        BlockInfo la_blockInfo = blockInfos.get(blockInfos.size() - 1);
        int la_size = getLastSize(metaData.getSize());
        int DataNodeId = la_blockInfo.getDataNodeId();
        int blockId = la_blockInfo.getBlockId();
        if (DataNodeInfo.BLOCK_SIZE - la_size >= bytes.length)
        {
            // 如果当前块可以容纳全部字节数组，则直接在当前块追加
            datanodes[DataNodeId].append(blockId, bytes);
        } else
        {
            // 否则，拆分字节数组并追加到多个块中
            byte[] part_bytes = Arrays.copyOfRange(bytes, 0, DataNodeInfo.BLOCK_SIZE - la_size);
            datanodes[DataNodeId].append(blockId, part_bytes);
            int rest_len = bytes.length - part_bytes.length;
            int part_len = part_bytes.length;
            while (rest_len >= DataNodeInfo.BLOCK_SIZE)
            {
                // 获取下一个数据节点和块ID
                int NextDataNodeId = namenode.getRandomDataNodeId();
                int NextBlockId = namenode.getNextAndIncrementBlockId(NextDataNodeId);

                // 创建新的块信息并添加到元数据中
                BlockInfo blockInfo = new BlockInfo(NextDataNodeId, NextBlockId);
                metaData.addBlockInfos(blockInfo);

                // 获取下一个块的字节数组并追加到数据节点
                byte[] blockBytes = Arrays.copyOfRange(bytes, part_len, part_len + DataNodeInfo.BLOCK_SIZE);
                datanodes[NextDataNodeId].append(NextBlockId, blockBytes);

                // 更新位置和剩余长度
                part_len += DataNodeInfo.BLOCK_SIZE;
                rest_len -= DataNodeInfo.BLOCK_SIZE;
            }
            if (rest_len > 0)
            {
                // 处理剩余的字节数组
                int NextDataNodeId = namenode.getRandomDataNodeId();
                int NextBlockId = namenode.getNextAndIncrementBlockId(NextDataNodeId);

                // 创建新的块信息并添加到元数据中
                BlockInfo blockInfo = new BlockInfo(NextDataNodeId, NextBlockId);
                metaData.addBlockInfos(blockInfo);

                // 获取剩余块的字节数组并追加到数据节点
                byte[] blockBytes = Arrays.copyOfRange(bytes, part_len, part_len + rest_len);
                datanodes[NextDataNodeId].append(NextBlockId, blockBytes);
            }
        }
        return metaData;
    }
    /**
     * 读取文件内容并返回字节数组。
     * @param fd 文件描述符
     * @return 如果成功读取文件，返回文件内容的字节数组；否则返回空字节数组
     */
    @Override
    public byte[] read(int fd)
    {
        FileDesc fileDesc = getFileDesc(fd);
        if (fileDesc == null)
        {
            System.out.println("fd doesn't exist");
            return "".getBytes(StandardCharsets.UTF_8);
        }
        if(!canRead(fileDesc)){
            return null;
        }
        //判断是否有元数据
        MetaDataInfo metadata = getFileDesc(fd).getMetaDataInfo();
        if(metadata == null){
            return "".getBytes(StandardCharsets.UTF_8);
        }

        //获得latest的元数据
        //MetaDataInfo latest_metadata = MetaDataInfo.Convert_fromString(namenode.getLatestMetaData(fileDesc.toString()));

        //修改元数据的时间戳
        metadata.setLastAccessTime(LocalDateTime.now());
        fileDesc.setMetaData(metadata);

        //返回文件内容的字节数组
        return getBytes(metadata);
    }
    /**
     * 根据元数据信息读取文件内容并返回字节数组。
     * @param metaData 元数据信息
     * @return 文件内容的字节数组
     */
    private byte[] getBytes(MetaDataInfo metaData)
    {
        ArrayList<BlockInfo> blockInfos = metaData.getBlockInfos();
        ArrayList<byte[]> bytesarray = new ArrayList<>();
        int total_len=0;

        // 遍历每个块信息，读取数据并存储在动态字节数组中
        for (BlockInfo blockInfo : blockInfos)
        {
            int DataNodeIdx = blockInfo.getDataNodeId();
            int BlockIdx = blockInfo.getBlockId();
            byte[] bytes = datanodes[DataNodeIdx].read(BlockIdx);
            bytesarray.add(bytes);
            total_len+=bytes.length;
        }

        // 合并动态字节数组为一个固定长度的结果数组
        int ptr = 0;
        byte[] result = new byte[total_len];
        for (byte[] block : bytesarray)
        {
            System.arraycopy(block, 0, result, ptr, block.length);
            ptr += block.length;
        }
        return result;
    }
    /**
     * 获取最后一个块的剩余空间大小。
     * @param size 文件总大小
     * @return 最后一个块的剩余空间大小
     */
    private int getLastSize(long size)
    {
        if (size == 0)
        {
            return 0;
        }
        int lastSize = (int) (size % DataNodeInfo.BLOCK_SIZE);
        if (lastSize == 0)
        {
            lastSize = DataNodeInfo.BLOCK_SIZE;
        }
        return lastSize;
    }
    /**
     * 根据文件描述符查找并返回对应的文件描述对象。
     * @param fd 文件描述符
     * @return 文件描述对象，如果未找到返回 null
     */
    private FileDesc getFileDesc(int fd)
    {
        for (FileDesc tmp : file_list)
        {
            if (tmp.getId() == fd)
            {
                return tmp;
            }
        }
        return null;
    }
    /**
     * 关闭文件并执行清理操作。
     * @param fd 文件描述符
     */
    @Override
    public void close(int fd)
    {
        FileDesc fileDesc = getFileDesc(fd);
        if (fileDesc == null)
        {
            System.out.println("fd doesn't exist");
            return;
        }
        //将元数据写回到NameNode并将fd删除
        namenode.file_close(fileDesc.toString());
        file_list.remove(fileDesc);
    }
}
