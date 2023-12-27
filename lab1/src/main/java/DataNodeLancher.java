import impl.DataNodeImpl;

public class DataNodeLancher
{
    public static void main(String[] args)
    {
        try{
            DataNodeImpl dataNodeServant = new DataNodeImpl(args);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
