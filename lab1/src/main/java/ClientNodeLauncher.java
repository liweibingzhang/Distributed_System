import impl.ClientImpl;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Scanner;

public class ClientNodeLauncher {
    public static void main(String[] args) {
        ClientImpl client = new ClientImpl(args);
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("Enter a command: ");
            String input = scanner.nextLine();
            String[] tokens = input.split(" ");
            String command = tokens[0];
            switch (command) {
                case "open":
                    String filePath = tokens[1];
                    String modeStatus = tokens[2];
                    int mode = -1;
                    if (modeStatus.equals("rw")) {
                        mode = 0b11;
                    } else if (modeStatus.equals("r")) {
                        mode = 0b01;
                    } else if (modeStatus.equals("w")) {
                        mode = 0b10;
                    } else {
                        System.out.println("Usage: open <file_path> <mode>");
                        break;
                    }
                    if (mode == -1) {
                        System.out.println("Usage: open <file_path> <mode>");
                        break;
                    }
                    int fd = client.open(filePath, mode);
                    if (fd == -1) {
                        System.out.println("Open failed!");
                    } else {
                        System.out.println("INFO: fd=" + fd);
                    }
                    break;
                case "read":
                    int read_fd = Integer.parseInt(tokens[1]);
                    byte[] data = client.read(read_fd);
                    if (data == null) {
                        System.out.println("Cannot Read!");
                        break;
                    } else {
                        System.out.println(new String(data));
                    }
                    break;
                case "append":
                    int DataNodeId = Integer.parseInt(tokens[1]);
                    String tmp = String.join(" ", Arrays.copyOfRange(tokens, 2, tokens.length));
                    client.append(DataNodeId, tmp.getBytes());
                    System.out.println("INFO: write done");
                    break;
                case "close":
                    int close_fd = Integer.parseInt(tokens[1]);
                    client.close(close_fd);
                    System.out.println("INFO: fd " + close_fd + " closed");
                    break;
                case "exit":
                    System.out.println("INFO: bye");
                    System.exit(0);
                    break;
                default:
                    System.out.println("Invalid command!");
            }
        }
    }
}