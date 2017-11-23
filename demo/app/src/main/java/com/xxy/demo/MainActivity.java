package com.xxy.demo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import android.view.View;

public class MainActivity extends Activity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }
    public void yydj(View v){
    startActivity(new Intent(this,RecordActivity.class));
    }
    public void spth(View v){

    }
    public void dt(View v){

    }
}
