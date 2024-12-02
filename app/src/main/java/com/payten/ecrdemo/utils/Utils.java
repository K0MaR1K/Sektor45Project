package com.payten.ecrdemo.utils;

import android.util.TypedValue;

import androidx.core.content.ContextCompat;

import com.payten.ecrdemo.BillActivity;
import com.payten.ecrdemo.MyApp;

public class Utils {
    public static int getColorFromAttribute(int attribute){
        TypedValue typedValue = new TypedValue();
        BillActivity.theme.resolveAttribute(attribute, typedValue, true);
        return ContextCompat.getColor(MyApp.appContext, typedValue.resourceId);
    }
}
