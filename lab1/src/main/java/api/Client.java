package api;

public interface Client {
    int open(String filepath, int mode);
    void append(int fd, byte[] bytes);
    byte[] read(int fd);
    void close(int fd);
}
