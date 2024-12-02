package com.payten.ecrdemo.utils;

import android.util.TypedValue;

import androidx.core.content.ContextCompat;

import com.payten.ecrdemo.MainActivity;
import com.payten.ecrdemo.MyApp;
import com.payten.ecrdemo.R;

public class Utils {
    public static int getColorFromAttribute(int attribute){
        TypedValue typedValue = new TypedValue();
        MainActivity.theme.resolveAttribute(attribute, typedValue, true);
        return ContextCompat.getColor(MyApp.appContext, typedValue.resourceId);
    }
}
