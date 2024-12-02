package com.payten.ecrdemo.transaction;

import com.google.gson.Gson;
import com.payten.ecrdemo.MyApp;
import com.payten.ecrdemo.ecr.EcrDef;
import com.payten.ecrdemo.ecr.EcrJsonReq;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

public class TransactionUtils {
    static class Transactions{
        ArrayList<TransactionData> listTransactions;
    }

    public static ArrayList<TransactionData> loadTransactions(){
        String jsonSting = "";

        try {
            InputStream input;
            try {
                String filePath = MyApp.appContext.getFilesDir() + "/" + "transactions.json";
                input = new FileInputStream(filePath);
            } catch (IOException e) {
                return new ArrayList<>();
            }

            int size = input.available();
            byte[] buffer = new byte[size];
            int readBytes = input.read(buffer);
            input.close();

            // byte buffer into a string
            if(readBytes > 0) {
                jsonSting = new String(buffer);
            }
        } catch (IOException e) {
            return new ArrayList<>();
        }

        try {
            return new Gson().fromJson(jsonSting, Transactions.class).listTransactions;
        }
        catch (Exception e){
            return new ArrayList<>();
        }
    }

    public static void storeTransactions(ArrayList<TransactionData> listTransactions){
        Transactions transactions = new Transactions();
        transactions.listTransactions = listTransactions;

        String jsonString = new Gson().toJson(transactions);

        OutputStream out = null;
        try {
            out = new FileOutputStream(MyApp.appContext.getFilesDir() + "/" + "transactions.json");
            out.write(jsonString.getBytes(), 0, jsonString.length());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
