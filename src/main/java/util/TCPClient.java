package util;

import android.util.Log;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;

public class TCPClient {
  private Socket socket;
  private PrintWriter out;
  private BufferedReader in;

  public TCPClient() {
  }

  public void initialize(String ipAddress, int port, int timeout) throws IOException {
    socket = new Socket();
    socket.connect(new InetSocketAddress(ipAddress, port), timeout);
    out = new PrintWriter(socket.getOutputStream(), true);
    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
  }

  public void connect(String ipAddress, int port, int timeout) throws IOException {
    if (isConnected()){
      disconnect(); // 断开之前的连接
    }
    initialize(ipAddress, port, timeout);
  }

  public void sendData(String data) {
    out.println(data);
  }

  public int sendDataWithReply(String data) throws IOException {
    out.println(data);
    out.flush();
    String response = in.readLine();
    if (response == null) {
      Log.d("Syslog", "没有收到服务器的相应");
      return 0;
    }
    Log.d("Syslog", "收到服务器的相应: " + response);
    return Integer.parseInt(response);
  }

  public boolean isConnected() {
    if (socket == null || !socket.isConnected()) {
      System.out.println("socket is null or socket is not connected: "+ socket );
      return false;
    }

    try {
      socket.sendUrgentData(0);
      return true;
    } catch (IOException e) {
      return false;
    }
  }

  public void disconnect() throws IOException {
    if (socket != null && !socket.isClosed()) {
      out.close();
      in.close();
      socket.close();
    }
  }
}