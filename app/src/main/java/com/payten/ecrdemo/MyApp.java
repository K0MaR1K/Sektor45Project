package com.payten.ecrdemo;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;

import com.payten.ecrdemo.transaction.TransactionData;

import java.util.ArrayList;

public class MyApp  extends Application {
    public static Activity currentActivity = null;
    public static Context appContext;
    public static String selectedAppType = "";
    public static String selectedNewAppType = "";

    public static ArrayList<TransactionData> transactionList = new ArrayList<>();

    public ActivityLifecycleCallbacks activityLifecycleCallbacks = new ActivityLifecycleCallbacks() {
        //        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            appContext = activity.getApplicationContext();
            currentActivity = activity;
        }

        //        @Override
        public void onActivityStarted(Activity activity) {
        }

        //        @Override
        public void onActivityResumed(Activity activity) {
            if(activity instanceof MainActivity){
                currentActivity = activity;
            }
        }

        //        @Override
        public void onActivityPaused(Activity activity) {

        }

        //        @Override
        public void onActivityStopped(Activity activity) {
        }

        //        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

        }

        //        @Override
        public void onActivityDestroyed(Activity activity) {
            if(activity instanceof  MainActivity){
                currentActivity = null;
            }
        }

    };


    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(activityLifecycleCallbacks);
    }
}
