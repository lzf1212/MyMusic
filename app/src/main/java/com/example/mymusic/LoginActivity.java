package com.example.mymusic;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;

public class LoginActivity extends Activity {

    private static final String TAG = "lzs";
    Button btn;
    EditText num;
    EditText pw;
    String num1;
    String password;
    Intent intent1;
    private NotificationManager notificationManager;
    private Notification notification;
    private SharedPreferences sp;


    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        num = findViewById(R.id.num);
        pw = findViewById(R.id.pw);
        sp = getSharedPreferences("mrsoft",MODE_PRIVATE);
    }

    @Override
    public void onStart()
    {
        super.onStart();
    }

    @Override
    public void onResume()
    {
        super.onResume();
    }

    @Override
    public void onPause()
    {
        super.onPause();
    }

    @Override
    public void onStop()
    {
        super.onStop();
    }

    @Override
    public void onRestart()
    {
        super.onRestart();
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
    }



    //重写onActivityResult
    @Override
    protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode == 1 && resultCode == 1){
            String returnData = data.getStringExtra("data");
            TextView tvDo = (TextView) findViewById(R.id.tvDoThing);
            tvDo.setText(returnData);
        }
    }
    //注册
    public void two5(View view)
    {
        boolean b = num.getText().toString()==null;
        boolean c= pw.getText()==null;
        Log.i(TAG, "two5: " + b);
        Log.i(TAG, "two5: "+c);
        Log.i(TAG, "two5: "+num.getText());
        Log.i(TAG, "two5: "+pw.getText());
        if(!TextUtils.isEmpty(num.getText()) && !TextUtils.isEmpty(pw.getText()))
        {
            String in_username = num.getText().toString(); //获取输入的账号
            String in_password = pw.getText().toString(); //获取输入的密码
            SharedPreferences.Editor editor = sp.edit(); //获取Editer对象
            editor.putString("username",in_username); //保存账号
            editor.putString("password",in_password); //保存密码
            editor.commit();
            Toast.makeText(LoginActivity.this,"注册成功",Toast.LENGTH_SHORT).show();
            //捕获数据并封装数据
            num1 = num.getText().toString();
            password = pw.getText().toString();
            intent1 = new Intent(this, RegisteredActivity.class);
            intent1.putExtra("id",num1);
            intent1.putExtra("ps",password);
            startActivityForResult(intent1,1);
        }else {
            Toast.makeText(LoginActivity.this,"请先进行注册",Toast.LENGTH_SHORT).show();
        }

    }

    //登录
    public void two6(View view) {

        if(!TextUtils.isEmpty(num.getText()) && !TextUtils.isEmpty(pw.getText())){
            String username = sp.getString("username","null");//获取账号
            String password = sp.getString("password","null");//获取密码
            String in_username = num.getText().toString(); //获取输入的账号
            String in_password = pw.getText().toString(); //获取输入的密码
            SharedPreferences.Editor editor = sp.edit(); //获取Editer对象
            if(in_username.equals(username) && in_password.equals(password)){
                //跳转
                Intent it1 = new Intent(LoginActivity.this, MusicActivity.class);
                startActivity(it1);
                Toast.makeText(LoginActivity.this,"登录成功",Toast.LENGTH_SHORT).show();
                finish();
            }else {
                Toast.makeText(LoginActivity.this,"账号或密码错误",Toast.LENGTH_SHORT).show();
            }
        }else {
            Toast.makeText(LoginActivity.this,"账号或密码填写错误",Toast.LENGTH_SHORT).show();
        }

    }


}
