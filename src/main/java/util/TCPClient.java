package util;

import android.util.Log;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

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

  public int sendDataWithReply(String data, int timeout) {
    try {
      out.println(data);
      out.flush();

      long startTime = System.currentTimeMillis();
      while (!in.ready()) {
        if (System.currentTimeMillis() - startTime > timeout) {
          Log.d("Syslog", "读取超时");
          return -1;
        }
      }

      char response = (char) in.read();
      Log.d("Syslog", "收到服务器的响应: " + response);
      return Integer.parseInt(String.valueOf(response));
    } catch (Exception e) {
      Log.e("TCPClient error: ", e.toString());
      return -1;
    }
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