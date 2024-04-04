package util;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;

public class TCPClient {
  private Socket socket;
  private PrintWriter out;
  private BufferedReader in;

  public TCPClient(String ipAddress, int port,  int timeout) throws IOException {
    socket = new Socket();
    socket.connect(new InetSocketAddress(ipAddress, port), timeout);
    out = new PrintWriter(socket.getOutputStream(), true);
    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
  }

  public void sendData(String data) {
    out.println(data);
  }

  public void disconnect() throws IOException {
    if (socket != null && !socket.isClosed()) {
      out.close();
      in.close();
      socket.close();
    }
  }
}
