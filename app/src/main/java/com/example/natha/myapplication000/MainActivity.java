package com.example.natha.myapplication000;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

   public class MainActivity extends AppCompatActivity implements View.OnClickListener {

       @Override
       protected void onCreate(Bundle savedInstanceState) {
           super.onCreate(savedInstanceState);
           setContentView(R.layout.activity_main);
           //初始化方法
           initUI();
       }

       private void initUI(){
           findViewById(R.id.btn1).setOnClickListener(this);
           findViewById(R.id.btn2).setOnClickListener(this);
           findViewById(R.id.btn3).setOnClickListener(this);
           }

       @Override
       public void onClick(View v) {
           switch (v.getId()) {
               case R.id.btn1:
                   //跳转到连接界面
                   Intent intent1 = new Intent();
                   intent1.setClass(getApplicationContext(), FirstActivity.class);
                   this.startActivity(intent1);
                   break;
               case R.id.btn2:
                   //跳转到开始界面
                   Intent intent2 = new Intent();
                   intent2.setClass(getApplicationContext(), SecondActivity.class);
                   this.startActivity(intent2);
                   break;
               case R.id.btn3:
                   //跳转到关于界面
                   Intent intent3 = new Intent();
                   intent3.setClass(getApplicationContext(), ThirdActivity.class);
                   this.startActivity(intent3);
                   break;
           }
       }
   }

