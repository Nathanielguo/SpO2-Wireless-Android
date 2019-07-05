package com.example.natha.myapplication000;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.content.Intent;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.view.KeyEvent;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;


public class FirstActivity extends AppCompatActivity implements View.OnClickListener {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_first);
        //初始化方法
        initUI();
    }

    private void initUI(){
        findViewById(R.id.确认).setOnClickListener(this);
        findViewById(R.id.退出).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.确认:
                //跳转到首页
                Intent intent4 = new Intent();
                intent4.setClass(getApplicationContext(), MainActivity.class);
                this.startActivity(intent4);
                break;


            case R.id.退出:

                AlertDialog alertDialog = new AlertDialog.Builder(FirstActivity.this)
                        .setTitle("退出程序")
                        .setMessage("是否退出程序")
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                finish();
                            }
                        })
                        .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                return;
                            }
                        }).create();
                alertDialog.show();


        }
    }

}


