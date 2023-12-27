package utils;


import java.io.Serializable;

public class BlockInfo implements Serializable
{

    private final int DataNodeId;
    private final int BlockId;

    public BlockInfo(int dataNodeId, int blockId)
    {
        this.DataNodeId = dataNodeId;
        this.BlockId = blockId;
    }
    public int getDataNodeId()
    {
        return DataNodeId;
    }

    public int getBlockId()
    {
        return BlockId;
    }
}
