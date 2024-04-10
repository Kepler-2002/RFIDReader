package com.example.myapplication;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.rfidread.Enumeration.*;
import com.rfidread.Interface.IAsynchronousMessage;
import com.rfidread.Models.GPI_Model;
import com.rfidread.Models.Tag_Model;
import com.rfidread.RFIDReader;
import util.TCPClient;

import java.io.IOException;
import java.util.*;
import java.util.Date;
import java.text.SimpleDateFormat;

public class ScanActivity extends AppCompatActivity implements IAsynchronousMessage{
  private TCPClient tcpClient;
  private String defaultIpAddress = "192.168.1.106";
  private int defaultPort = 7899; // 默认端口号



  private String [] Ports = {
          "/dev/ttyUSB2",
          "/dev/ttyUSB1",
          "/dev/ttyUSB0",
          "/dev/ttyS8",
          "/dev/ttyS7",
          "/dev/ttyS6",
          "/dev/ttyS5",
          "/dev/ttyS4",
          "/dev/ttyS3",
          "/dev/ttyS2",
          "/dev/ttyS1",
          "/dev/ttyS0"};

  private String connID;

  private Spinner spinnerPower;
  private TextView textViewReadCount;

  private TextView text1;

  private TableLayout tableLayout;

  private Button buttonRefresh;
  private EditText editTextIpAddress;
  private Button buttonConnect;

  private ProgressBar progressBarConnecting;
  private TextView textConnectingStatus;

  private boolean conn;

//  private boolean isReconnectRunning = false;

  // 不同的选择框选项数据
  private String [] powerOptions = new String[30];

//  // 声明一个Handler用于定时任务
//  private Handler handler = new Handler();
//  private Runnable reconnectRunnable = new Runnable() {
//    @Override
//    public void run() {
//      if (!isReconnectRunning) {
//          isReconnectRunning = true;
//      }
//
//      // 检查连接状态
//      if (!tcpClient.isConnected()) {
//        // 连接断开，进行重新连接
//        reconnectToTCPClient();
//      }
//
//      // 重新调度任务
//      handler.postDelayed(this, 5000);
//    }
//  };


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Objects.requireNonNull(getSupportActionBar()).hide();
    setContentView(R.layout.activity_scan);

    // 获取ProgressBar引用
    progressBarConnecting = findViewById(R.id.progressBarConnecting);
    textConnectingStatus = findViewById(R.id.textConnectingStatus);

    // 从SharedPreferences中读取默认IP地址和端口号
    SharedPreferences sharedPref = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
    defaultIpAddress = sharedPref.getString("ipAddress", "192.168.1.106");
    defaultPort = sharedPref.getInt("port", 7899);

    // 初始化视图组件
    spinnerPower= findViewById(R.id.spinnerPower);
    textViewReadCount = findViewById(R.id.textViewReadCount);
    buttonRefresh = findViewById(R.id.buttonRefresh);
    tableLayout = findViewById(R.id.tableLayout);
    editTextIpAddress = findViewById(R.id.editTextIpAddress);
    editTextIpAddress.setHint(defaultIpAddress + ":" + defaultPort);
    buttonConnect = findViewById(R.id.buttonConnect);
    text1 = findViewById(R.id.text1);

    ImageView imageViewLogo = findViewById(R.id.imageViewLogo);
    imageViewLogo.setImageResource(R.drawable.logo); // 设置logo图片资源

    // 显示进度条
    progressBarConnecting.setVisibility(View.VISIBLE);
    textConnectingStatus.setVisibility(View.VISIBLE);
    textConnectingStatus.setText("正在连接读写器");

    spinnerPower.setVisibility(View.INVISIBLE);
    textViewReadCount.setVisibility(View.INVISIBLE);
    buttonRefresh.setVisibility(View.INVISIBLE);
    tableLayout.setVisibility(View.INVISIBLE);
    editTextIpAddress.setVisibility(View.INVISIBLE);
    buttonConnect.setVisibility(View.INVISIBLE);
    text1.setVisibility(View.INVISIBLE);
    // 执行读写器连接操作，你可以根据实际情况替换这里的逻辑
    ConnectToReaderTask connectTask = new ConnectToReaderTask(this); // 传递活动的上下文
    connectTask.execute();

    // 设置连接按钮的点击事件
    buttonConnect.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        // 在这里处理连接操作
        // 获取输入框中的IP地址和端口号
        String ipAddressPort = editTextIpAddress.getText().toString();
        String[] parts = ipAddressPort.split(":");


        if (parts.length == 2) {
          String ipAddress = parts[0];
          int port = Integer.parseInt(parts[1]);

          connectToTCPClient(ipAddress,port);

//          if (tcpClient.isConnected() && !isReconnectRunning) {
//            handler.postDelayed(reconnectRunnable, 5000);
//          }

          // 将获取的IP地址和端口号设置为默认值
          defaultIpAddress = ipAddress;
          defaultPort = port;

          editTextIpAddress.setHint(defaultIpAddress + ":" + defaultPort);


          // 保存新的默认IP地址和端口号到SharedPreferences
          SharedPreferences.Editor editor = sharedPref.edit();
          editor.putString("ipAddress", ipAddress);
          editor.putInt("port", port);
          editor.apply(); // 提交更改
          editor.commit();
        } else {
          showToast("请输入有效的 IP 地址和端口号（格式：IP:端口号）");
        }
      }
    });

    buttonRefresh.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        // 处理提交操作
        // 在这里执行提交操作，清空表格等操作
        // 提交成功后禁用按钮
        buttonRefresh.setEnabled(false);
        // 清空表格
        clearTable();

        buttonRefresh.setEnabled(true);
      }
    });
  }

  // 在连接断开时进行重新连接
  public void reconnectToTCPClient() {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        showToast("TCP连接断开，正在重新连接...");
      }
    });

    // 进行重新连接操作
    connectToTCPClient(defaultIpAddress, defaultPort);
  }


  private void connectToTCPClient(String targetIpAddress, int targetPort) {
    new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          if (tcpClient != null) {
            tcpClient.disconnect();
          }
          // 创建 TCPClient 实例，建立连接
          tcpClient = new TCPClient(targetIpAddress, targetPort,5000);
          runOnUiThread(new Runnable() {
            @Override
            public void run() {
              Log.d("SysLog", "TCP Connection successful");
              Toast.makeText(ScanActivity.this, "TCP连接建立成功", Toast.LENGTH_SHORT).show();
            }
          });

        } catch (final IOException e) {
          e.printStackTrace();
          runOnUiThread(new Runnable() {
            @Override
            public void run() {
              Log.d("TCP Error", e.toString());
              AlertDialog.Builder builder = new AlertDialog.Builder(ScanActivity.this);
              builder.setMessage("服务器TCP连接失败，请检查网络连接")
                      .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {

                        }
                      });
              AlertDialog dialog = builder.create();
              dialog.show();
            }
          });
        }
      }
    }).start();
  }


  // 初始化选择框
  private void initSpinners(int defaultPowerLevel) {

    int defaultPowerLevelPosition = 0;

    for(int i = 1; i <= 30; i++){
      powerOptions[i - 1] = String.valueOf(i);
      if (i == defaultPowerLevel) {
        // 设置默认显示值为当前功率等级
        defaultPowerLevelPosition = i - 1;
      }
    }

    // 为每个选择框设置不同的选项数据源
    ArrayAdapter<String> powerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, powerOptions);

    // 设置下拉列表风格
    powerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    // 为每个选择框设置适配器
    spinnerPower.setAdapter(powerAdapter);
    // 设置默认选中的功率等级
    Log.d("System.out","defaultPowerLevelPosition: " + defaultPowerLevelPosition);
    spinnerPower.setSelection(defaultPowerLevelPosition);

    // 在选择框的初始化中添加选择监听器
    spinnerPower.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
        // 获取用户选择的功率等级（注意，这里的position是从0开始的，所以需要加1）
        int selectedPowerLevel = position + 1;

        // 创建一个HashMap来存储四根天线的功率设置
        HashMap<Integer, Integer> dicPower = new HashMap<Integer, Integer>();
        dicPower.put(1, selectedPowerLevel);
        dicPower.put(2, selectedPowerLevel);
        dicPower.put(3, selectedPowerLevel);
        dicPower.put(4, selectedPowerLevel);

        // 调用设置功率的函数

        int result = RFIDReader._Config.SetANTPowerParam(connID, dicPower);
        if (result == 0) {
          runOnUiThread(new Runnable() {
            @Override
            public void run() {
              // 保存新的默认功率值到SharedPreferences
              SharedPreferences sharedPref = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
              SharedPreferences.Editor editor = sharedPref.edit();
              editor.putInt("defaultPowerLevel", selectedPowerLevel);
              editor.apply(); // 提交更改
              String currentPowerLevel1 = RFIDReader._Config.GetANTPowerParam2(connID);
              Log.d("System.out", "CurrentPowerLevel: " + currentPowerLevel1);
              Toast.makeText(ScanActivity.this, "设置功率成功，当前功率等级：" + currentPowerLevel1, Toast.LENGTH_SHORT).show();
            }
          });
        } else {
          runOnUiThread(new Runnable() {
            @Override
            public void run() {
              String currentPowerLevel1 = RFIDReader._Config.GetANTPowerParam2(connID);
              Toast.makeText(ScanActivity.this, "设置功率失败，当前功率等级：" + currentPowerLevel1, Toast.LENGTH_SHORT).show();
            }
          });
        }
      }

      @Override
      public void onNothingSelected(AdapterView<?> parentView) {
        // 选择框未选择时的处理，可以留空或添加逻辑
      }
    });


  }


//  private void connectToReader() {
//    for (int i = 0; i < Ports.length; i++) {
//      conn = RFIDReader.CreateSerialConn(Ports[i] + ":115200", ScanActivity.this);
//      if (conn){
//        int finalI = i;
//        connID = Ports[i]+ ":115200";
//        runOnUiThread(new Runnable() {
//          @Override
//          public void run() {
//            showToast("串口 " + Ports[finalI]+ ":115200" + "连接成功");
//          }
//        });
//
//        break;
//      }
//    }
//
//    if(conn) {
////          RFIDReader._Config.Stop(connID);
//      // 连接成功，隐藏进度条，显示正常界面
//      runOnUiThread(new Runnable() {
//        @Override
//        public void run() {
//          progressBarConnecting.setVisibility(View.GONE);
//          textConnectingStatus.setVisibility(View.GONE);
//          spinnerPower.setVisibility(View.VISIBLE);
//          textViewReadCount.setVisibility(View.VISIBLE);
//          buttonRefresh.setVisibility(View.VISIBLE);
//          tableLayout.setVisibility(View.VISIBLE);
//          editTextIpAddress.setVisibility(View.VISIBLE);
//          buttonConnect.setVisibility(View.VISIBLE);
//          text1.setVisibility(View.VISIBLE);
//        }
//      });
//    }
//    else{
//      // 连接失败，显示警告对话框并退出应用程序
//      runOnUiThread(new Runnable() {
//        @Override
//        public void run() {
//          AlertDialog.Builder builder = new AlertDialog.Builder(ScanActivity.this);
//          builder.setMessage("连接读写器失败，请联系技术支持")
//                  .setPositiveButton("确定", new DialogInterface.OnClickListener() {
//                    public void onClick(DialogInterface dialog, int id) {
//                      // 退出应用程序
//                      finish();
//                    }
//                  });
//          AlertDialog dialog = builder.create();
//          dialog.show();
//        }
//      });
//    }
//  }

  public void showConnectingProgress() {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        progressBarConnecting.setVisibility(View.VISIBLE);
      }
    });
  }

  public void hideConnectingProgress() {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        progressBarConnecting.setVisibility(View.GONE);
      }
    });
  }

  public boolean connectToReader() {
    for (int i = 0; i < Ports.length; i++) {
      conn = RFIDReader.CreateSerialConn(Ports[i] + ":115200", this);
      if (conn) {
        int finalI = i;
        connID = Ports[i] + ":115200";
        runOnUiThread(new Runnable() {
          @Override
          public void run() {
            showToast("串口 " + Ports[finalI] + ":115200" + "连接成功");
          }
        });

        return true; // 连接成功，返回 true
      }
    }

    return false; // 连接失败，返回 false
  }

  public void initUI() {
    if (conn){
      // 从SharedPreferences中读取默认IP地址和端口号
      SharedPreferences sharedPref = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
      defaultIpAddress = sharedPref.getString("ipAddress", "192.168.1.106");
      defaultPort = sharedPref.getInt("port", 7899);
      int defaultPowerLevel = sharedPref.getInt("defaultPowerLevel", 30);
      Log.d("System.out", "Default Power Level: " + defaultPowerLevel);
      // 初始化选择框
      initSpinners(defaultPowerLevel);
      connectToTCPClient(defaultIpAddress, defaultPort);

//      if(tcpClient.isConnected() && !isReconnectRunning) {
//        // 开始定时重连检测任务
//        handler.postDelayed(reconnectRunnable, 5000);
//      }
    }
    // 继续初始化界面的代码
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        spinnerPower.setVisibility(View.VISIBLE);
        textViewReadCount.setVisibility(View.VISIBLE);
        buttonRefresh.setVisibility(View.VISIBLE);
        tableLayout.setVisibility(View.VISIBLE);
        editTextIpAddress.setVisibility(View.VISIBLE);
        buttonConnect.setVisibility(View.VISIBLE);
        text1.setVisibility(View.VISIBLE);
        textConnectingStatus.setVisibility(View.INVISIBLE);
      }
    });
  }

  public void showConnectionErrorDialog() {
    // 连接失败，显示警告对话框
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        AlertDialog.Builder builder = new AlertDialog.Builder(ScanActivity.this);
        builder.setMessage("连接读写器失败，请联系技术支持")
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                  public void onClick(DialogInterface dialog, int id) {
                    // 退出应用程序
                    finish();
                  }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
      }
    });
  }

  // 清空表格
  private void clearTable() {
    int childCount = tableLayout.getChildCount();
    // 从索引1开始，以避免移除表头
    for (int i = 1; i < childCount; i++) {
      tableLayout.removeViewAt(1);
    }

    // 清空uniqueEpcData
    EpcDataMap.clear();
    EpcDataList.clear();

    textViewReadCount.setText("读取数量: " + "0"); // 更新显示的读取数量

  }

  // 显示 Toast 消息的方法
  private void showToast(String message) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
  }

  // 在表格中插入一行数据
  private void insertRowInTable(String epcData) {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        // 在这里执行需要在主线程上进行的UI操作
        TableRow row = new TableRow(ScanActivity.this);
        TextView textViewRFID = new TextView(ScanActivity.this);

        // 设置每列的文本内容，这里需要根据实际数据进行设置
        textViewRFID.setText(epcData);
        textViewRFID.setTextSize(24);

        // 添加到表格行
        row.addView(textViewRFID);

        // 添加到表格
        tableLayout.addView(row);


      }
    });
  }

  private boolean isGPO1On = false; // 用于跟踪指示灯状态
  private long lastDataTimestamp = 0; // 记录上一次读取到数据的时间戳

  private void TurnLightOnAndOff(){

    // 在OutPutTags方法中
    runOnUiThread(new Runnable() {
      @Override
      public void run() {

        // 获取当前时间戳
        long currentTimestamp = System.currentTimeMillis();

        // 如果指示灯已经亮，并且时间差小于3秒，继续保持亮的状态
        if (isGPO1On && (currentTimestamp - lastDataTimestamp < 3000)) {
          // Do nothing, keep the light on
        } else {
          // 否则，将指示灯点亮，然后更新时间戳
          setGPO1State(eGPOState._High); // 点亮指示灯
          isGPO1On = true;
          lastDataTimestamp = currentTimestamp;

          // 在3秒后将指示灯熄灭
          new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
              setGPO1State(eGPOState.Low); // 熄灭指示灯
              isGPO1On = false;
            }
          }, 3000);
        }
      }
    });
  }
  private void setGPO1State(eGPOState state) {
    // 使用 SetReaderGPOState 函数设置 eGPO1 的状态
    HashMap<eGPO, eGPOState> gpoStates = new HashMap<>();
    gpoStates.put(eGPO._1, state);
    RFIDReader._Config.SetReaderGPOState(connID, gpoStates);
  }

  // 更新读取数量
  private void updateReadCount() {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        int currentReadCount = Integer.parseInt(textViewReadCount.getText().toString().split(" ")[1]); // 获取当前读取数量
        currentReadCount++; // 增加读取数量
        textViewReadCount.setText("读取数量: " + currentReadCount); // 更新显示的读取数量
      }
    });

  }


  @Override
  public void WriteDebugMsg(String s, String s1) {
//    Log.d("APILog", "s1: " + s1);
  }

  @Override
  public void WriteLog(String s, String s1) {

  }

  @Override
  public void PortConnecting(String s) {

  }

  @Override
  public void PortClosing(String s) {
    Log.d("APILog", "closing connect: " + s);
  }

  private HashMap<String, Integer> EpcDataMap = new HashMap<>(); // 用于存储EPC数据和对应的次数

  private ArrayList<String> EpcDataList =  new ArrayList<>();




  @Override
  public void OutPutTags(Tag_Model tag_model) {
    // 获取标签的EPC数据

    Log.d("Data Callback", "OutPutTags called with EPC: " + tag_model._EPC);
    if(EpcDataMap.get(tag_model._EPC) == null) {
      // 插入表格中并更新读取数量
      insertRowInTable(tag_model._EPC);
      updateReadCount();

      EpcDataMap.put(tag_model._EPC, 1);
      EpcDataList.add(tag_model._EPC);
    }else {
      EpcDataMap.put(tag_model._EPC, EpcDataMap.get(tag_model._EPC)  + 1);
    }

  }

  @Override
  public void OutPutTagsOver(String s) {
    Log.d("Callback", "Output Over, connID: " + s);
  }


  @Override
  public void GPIControlMsg(String s, GPI_Model gpi_model) {

//    Log.d("Callback", "ConnID: " + connID + " eGPI: " + eGPI._1 + " GPI Params: " +
//            RFIDReader._Config.GetReaderGPIParam(connID, eGPI._1));
    Log.d("Callback", "GPIControlMSg called with GPI Status: " + gpi_model.StartOrStop);

    if(gpi_model.StartOrStop == 1){ // 触发停止
      int max = 0;
      String SentData = "";
      for (String item:EpcDataList) {
        if (EpcDataMap.get(item) != null && EpcDataMap.get(item) > max){
          max = EpcDataMap.get(item);
          SentData = item;
        }
      }
      Date now = new Date();
      SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss.SSS");
      String currentTime = formatter.format(now);
      if(max != 0){
        // 发送数据到目标地址和端口号
        tcpClient.sendData(SentData + " " + currentTime);
        TurnLightOnAndOff();
      }else {
        tcpClient.sendData("noread " + currentTime);
      }
      //重新开始计数
      EpcDataMap = new HashMap<>();
      EpcDataList = new ArrayList<>();
    }
  }

  @Override
  public void OutPutScanData(String s, byte[] bytes) {
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    RFIDReader.CloseAllConnect();
  }

}

class ConnectToReaderTask extends AsyncTask<Void, Void, Boolean> {
  private ScanActivity scanActivity;

  public ConnectToReaderTask(ScanActivity activity) {
    this.scanActivity = activity;
  }

  @Override
  protected void onPreExecute() {
    // 在执行前显示进度条
    scanActivity.showConnectingProgress();
  }

  @Override
  protected Boolean doInBackground(Void... params) {
    // 在后台执行连接读写器的操作
    return scanActivity.connectToReader(); // 连接读写器的代码
  }

  @Override
  protected void onPostExecute(Boolean connected) {
    // 在执行完后隐藏进度条
    scanActivity.hideConnectingProgress();

    if (connected) {
      // 连接成功，继续初始化界面
      scanActivity.initUI();
    } else {
      // 连接失败，显示警告对话框或其他操作
      scanActivity.showConnectionErrorDialog();
    }
  }
}