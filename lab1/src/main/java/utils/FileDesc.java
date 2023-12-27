package utils;


import com.google.gson.Gson;

//TODO: According to your design, complete the FileDesc class, which wraps the information returned by NameNode open()
public class FileDesc {
    /* the id should be assigned uniquely during the lifetime of NameNode,
     * so that NameNode can know which client's open has over at close
     * e.g., on nameNode1
     * client1 opened file "Hello.txt" with mode 'w' , and retrieved a FileDesc with 0x889
     * client2 tries opening the same file "Hello.txt" with mode 'w' , and since the 0x889 is not closed yet, the return
     * value of open() is null.
     * after a while client1 call close() with the FileDesc of id 0x889.
     * client2 tries again and get a new FileDesc with a new id 0x88a
     */
    private int id;
    private int mode;
    private MetaDataInfo metadatainfo;
    private String filePath;
    public FileDesc(int id, int mode, MetaDataInfo metaDatainfo,  String filePath) {
        this.mode = mode;
        this.id = id;
        this.metadatainfo = metaDatainfo;
        this.filePath = filePath;
    }

    public int getId()
    {
        return id;
    }


    public int getMode()
    {
        return mode;
    }
    public void setMetaData(MetaDataInfo metadatainfo)
    {
        this.metadatainfo = metadatainfo;
    }

    public MetaDataInfo getMetaDataInfo()
    {
        return metadatainfo;
    }

    public String getFilePath()
    {
        return filePath;
    }

    @Override
    public String toString(){
        Gson gson = new Gson();
        String jsonString = gson.toJson(this);
        return jsonString;
    }

    public static FileDesc Convert_fromString(String str){
        Gson gson = new Gson();
        FileDesc fileDesc = gson.fromJson(str, FileDesc.class);
        return fileDesc;
    }

}