package com.example.natha.myapplication000;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * 实时监测页：建立到 ESP8266 (192.168.4.1:8080 或用户配置的地址) 的 TCP 连接，
 * 持续接收 10 字节定长数据帧，解析出 660nm/940nm 的 DC/AC 四路数据并驱动波形绘制。
 *
 * 主要修复点（相对原版本）：
 *  1. 使用 DataInputStream.readFully() 保证每次读满 10 字节定长帧，避免半帧/错位数据。
 *  2. 检测到 EOF（连接断开）时主动退出循环并提示用户，而不是用旧数据继续绘制。
 *  3. 加入 isRunning 标志，避免重复点击"开始"创建多条并发线程争用同一个 Socket。
 *  4. onDestroy() 中关闭 socket / 终止线程，避免资源泄漏。
 *  5. 连接地址可来自连接配置页(SharedPreferences)，不再硬编码。
 *  6. 所有异常都会通过 Handler 切回主线程更新状态文案，给用户明确反馈。
 */
public class SecondActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "spo2_connection";
    private static final String KEY_IP = "key_ip";
    private static final String KEY_PORT = "key_port";
    private static final String DEFAULT_IP = "192.168.4.1";
    private static final int DEFAULT_PORT = 8080;
    private static final int FRAME_SIZE = 10; // 每帧 10 字节：2 字节帧头/保留 + 4×2字节通道数据
    private static final int SOCKET_TIMEOUT_MS = 5000;

    private Socket socket;
    private volatile boolean isRunning = false;
    private Thread receiveThread;

    private Button btnStart;
    private TextView tvStatus;
    private View statusDot;

    // 四路波形：660nm/940nm 的 DC/AC
    private SpO2 waveDc660;
    private SpO2 waveDc940;
    private SpO2 waveAc660;
    private SpO2 waveAc940;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);
        initUI();
    }

    private void initUI() {
        btnStart = findViewById(R.id.start);
        tvStatus = findViewById(R.id.tvStatus);
        statusDot = findViewById(R.id.statusDot);

        waveDc660 = findViewById(R.id.ECGView03);
        waveDc940 = findViewById(R.id.ECGView04);
        waveAc660 = findViewById(R.id.ECGView05);
        waveAc940 = findViewById(R.id.ECGView02);

        waveDc660.setLineColor(0xE2, 0x43, 0x3C); // 660nm 红
        waveAc660.setLineColor(0xE2, 0x43, 0x3C);
        waveDc940.setLineColor(0x1D, 0x6F, 0xB8); // 940nm 蓝
        waveAc940.setLineColor(0x1D, 0x6F, 0xB8);

        setStatus(getString(R.string.status_disconnected), R.drawable.status_dot_disconnected);

        btnStart.setOnClickListener(v -> {
            if (isRunning) {
                stopReceiving();
            } else {
                startReceiving();
            }
        });
    }

    /**
     * 启动接收线程；若已在运行则忽略，避免重复连接。
     */
    private void startReceiving() {
        if (isRunning) {
            return;
        }
        isRunning = true;
        btnStart.setText(R.string.btn_stop);
        setStatus(getString(R.string.status_connecting), R.drawable.status_dot_connecting);

        String ip = getSavedIp();
        int port = getSavedPort();

        receiveThread = new Thread(() -> runReceiveLoop(ip, port));
        receiveThread.setDaemon(true);
        receiveThread.start();
    }

    /**
     * 停止接收并关闭连接。
     */
    private void stopReceiving() {
        isRunning = false;
        closeSocketQuietly();
        btnStart.setText(R.string.btn_start);
        setStatus(getString(R.string.status_disconnected), R.drawable.status_dot_disconnected);
    }

    /**
     * 子线程：建立连接 -> 循环读取定长帧 -> 解析四路数据 -> 驱动波形。
     */
    private void runReceiveLoop(String ip, int port) {
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(ip, port), SOCKET_TIMEOUT_MS);
            socket.setSoTimeout(SOCKET_TIMEOUT_MS);

            runOnUiThread(() -> setStatus(getString(R.string.status_connected), R.drawable.status_dot_connected));

            DataInputStream dis = new DataInputStream(socket.getInputStream());
            byte[] buffer = new byte[FRAME_SIZE];

            while (isRunning) {
                // 读满一个完整数据帧；流结束(EOF)会抛出 EOFException
                dis.readFully(buffer);

                // 大端解析：buffer[0..1] 为帧头/保留位，buffer[2..9] 为四路 16 位数据
                int dc660 = ((buffer[2] & 0xFF) << 8) | (buffer[3] & 0xFF);
                int dc940 = ((buffer[4] & 0xFF) << 8) | (buffer[5] & 0xFF);
                int ac660 = ((buffer[6] & 0xFF) << 8) | (buffer[7] & 0xFF);
                int ac940 = ((buffer[8] & 0xFF) << 8) | (buffer[9] & 0xFF);

                waveDc660.setLinePoint(dc660);
                waveDc940.setLinePoint(dc940);
                waveAc660.setLinePoint(ac660);
                waveAc940.setLinePoint(ac940);
            }
        } catch (IOException e) {
            if (isRunning) {
                // 非用户主动停止导致的异常 -> 提示连接失败/中断
                runOnUiThread(() -> {
                    setStatus(getString(R.string.status_error), R.drawable.status_dot_disconnected);
                    btnStart.setText(R.string.btn_start);
                });
            }
        } finally {
            isRunning = false;
            closeSocketQuietly();
        }
    }

    private void closeSocketQuietly() {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ignored) {
                // 关闭异常无需处理
            }
            socket = null;
        }
    }

    private void setStatus(String text, int dotDrawableRes) {
        tvStatus.setText(text);
        statusDot.setBackgroundResource(dotDrawableRes);
    }

    private String getSavedIp() {
        return getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getString(KEY_IP, DEFAULT_IP);
    }

    private int getSavedPort() {
        String portStr = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getString(KEY_PORT, String.valueOf(DEFAULT_PORT));
        try {
            return Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            return DEFAULT_PORT;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 离开页面时确保线程退出、socket 释放，避免资源泄漏
        isRunning = false;
        closeSocketQuietly();
    }
}
