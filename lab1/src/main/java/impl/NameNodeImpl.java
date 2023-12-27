package impl;
//TODO: your implementation

import api.NameNodePOA;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import utils.BlockInfo;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import utils.DataNodeInfo;
import utils.FileDesc;
import utils.MetaDataInfo;
import java.io.*;
import java.util.concurrent.locks.ReentrantLock;

public class NameNodeImpl extends NameNodePOA
{
    private int[] NextBlockId = new int[DataNodeInfo.MAX_DATA_NODE];
    private int DataNodeCount;
    transient ReentrantLock DataNodeIdLock = new ReentrantLock();
    transient ReentrantLock BlockIdLock = new ReentrantLock();
    transient ReentrantLock CloseLock = new ReentrantLock();
    transient ReentrantLock OpenLock = new ReentrantLock();
    private Map<String, MetaDataInfo> FileMap = new HashMap<>();
    private List<FileDesc> openfd = new ArrayList<>();
    private int currentfd = 0;

    public NameNodeImpl()
    {
        DataNodeCount = 0;
    }
    public NameNodeImpl(String[] args)
    {
        String re = readFromDisk();
        if(re == null){
            DataNodeCount = 0;
            return;
        }
        NameNodeImpl namenode = fromString(re);
        this.NextBlockId = namenode.NextBlockId;
        this.openfd = namenode.openfd;
        this.FileMap = namenode.FileMap;
        this.currentfd = namenode.currentfd;
        DataNodeCount = 0;
    }
    private boolean canWrite(FileDesc fileDesc){
        return (fileDesc.getMode() & 0b10) != 0;
    }
    private boolean canRead(FileDesc fileDesc){
        return (fileDesc.getMode() & 0b01) != 0;
    }
    /**
     * 打开文件并返回文件元数据信息的字符串表示。
     * @param filepath 文件路径
     * @param mode 文件打开模式
     * @return 如果打开成功，返回文件描述字符串；如果文件不存在或打开模式不符合条件，返回空字符串
     */
    @Override
    public String file_open(String filepath, int mode)
    {
        //Lock it
        OpenLock.lock();

        // 获取文件的元数据信息
        MetaDataInfo metadata = FileMap.get(filepath);

        if (metadata == null && (mode & 0b10) != 0)
        {
            // 如果文件不存在且是写入模式，则创建新的元数据信息和块信息
            MetaDataInfo metadata_new = new MetaDataInfo(filepath);
            //Give it a block
            int Random_dataNodeId = getRandomDataNodeId();
            int Next_blockId = getNextAndIncrementBlockId(Random_dataNodeId);
            BlockInfo blockinfo = new BlockInfo(Random_dataNodeId, Next_blockId);
            metadata_new.addBlockInfos(blockinfo);
            FileMap.put(filepath, metadata_new);
            currentfd++;
            FileDesc fileDesc = new FileDesc(currentfd, mode, metadata_new, filepath);
            openfd.add(fileDesc);
            return fileDesc.toString();
        } else if (metadata == null)
        {
            // 如果文件不存在但不是写入模式，则返回空字符串
            return "";
        } else if ((mode & 0b10) != 0)
        {
            // 如果是写入模式，检查文件是否已经以写入模式打开，如果是则返回空字符串
            for (FileDesc fileDesc : openfd)
            {
                if (fileDesc.getMode() == 0b10 && fileDesc.getFilePath().equals(filepath))
                {
                    return "";
                }
            }
            // 分配新的文件描述符并返回字符串表示
            currentfd++;
            FileDesc fileDesc = new FileDesc(currentfd, mode, metadata, filepath);
            openfd.add(fileDesc);
            return fileDesc.toString();
        } else if ((mode & 0b10) == 0)
        {
            // 如果是读取模式，分配新的文件描述符并返回字符串表示
            currentfd++;
            FileDesc fileDesc = new FileDesc(currentfd, mode, metadata, filepath);
            openfd.add(fileDesc);
            return fileDesc.toString();
        }
        OpenLock.unlock();
        return null;
    }
    @Override
    public int getNextBlockId(int dataNodeId)
    {
        BlockIdLock.lock();
        int id = NextBlockId[dataNodeId];
        BlockIdLock.unlock();
        return id;
    }

    @Override
    public int getNextAndIncrementBlockId(int DataNodeId)
    {
        BlockIdLock.lock();
        int blockId = NextBlockId[DataNodeId]++;
        BlockIdLock.unlock();
        return blockId;
    }
    /**
     * 关闭文件并执行相应的清理和更新操作。
     * @param fileInfo 文件描述字符串
     */
    @Override
    public void file_close(String fileInfo)
    {
        CloseLock.lock();
        FileDesc fileDesc = FileDesc.Convert_fromString(fileInfo);
        removeFromOpenFd(fileDesc);
        //TODO:考虑并发安全问题
        //处理写入权限的情况
        if (canWrite(fileDesc))
        {
            MetaDataInfo metadata = fileDesc.getMetaDataInfo();
            MetaDataInfo pre_metadata = FileMap.get(fileDesc.getFilePath());
            pre_metadata.setSize(metadata.getSize());
            pre_metadata.setLastModifiedTime(metadata.getLastModifiedTime());

            // 如果新的访问时间比存储的访问时间晚，则更新存储的访问时间
            if(pre_metadata.getLastAccessTime().isBefore(metadata.getLastAccessTime())){
                pre_metadata.setLastAccessTime(metadata.getLastAccessTime());
            }
            pre_metadata.setBlockInfos(metadata.getBlockInfos());
        }
        // 处理读取权限的情况
        if (canRead(fileDesc))
        {
            MetaDataInfo metadata = fileDesc.getMetaDataInfo();
            MetaDataInfo pre_metadata = FileMap.get(fileDesc.getFilePath());

            // 如果新的访问时间比存储的访问时间晚，则更新存储的访问时间
            if(pre_metadata.getLastAccessTime().isBefore(metadata.getLastAccessTime())){
                pre_metadata.setLastAccessTime(metadata.getLastAccessTime());
            }
        }
        writeToDisk();
        CloseLock.unlock();
    }
    /**
     * 从打开文件列表中移除指定的文件描述符。
     * @param fileDesc 要移除的文件描述符
     */
    private void removeFromOpenFd(FileDesc fileDesc)
    {
        for (int i = 0; i < openfd.size(); i++)
        {
            if (openfd.get(i).getId() == fileDesc.getId())
            {
                openfd.remove(i);
                break;
            }
        }
    }
    /**
     * 获取文件最新的元数据信息。
     * @param fileDescInfo 文件描述符字符串
     * @return 最新的元数据信息字符串
     */
    public String getLatestMetaData(String fileDescInfo){
        CloseLock.lock();
        FileDesc fileDesc = FileDesc.Convert_fromString(fileDescInfo);
        MetaDataInfo metadata = fileDesc.getMetaDataInfo();
        MetaDataInfo pre_metadata = FileMap.get(fileDesc.getFilePath());
        if(pre_metadata.getLastModifiedTime().isAfter(metadata.getLastModifiedTime())){
            CloseLock.unlock();
            return pre_metadata.toString();
        }
        CloseLock.unlock();
        return fileDesc.getMetaDataInfo().toString();
    }
    public int getDataNodeCount(){
        DataNodeIdLock.lock();
        int cnt = DataNodeCount;
        DataNodeIdLock.unlock();
        return cnt;
    }
    @Override
    public int getNextAndIncrementDataNodeId()
    {
        DataNodeIdLock.lock();
        int cnt = DataNodeCount++;
        DataNodeIdLock.unlock();
        return cnt;
    }
    @Override
    public String toString(){
        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();
        return gson.toJson(this);
    }
    private NameNodeImpl fromString(String str){
        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();
        return gson.fromJson(str, NameNodeImpl.class);
    }
    @Override
    public int getRandomDataNodeId()
    {	//根据已有的dataNode数量，来产生随机数
        return (int) (Math.random() % (DataNodeCount));
    }
    private void writeToDisk(){
        String filePath = "./FSData/FSImage";
        byte[] bytes = this.toString().getBytes();
        try {
            Path directoryPath = Paths.get("./FSData");
            if (!Files.exists(directoryPath)) {
                Files.createDirectories(directoryPath);
            }
            try (RandomAccessFile file = new RandomAccessFile(filePath, "rw")) {
                file.setLength(0);
                // 将字节数组写入文件
                file.write(bytes);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private String readFromDisk(){
        String filePath = "./FSData/FSImage";
        try (FileInputStream fileInputStream = new FileInputStream(filePath);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buf = new byte[DataNodeInfo.BLOCK_SIZE];
            int bytesRead;
            while ((bytesRead = fileInputStream.read(buf)) != -1) {
                output.write(buf, 0, bytesRead);
            }
            return output.toString();
        } catch (IOException e) {
            System.out.println("Previous FSImage doesn't exist");
        }
        return null;
    }
}
