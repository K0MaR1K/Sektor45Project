package com.payten.ecrdemo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class MyReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("RECEIVER", "Intent received (including ECR response): " + intent.getAction());

        if(MyApp.currentActivity instanceof MainActivity){
            Log.d("RECEIVER", "Main activity already started");
            Intent i = new Intent(context, MainActivity.class);
            i.setAction(intent.getAction());
            if (intent.getStringExtra("ResponseResult") != null && intent.getStringExtra("ResponseResult") != "") {
                i.putExtra("ResponseResult", intent.getStringExtra("ResponseResult"));
            }
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            context.startActivity(i);
        }
        else if(MyApp.currentActivity == null){
            Log.d("RECEIVER", "Start main activity");
            Intent i = new Intent(context, MainActivity.class);
            i.setAction(intent.getAction());
            if (intent.getStringExtra("ResponseResult") != null && intent.getStringExtra("ResponseResult") != "") {
                i.putExtra("ResponseResult", intent.getStringExtra("ResponseResult"));
            }
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
        }
    }
}
