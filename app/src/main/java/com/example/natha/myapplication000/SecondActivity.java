package com.example.natha.myapplication000;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.support.annotation.Nullable;

public class SecondActivity extends AppCompatActivity {

    // 主线程Handler
    // 用于将从服务器获取的消息显示出来
    private Handler handler;

    // Socket变量服务器
    private Socket socket;

    // Socket变量客户端
    //private ServerSocket serversocket;

    // 输入流对象
    private InputStream is;

    // 连接和开始监测按钮
    private Button btnstart;

    //四个波形
    private SpO2 ecgview03;
    private SpO2 ecgview04;
    private SpO2 ecgview05;
    private SpO2 ecgview02;

    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);

        // 初始化所有按钮
        btnstart = (Button) findViewById(R.id.start);
        btnstart.setOnClickListener(new ButtonStart());

        //初始化四个波形绑定
        ecgview03 = (SpO2) findViewById(R.id.ECGView03);
        ecgview04 = (SpO2) findViewById(R.id.ECGView04);
        ecgview05 = (SpO2) findViewById(R.id.ECGView05);
        ecgview02 = (SpO2) findViewById(R.id.ECGView02);
    }

        /**
         * 创建客户端 & 服务器的Socket连接
         */

    class ButtonStart implements View.OnClickListener{
        @Override
        public void onClick(View v) {

            // 利用线程开启
            class ClientThread extends Thread{
                public void run() {
                    try {
                        // 创建Socket对象 & 指定服务端的IP 及 端口号
                        socket = new Socket("192.168.4.1", 8080);
                        //Socket socket=Socket.accept();

                        // 创建Socket对象 & 指定端口号
                        //serversocket=new ServerSocket(8080);
                        //Socket socket=serversocket.accept();

                        //传输数据
                        is=socket.getInputStream();
                        byte[]buffer=new byte[10];
                        int length=0;
                        ecgview03.setLineColor(255,00,51);//定义颜色
                        ecgview04.setLineColor(00,102,204);
                        ecgview05.setLineColor(255,00,51);
                        ecgview02.setLineColor(00,102,204);

                        //数据处理和计算
                        while (true) {
                            length=is.read(buffer);
                            int i=(buffer[2]&0xFF)*256+(buffer[3]&0xFF);
                            int j=(buffer[4]&0xFF)*256+(buffer[5]&0xFF);
                            int k=(buffer[6]&0xFF)*256+(buffer[7]&0xFF);
                            int l=(buffer[8]&0xFF)*256+(buffer[9]&0xFF);

                            ecgview03.setLinePoint(i);
                            ecgview04.setLinePoint(j);
                            ecgview05.setLinePoint(k);
                            ecgview02.setLinePoint(l);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            new ClientThread().start();
        }
    }

}
