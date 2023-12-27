package utils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import com.google.gson.Gson;

public class MetaDataInfo
{
    private int size;
    private String filepath;
    private ArrayList<BlockInfo> blockInfos;
    private TimeInfo timeinfo;
    public MetaDataInfo(){}
    public MetaDataInfo(String filepath)
    {
        this.timeinfo = new TimeInfo();
        this.filepath = filepath;
        this.size = 0;
        this.blockInfos = new ArrayList<>();
        this.timeinfo.createTime = LocalDateTime.now();
        this.timeinfo.lastAccessTime = LocalDateTime.now();
        this.timeinfo.lastModifiedTime = LocalDateTime.now();
    }
    public MetaDataInfo(int size, String filepath, ArrayList<BlockInfo> blockInfos, TimeInfo timeinfo){
        this.size = size;
        this.filepath = filepath;
        this.blockInfos = blockInfos;
        this.timeinfo = timeinfo;
    }
    public int getSize()
    {
        return size;
    }

    public void setSize(int size)
    {
        this.size = size;
    }

    public ArrayList<BlockInfo> getBlockInfos()
    {
        return blockInfos;
    }

    public void setBlockInfos(ArrayList<BlockInfo> blockInfos)
    {
        this.blockInfos = blockInfos;
    }
    public void addBlockInfos(BlockInfo blockInfo)
    {
        this.blockInfos.add(blockInfo);
    }
    public LocalDateTime getLastModifiedTime()
    {
        return timeinfo.lastModifiedTime;
    }

    public void setLastModifiedTime(LocalDateTime lastModifiedTime)
    {
        this.timeinfo.lastModifiedTime = lastModifiedTime;
    }

    public LocalDateTime getLastAccessTime()
    {
        return timeinfo.lastAccessTime;
    }

    public void setLastAccessTime(LocalDateTime lastAccessTime)
    {
        this.timeinfo.lastAccessTime = lastAccessTime;
    }
    @Override
    public String toString(){
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    public static MetaDataInfo Convert_fromString(String str){
        Gson gson = new Gson();
        return gson.fromJson(str, MetaDataInfo.class);
    }
}
