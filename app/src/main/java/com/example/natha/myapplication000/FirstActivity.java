package com.example.natha.myapplication000;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.util.regex.Pattern;

/**
 * 连接配置页：用户在此输入下位机（ESP8266）的 IP 地址与端口号。
 * 输入内容会做基础校验，并通过 Intent 传递给 SecondActivity，
 * 同时持久化到 SharedPreferences，下次进入自动回填。
 */
public class FirstActivity extends AppCompatActivity implements View.OnClickListener {

    public static final String EXTRA_IP = "extra_ip";
    public static final String EXTRA_PORT = "extra_port";

    private static final String PREFS_NAME = "spo2_connection";
    private static final String KEY_IP = "key_ip";
    private static final String KEY_PORT = "key_port";

    private static final String DEFAULT_IP = "192.168.4.1";
    private static final String DEFAULT_PORT = "8080";

    // 简单的 IPv4 格式校验
    private static final Pattern IP_PATTERN = Pattern.compile(
            "^((25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)\\.){3}(25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)$");

    private EditText editIp;
    private EditText editPort;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_first);
        initUI();
    }

    private void initUI() {
        editIp = findViewById(R.id.editText01);
        editPort = findViewById(R.id.editText02);

        // 回填上次保存的连接信息
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        editIp.setText(prefs.getString(KEY_IP, DEFAULT_IP));
        editPort.setText(prefs.getString(KEY_PORT, DEFAULT_PORT));

        findViewById(R.id.确认).setOnClickListener(this);
        findViewById(R.id.退出).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.确认) {
            onConfirm();
        } else if (id == R.id.退出) {
            showExitDialog();
        }
    }

    /**
     * 校验输入并跳转到主菜单（连接信息会在 SecondActivity 中实际使用）
     */
    private void onConfirm() {
        String ip = editIp.getText().toString().trim();
        String portStr = editPort.getText().toString().trim();

        if (TextUtils.isEmpty(ip)) {
            Toast.makeText(this, R.string.error_ip_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!IP_PATTERN.matcher(ip).matches()) {
            Toast.makeText(this, R.string.error_ip_invalid, Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(portStr)) {
            Toast.makeText(this, R.string.error_port_empty, Toast.LENGTH_SHORT).show();
            return;
        }

        int port;
        try {
            port = Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, R.string.error_port_invalid, Toast.LENGTH_SHORT).show();
            return;
        }
        if (port < 1 || port > 65535) {
            Toast.makeText(this, R.string.error_port_invalid, Toast.LENGTH_SHORT).show();
            return;
        }

        // 持久化保存，下次自动回填
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putString(KEY_IP, ip)
                .putString(KEY_PORT, portStr)
                .apply();

        // 返回主菜单（连接参数会在用户点击"开始监测"后由 SecondActivity 读取）
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.putExtra(EXTRA_IP, ip);
        intent.putExtra(EXTRA_PORT, port);
        startActivity(intent);
        finish();
    }

    private void showExitDialog() {
        new AlertDialog.Builder(FirstActivity.this)
                .setTitle(R.string.exit_title)
                .setMessage(R.string.exit_message)
                .setPositiveButton(R.string.dialog_ok, (dialog, which) -> finish())
                .setNegativeButton(R.string.dialog_cancel, (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }
}
