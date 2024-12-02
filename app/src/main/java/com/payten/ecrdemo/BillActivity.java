package com.payten.ecrdemo;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.gson.GsonBuilder;
import com.payten.ecrdemo.config.MainConfig;
import com.payten.ecrdemo.ecr.EcrDef;
import com.payten.ecrdemo.ecr.EcrJsonReq;
import com.payten.ecrdemo.ecr.EcrJsonRsp;
import com.payten.ecrdemo.ecr.EcrRequestTransactionType;
import com.payten.ecrdemo.ecr.EcrResponseCode;
import com.payten.ecrdemo.transaction.TransactionData;
import com.payten.ecrdemo.transaction.TransactionUtils;
import com.payten.ecrdemo.utils.HashUtils;
import com.google.gson.Gson;
import com.payten.ecrdemo.utils.Utils;
import com.payten.service.PaytenAidlInterface;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


public class BillActivity extends AppCompatActivity implements View.OnClickListener {
    public static Resources.Theme theme;

    private static final int FONT_SIZE = 20;
    private static final int LOWER_MARGIN = 16;


    public static final int ECR_NONE = 0;
    public static final int ECR_TRANSACTION = 1;
    public static final int ECR_SETTLE = 2;
    public static final int ECR_VOID = 3;

    public static int ecrAction = ECR_NONE;
    public static int voidTranIndex;

    class Stavka {
        public String name;
        public Float price;
        public String date;
        Stavka(String item, Float price, String date) {
            this.name = item;
            this.price = price;
            this.date = date;
        }
    }

    private ConstraintLayout mainScreen;
    private ConstraintLayout resultScreen;
    private ConstraintLayout systemScreen;
    private PaytenAidlInterface paytenAidlInterface;
    private final Executor executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean serviceOrPayment = true;
    private boolean android10Spec = false;

    MainConfig mainConfig;

    ScrollView billText;
    TextView btnDa;
    TextView btnNe;
    ImageButton btnCategory1;
    ImageButton btnCategory2;
    ImageButton btnCategory3;
    ImageButton btnCategory4;
    Button btnGoToWelcomeScreen;
    ImageButton btnGoToCategoriesScreen;
    ImageButton btnGoToCategoriesScreen1;
    ImageButton btnGoToBillScreen;
    ImageButton  btnPay;
    ImageButton  btnCancel;
    TableLayout billTable;

    ArrayList<Stavka> billDataEntryList = new ArrayList<>();

    ArrayList<Button> chargesButtons = new ArrayList<>();
    ArrayList<Stavka> chargesData = new ArrayList<>();
    ArrayList<Stavka> chargesKazne = new ArrayList<>();
    ArrayList<Stavka> chargesPorezi = new ArrayList<>();
    ArrayList<Stavka> chargesRegistracije = new ArrayList<>();
    ArrayList<Stavka> chargesOstaleTakse = new ArrayList<>();

    BigDecimal billTotal = BigDecimal.ZERO;
    BigDecimal donation = BigDecimal.ZERO;

    EcrJsonRsp resp = null;

    TextView resultText;
    ImageButton btnPrint;
    Button btnOk;
    ImageButton btnSystemClose;
    ImageButton btnSystemOk;
    ProgressBar progressBar;
    CountDownTimer resultScreenTimer;

    long settingsClickTimer;
    int settingsClickCount;

    TableLayout transTable;
    ScrollView transScroll;

    ArrayList<ImageButton> voidButtons = new ArrayList<>();

    TextView settlement;

    private void loadConfig(String configFile){
        String configJson;
        configJson = getAssetJsonData(getApplicationContext(),configFile);

        try {
            mainConfig = new Gson().fromJson(configJson, MainConfig.class);
            if (mainConfig.items == null || mainConfig.items.size() == 0){
                finish();
            }
        }
        catch (Exception e){
            finish();
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        chargesPorezi.add(new Stavka("Porez na imovinu", 2680.00f, "30.11.2024."));
        chargesRegistracije.add(new Stavka("Registracija vozila", 14870.00f, "23.10.2024."));
        chargesKazne.add(new Stavka("Parking kazna", 1480.0f, "23.03.2024."));
        chargesKazne.add(new Stavka("Kazna za prekoračenje brzine", 3000.00f, "17.09.2024."));

        super.onCreate(savedInstanceState);

        settingsClickTimer = 0;
        settingsClickCount = 0;

        if (MyApp.selectedAppType.equals("")) {
            MyApp.selectedAppType = "config/RecordStore.json";
            MyApp.selectedNewAppType = "config/RecordStore.json";
        }

        loadConfig(MyApp.selectedAppType);
        setAppTheme();
        theme = getTheme();

        MyApp.transactionList = TransactionUtils.loadTransactions();

        setContentView(R.layout.activity_main);

        btnPay = findViewById(R.id.button_pay);
        btnCancel = findViewById(R.id.button_back);
        btnGoToWelcomeScreen = findViewById(R.id.go_to_welcome_screen_button);
        btnGoToCategoriesScreen = findViewById(R.id.go_to_categories_screen_button);
        btnGoToBillScreen = findViewById(R.id.go_to_bill_screen_button);
        btnGoToCategoriesScreen1 = findViewById(R.id.back_to_categories_button);
        btnCategory1 = findViewById(R.id.button_1);
        btnCategory2 = findViewById(R.id.button_2);
        btnCategory3 = findViewById(R.id.button_3);
        btnCategory4 = findViewById(R.id.button_4);

        btnDa = findViewById(R.id.accept_button);
        btnNe = findViewById(R.id.reject_button);

        btnPay.setOnClickListener(this);
        btnCancel.setOnClickListener(this);
        btnGoToWelcomeScreen.setOnClickListener(this);
        btnGoToBillScreen.setOnClickListener(this);
        btnGoToCategoriesScreen.setOnClickListener(this);
        btnGoToCategoriesScreen1.setOnClickListener(this);
        btnCategory1.setOnClickListener(this);
        btnCategory2.setOnClickListener(this);
        btnCategory3.setOnClickListener(this);
        btnCategory4.setOnClickListener(this);

        btnDa.setOnClickListener(this);
        btnNe.setOnClickListener(this);

        billText = findViewById(R.id.BillView);
        billTable = findViewById(R.id.BillTable);

        findViewById(R.id.bill_screen).setOnClickListener(this);

        billTable.removeAllViewsInLayout();

        mainScreen = findViewById(R.id.bill_screen);
        resultScreen = findViewById(R.id.resultScreen);
        systemScreen = findViewById(R.id.systemScreen);

        resultText = findViewById(R.id.resultText);
        btnPrint = findViewById(R.id.button_Print);
        btnPrint.setOnClickListener(this);
        btnOk = findViewById(R.id.btnOk);
        btnOk.setOnClickListener(this);
        progressBar = findViewById(R.id.progressBar);

        btnSystemClose = findViewById(R.id.btnSystemClose);
        btnSystemClose.setOnClickListener(this);
        btnSystemOk = findViewById(R.id.btnSystemOk);
        btnSystemOk.setOnClickListener(this);

        transTable = findViewById(R.id.transactionTable);
        transTable.removeAllViewsInLayout();

        transScroll = findViewById(R.id.transactionView);

        settlement = findViewById(R.id.settlement);
        settlement.setOnClickListener(this);

        if (paytenAidlInterface ==null){
            Intent it=new Intent();
            it.setClassName("com.payten.service","com.payten.service.PaytenEcrService");
            bindService(it,connection, Context.BIND_AUTO_CREATE);
        }

        billText.fullScroll(View.FOCUS_DOWN);
    }

    private void setAppTheme() {
        int theme = R.style.MainTheme;

        setTheme(theme);
    }

    protected ServiceConnection connection=new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            //Receive the Object which is return by the Sever_Service on Bind Function
            paytenAidlInterface = PaytenAidlInterface.Stub.asInterface(iBinder);
            Toast.makeText(getApplicationContext(),"Service Connected",Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }
    };

    EcrJsonRsp processEcrResponse(String json){
        try {
            return new Gson().fromJson(json, EcrJsonRsp.class);
        }
        catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    private void calculateDonation() {
        TextView total_text = findViewById(R.id.total_amount);
        TextView donation_text = findViewById(R.id.round_amount);
        total_text.setText(formatAmount(billTotal, true));
        donation = BigDecimal.valueOf(Math.ceil((billTotal.doubleValue() + 0.01) / 100) * 100);
        donation_text.setText(formatAmount(donation, true));
    }

    private void doEcr(String json, boolean fromAssets){
        executor.execute(()->{
            try {
                if (fromAssets) {
                    if (paytenAidlInterface != null) {
                        String resultData = paytenAidlInterface.ecrResponse(getAssetJsonData(getApplicationContext(), json));
                        handler.post(() -> {
                            Log.d("ECR", "Response received: " + resultData);
                            resp = processEcrResponse(resultData);
                            //returnToMainScreen();
                        });
                    }
                }
                else{
                    if (paytenAidlInterface != null) {
                        String resultData = paytenAidlInterface.ecrResponse(json);
                        handler.post(() -> {
                            Log.d("ECR", "Response received: " + resultData);
                            resp = processEcrResponse(resultData);
                            //returnToMainScreen();
                        });
                    }
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        String ACTION_DEFAULT_ECR_APP = "com.payten.devicemanager.DefaultEcrApp";
        if (intent.getAction() == null || (!intent.getAction().equals("android.intent.action.MAIN") && !intent.getAction().equals(ACTION_DEFAULT_ECR_APP))){
            Log.d("ECR", "Response: " + intent.getStringExtra("ResponseResult"));
            resp = processEcrResponse(intent.getStringExtra("ResponseResult"));
            showResultScreen(false);
        }

        super.onNewIntent(intent);
    }

    @SuppressLint("QueryPermissionsNeeded")
    private void sendJsonStringToApos(String json, boolean fromAssets) throws RemoteException, InterruptedException {
        if(!serviceOrPayment){
            // Send action to service
            doEcr(json, fromAssets);
        }
        else
        {
            // Send request to payment application
            if(android10Spec)
            {
                Intent i = new Intent("android.intent.action.MAIN");
                i.setComponent(new ComponentName("com.payten.paytenapos", "com.payten.paytenapos.ui.activities.SplashActivity"));
                i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                if (i.resolveActivity(getPackageManager()) != null) {
                    startActivity(i);
                }
            }

            Intent intent = new Intent("com.payten.ecr.action");
            intent.setPackage("com.payten.paytenapos");
            if (fromAssets) {
                intent.putExtra("ecrJson", getAssetJsonData(getApplicationContext(), json));
            }
            else{
                intent.putExtra("ecrJson", json);
            }
            intent.putExtra("senderIntentFilter", "senderIntentFilter");
            intent.putExtra("senderPackage", getPackageName());
            intent.putExtra("senderClass", "com.payten.ecrdemo.MainActivity");
            intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            getApplicationContext().sendBroadcast(intent);
        }
    }

    //BUILDING JSON STRING FROM ASSETS FOLDER
    public static String getAssetJsonData(Context context,String path) {
        String json = null;
        try {
            InputStream is = context.getAssets().open(path);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }

    // WHEN THIS ACTIVITY IS LOADED USE THIS FOR EACH ITEM TO ADD IT TO BILL ->
    void addItemToBill(Stavka data){
        settingsClickTimer = 0;
        settingsClickCount = 0;

        billDataEntryList.add(data);
        BigDecimal amt = new BigDecimal(data.price);
        billTotal = billTotal.add(amt);
    }
    void removeItemFromBill(Stavka data){
        settingsClickTimer = 0;
        settingsClickCount = 0;

        billDataEntryList.remove(data);
        BigDecimal amt = new BigDecimal(data.price);
        billTotal = billTotal.subtract(amt);
    }

    String formatAmount(BigDecimal d, boolean withCurrency){
        String s = String.valueOf(d);
        int decimalPosition = s.indexOf('.');
        if (decimalPosition < 0){
            // No decimals point
            s = s + ".00";
        }
        else if (decimalPosition == s.length() - 2) {
            // Only one decimal
            s = s + "0";
        }
        else if (decimalPosition == s.length() - 1) {
            // decimal point at the end without digits to follow
            s = s + "00";
        }
        if (withCurrency){
            s = s + " " + mainConfig.currency;
        }
        return s;
    }

    void performSettlement(){
        EcrJsonReq ecrJsonReq = new EcrJsonReq();
        ecrJsonReq.header = new EcrJsonReq.Header();
        ecrJsonReq.request = new EcrJsonReq.Request();
        ecrJsonReq.request.financial = new EcrJsonReq.Financial();
        ecrJsonReq.request.financial.transaction = EcrRequestTransactionType.settlement;
        ecrJsonReq.request.financial.options = new EcrJsonReq.Options();
        ecrJsonReq.request.financial.options.language = mainConfig.language;
        ecrJsonReq.request.financial.options.print = "true";

        String tempRequest = "\"request\":"+new Gson().toJson(ecrJsonReq.request);
        String generatedSHA512 = HashUtils.performSHA512(tempRequest);

        ecrJsonReq.header.version = "01";
        ecrJsonReq.header.length = tempRequest.length();
        ecrJsonReq.header.hash = generatedSHA512;

        String jsonRequest = new Gson().toJson(ecrJsonReq);

        ecrAction = ECR_SETTLE;

        try {
            sendJsonStringToApos(jsonRequest, false);
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    void performVoid(TransactionData transactionData){
        EcrJsonReq ecrJsonReq = new EcrJsonReq();
        ecrJsonReq.header = new EcrJsonReq.Header();
        ecrJsonReq.request = new EcrJsonReq.Request();
        ecrJsonReq.request.financial = new EcrJsonReq.Financial();
        ecrJsonReq.request.financial.id = new EcrJsonReq.Id();
        ecrJsonReq.request.financial.id.invoice = transactionData.invoice;
        ecrJsonReq.request.financial.transaction = EcrRequestTransactionType.voidtran;
        ecrJsonReq.request.financial.options = new EcrJsonReq.Options();
        ecrJsonReq.request.financial.options.language = mainConfig.language;
        ecrJsonReq.request.financial.options.print = "true";

        String tempRequest = "\"request\":"+new Gson().toJson(ecrJsonReq.request);
        String generatedSHA512 = HashUtils.performSHA512(tempRequest);

        ecrJsonReq.header.version = "01";
        ecrJsonReq.header.length = tempRequest.length();
        ecrJsonReq.header.hash = generatedSHA512;

        String jsonRequest = new Gson().toJson(ecrJsonReq);

        ecrAction = ECR_VOID;
        voidTranIndex = MyApp.transactionList.indexOf(transactionData);

        try {
            sendJsonStringToApos(jsonRequest, false);
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    void performPayment(){
        EcrJsonReq ecrJsonReq = new EcrJsonReq();
        ecrJsonReq.header = new EcrJsonReq.Header();
        ecrJsonReq.request = new EcrJsonReq.Request();
        ecrJsonReq.request.financial = new EcrJsonReq.Financial();
        //ecrJsonReq.request.financial.id = new EcrJsonReq.Id();
        ecrJsonReq.request.financial.transaction = EcrRequestTransactionType.sale;
        ecrJsonReq.request.financial.amounts = new EcrJsonReq.Amounts();
        ecrJsonReq.request.financial.amounts.currencyCode = mainConfig.currency;
        ecrJsonReq.request.financial.amounts.base = formatAmount(billTotal, false);
        ecrJsonReq.request.financial.options = new EcrJsonReq.Options();
        ecrJsonReq.request.financial.options.language = mainConfig.language;
        ecrJsonReq.request.financial.options.print = "true";

        String tempRequest = "\"request\":"+new Gson().toJson(ecrJsonReq.request);
        String generatedSHA512 = HashUtils.performSHA512(tempRequest);

        ecrJsonReq.header.version = "01";
        ecrJsonReq.header.length = tempRequest.length();
        ecrJsonReq.header.hash = generatedSHA512;

        String jsonRequest = new Gson().toJson(ecrJsonReq);
        Log.d("ECR", "Request: " + jsonRequest);

        TransactionData newTrans = new TransactionData();
        newTrans.transaction = ecrJsonReq.request.financial.transaction;
        newTrans.base = ecrJsonReq.request.financial.amounts.base;
        newTrans.currencyCode = ecrJsonReq.request.financial.amounts.currencyCode;
        newTrans.pending = true;
        newTrans.voided = false;
        MyApp.transactionList.add(newTrans);

        ecrAction = ECR_TRANSACTION;

        try {
            sendJsonStringToApos(jsonRequest, false);
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        returnToMainScreen();
    }

    void showSystemScreen(){
        mainScreen.setVisibility(View.GONE);
        systemScreen.setVisibility(View.VISIBLE);
        resultScreen.setVisibility(View.GONE);

        setTransTableHeader();
        setTransTableData();
    }

    void showResultScreen(boolean inProgress){
        mainScreen.setVisibility(View.GONE);
        systemScreen.setVisibility(View.GONE);
        resultScreen.setVisibility(View.VISIBLE);

        if (inProgress){
            resultText.setText("Transaction in progress...");
            resultText.setTextColor(Utils.getColorFromAttribute(R.attr.colorOnSecondary));
            progressBar.setVisibility(View.VISIBLE);
            btnPrint.setVisibility(View.INVISIBLE);
            btnOk.setVisibility(View.INVISIBLE);
        }
        else{
            new Handler(Looper.getMainLooper()).post(() -> resultScreenTimer = new CountDownTimer(10 * 1000, 10 * 1000) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                    }

                    @Override
                    public void onFinish() {
                        returnToMainScreen();
                    }
                }.start()
            );

            progressBar.setVisibility(View.GONE);
            btnOk.setVisibility(View.VISIBLE);

            if (ecrAction == ECR_TRANSACTION) {
                if (resp != null && resp.response != null && resp.response.financial != null && resp.response.financial.result != null &&
                        resp.response.financial.result.code != null && resp.response.financial.result.code.equals(EcrResponseCode.approved) &&
                        resp.response.financial.id != null && resp.response.financial.id.card != null) {
                    // Update last transaction
                    int id = MyApp.transactionList.size() - 1;
                    MyApp.transactionList.get(id).invoice = resp.response.financial.id.invoice;
                    MyApp.transactionList.get(id).authorization = resp.response.financial.id.authorization;
                    MyApp.transactionList.get(id).reference = resp.response.financial.id.reference;
                    MyApp.transactionList.get(id).cardName = resp.response.financial.id.card.name;
                    MyApp.transactionList.get(id).pan = resp.response.financial.id.card.pan;
                    MyApp.transactionList.get(id).bin = resp.response.financial.id.card.bin;
                    if (resp.response.financial.amounts != null) {
                        MyApp.transactionList.get(id).base = resp.response.financial.amounts.base;
                    }
                    MyApp.transactionList.get(id).pending = false;
                    MyApp.transactionList.get(id).dateTime = resp.response.financial.dateTime;

                    btnPrint.setVisibility(View.VISIBLE);

                    // Display transaction success
                    resultText.setText("Transaction successful.");
                    resultText.setTextColor(getColor(R.color.green));
                } else {
                    // Delete last attempted transaction from the list
                    MyApp.transactionList.remove(MyApp.transactionList.size() - 1);

                    // Display transaction failed
                    resultText.setText("Transaction failed!");
                    resultText.setTextColor(getColor(R.color.colorCancel));
                }

                TransactionUtils.storeTransactions(MyApp.transactionList);
            }
            else if (ecrAction == ECR_SETTLE){
                if (resp != null && resp.response != null && resp.response.financial != null && resp.response.financial.result != null &&
                        resp.response.financial.result.code != null && resp.response.financial.result.code.equals(EcrResponseCode.approved)) {
                    MyApp.transactionList = new ArrayList<>();
                    TransactionUtils.storeTransactions(MyApp.transactionList);

                    // Display settlement success
                    resultText.setText("Settlement successful.");
                    resultText.setTextColor(getColor(R.color.green));
                }
                else{
                    // Display settlement failed
                    resultText.setText("Settlement failed!");
                    resultText.setTextColor(getColor(R.color.colorCancel));
                }
            }
            else if (ecrAction == ECR_VOID){
                if (resp != null && resp.response != null && resp.response.financial != null && resp.response.financial.result != null &&
                        resp.response.financial.result.code != null && resp.response.financial.result.code.equals(EcrResponseCode.approved)) {
                    MyApp.transactionList.get(voidTranIndex).voided = true;
                    TransactionUtils.storeTransactions(MyApp.transactionList);

                    // Display settlement success
                    resultText.setText("Void successful.");
                    resultText.setTextColor(getColor(R.color.green));
                }
                else{
                    // Display void failed
                    resultText.setText("Void failed!");
                    resultText.setTextColor(getColor(R.color.colorCancel));
                }
            }
        }
    }

    void setTransTableHeader(){
        transTable.removeAllViewsInLayout();

        TableRow tr=new TableRow(this);
        tr.setLayoutParams(new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT));

        TextView b3=new TextView(this);
        b3.setText("Trans");
        b3.setTextColor(getColor(R.color.anthracite));
        b3.setTextSize(FONT_SIZE);
        b3.setTypeface(b3.getTypeface(), Typeface.BOLD);
        tr.addView(b3);

        TextView b4=new TextView(this);
        b4.setPadding(20, 0, 0, 0);
        b4.setGravity(Gravity.RIGHT);
        b4.setTextSize(FONT_SIZE);
        b4.setText("Invoice");
        b4.setTextColor(getColor(R.color.anthracite));
        b4.setTypeface(b4.getTypeface(), Typeface.BOLD);
        tr.addView(b4);

        TextView b5=new TextView(this);
        b5.setPadding(40, 0, 0, 0);
        b5.setGravity(Gravity.RIGHT);
        b5.setText("Amount");
        b5.setTextColor(getColor(R.color.anthracite));
        b5.setTextSize(FONT_SIZE);
        b5.setTypeface(b5.getTypeface(), Typeface.BOLD);
        tr.addView(b5);

        ImageButton i1 =new ImageButton(this);
        i1.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.colorCancel)));
        i1.setImageResource(R.drawable.ic_cancel);
        i1.setScaleType(ImageView.ScaleType.FIT_CENTER);
        i1.setVisibility(View.INVISIBLE);
        tr.addView(i1);

        TableRow tr2=new TableRow(this);
        tr2.setLayoutParams(new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT));

        TextView b6=new TextView(this);
        b6.setText("PAN");
        b6.setTextColor(getColor(R.color.anthracite));
        b6.setTextSize(FONT_SIZE);
        b6.setTypeface(b6.getTypeface(), Typeface.BOLD);
        tr2.addView(b6);

        TextView b7=new TextView(this);
        b7.setPadding(20, 0, 0, 0);
        b7.setTextSize(FONT_SIZE);
        b7.setText("Card");
        b7.setTextColor(getColor(R.color.anthracite));
        b7.setTypeface(b7.getTypeface(), Typeface.BOLD);
        tr2.addView(b7);

        TextView b8=new TextView(this);
        b8.setPadding(40, 0, 0, 0);
        b8.setGravity(Gravity.RIGHT);
        b8.setText("Auth #");
        b8.setTextColor(getColor(R.color.anthracite));
        b8.setTextSize(FONT_SIZE);
        b8.setTypeface(b8.getTypeface(), Typeface.BOLD);
        tr2.addView(b8);

        ImageButton i2 =new ImageButton(this);
        i2.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.colorCancel)));
        i2.setImageResource(R.drawable.ic_cancel);
        i2.setScaleType(ImageView.ScaleType.FIT_CENTER);
        i2.setVisibility(View.INVISIBLE);
        tr.addView(i2);

        transTable.addView(tr);
        transTable.addView(tr2);

        final View vline = new View(this);
        vline.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, 2));
        vline.setBackgroundColor(getColor(R.color.anthracite));
        transTable.addView(vline); // add line below heading

        transScroll.fullScroll(View.FOCUS_DOWN);
    }

    void setTransTableData(){
        voidButtons = new ArrayList<>();
        voidTranIndex = -1;

        int i = 0;
        for (TransactionData td: MyApp.transactionList) {
            if (!td.pending) {
                int color;
                if (i % 2 == 0) {
                    color = getColor(R.color.white);
                } else {
                    color = getColor(R.color.light_gray);
                }
                i++;

                TableRow tr = new TableRow(this);
                tr.setLayoutParams(new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT));
                tr.setBackgroundColor(color);

                TextView b3 = new TextView(this);
                if (td.voided){
                    b3.setText("VD " + td.transaction.toUpperCase());
                }
                else {
                    b3.setText(td.transaction.toUpperCase());
                }
                b3.setTextSize(FONT_SIZE);
                if (td.voided){
                    b3.setTextColor(getColor(R.color.red));
                }
                tr.addView(b3);

                TextView b4 = new TextView(this);
                b4.setPadding(20, 0, 0, 0);
                b4.setGravity(Gravity.RIGHT);
                b4.setTextSize(FONT_SIZE);
                b4.setText(td.invoice);
                if (td.voided){
                    b4.setTextColor(getColor(R.color.red));
                }
                tr.addView(b4);

                TextView b5 = new TextView(this);
                b5.setPadding(40, 0, 0, 0);
                b5.setGravity(Gravity.RIGHT);
                b5.setText(td.base + " " + td.currencyCode);
                b5.setTextSize(FONT_SIZE);
                if (td.voided){
                    b5.setTextColor(getColor(R.color.red));
                }
                tr.addView(b5);

                ImageButton i1 =new ImageButton(this);
                i1.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.colorCancel)));
                i1.setImageResource(R.drawable.ic_cancel);
                i1.setScaleType(ImageView.ScaleType.FIT_CENTER);
                i1.setVisibility(View.INVISIBLE);
                tr.addView(i1);

                TableRow tr2 = new TableRow(this);
                tr2.setLayoutParams(new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.MATCH_PARENT));
                tr2.setBackgroundColor(color);

                TextView b6 = new TextView(this);
                b6.setText(td.pan.substring(td.pan.length() - 8));
                b6.setTextSize(FONT_SIZE);
                if (td.voided){
                    b6.setTextColor(getColor(R.color.red));
                }
                tr2.addView(b6);

                TextView b7 = new TextView(this);
                b7.setPadding(20, 0, 0, 0);
                b7.setTextSize(FONT_SIZE);
                b7.setText(td.cardName);
                if (td.voided){
                    b7.setTextColor(getColor(R.color.red));
                }
                tr2.addView(b7);

                TextView b8 = new TextView(this);
                b8.setPadding(40, 0, 0, 0);
                b8.setGravity(Gravity.RIGHT);
                b8.setText(td.authorization);
                b8.setTextSize(FONT_SIZE);
                if (td.voided){
                    b8.setTextColor(getColor(R.color.red));
                }
                tr2.addView(b8);

                ImageButton i2 =new ImageButton(this);
                i2.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.colorCancel)));
                i2.setImageResource(R.drawable.ic_cancel);
                i2.setScaleType(ImageView.ScaleType.FIT_CENTER);
                if (td.voided) {
                    i2.setVisibility(View.INVISIBLE);
                }
                else{
                    i2.setVisibility(View.VISIBLE);
                }
                tr2.addView(i2);

                i2.setOnClickListener(this);
                voidButtons.add(i2);

                transTable.addView(tr);
                transTable.addView(tr2);
            }
        }
    }

    void setBillTableHeader(){
        billTable.removeAllViewsInLayout();
        TableRow tr=new TableRow(this);

        tr.setLayoutParams(new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.MATCH_PARENT));
        ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) tr.getLayoutParams();
        mlp.setMargins(0, 5, 0, 0);
        tr.setLayoutParams(mlp);

        TextView b3=new TextView(this);
        b3.setText("Naziv");
        b3.setTextColor(getColor(R.color.white));
        b3.setTextSize(FONT_SIZE);
        b3.setTypeface(b3.getTypeface(), Typeface.BOLD);
        tr.addView(b3);

        TextView b5=new TextView(this);
        b5.setGravity(Gravity.RIGHT);
        b5.setText("Cena");
        b5.setTextColor(getColor(R.color.white));
        b5.setTextSize(FONT_SIZE);
        b5.setTypeface(b5.getTypeface(), Typeface.BOLD);
        tr.addView(b5);
        billTable.addView(tr);

        final View vline = new View(this);
        vline.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, 2));
        mlp = (ViewGroup.MarginLayoutParams) vline.getLayoutParams();
        mlp.setMargins(0, 5, 0, 5);
        vline.setBackgroundColor(getColor(R.color.white));
        billTable.addView(vline); // add line below heading

        billText.fullScroll(View.FOCUS_DOWN);
    }

    void setBillTableData(){
        int i = 0;
        for (Stavka bd: billDataEntryList) {
            int color;
            if (i % 2 == 0) {
                color = getColor(R.color.white);
            } else {
                color = getColor(R.color.light_gray);
            }

            i++;
            BigDecimal itemTotal = new BigDecimal(bd.price);

            TableRow tr=new TableRow(this);
            tr.setLayoutParams(new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT));
            ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) tr.getLayoutParams();
            mlp.setMargins(0, 5, 0, 5);

            TextView b=new TextView(this);
            String str=String.valueOf(bd.name);

            if(str.length() > 15){
                str = str.substring(0, 16);
                str += "...";
            }

            b.setText(str);
            b.setTextSize(FONT_SIZE);
            b.setTextColor(getColor(R.color.white));
            tr.addView(b);
            
            TextView b2=new TextView(this);
            b2.setGravity(Gravity.RIGHT);
            b2.setText(formatAmount(itemTotal, true));
            b2.setTextSize(FONT_SIZE);
            b2.setTextColor(getColor(R.color.white));
            tr.addView(b2);

            billTable.addView(tr);
        }

        if (billTotal.compareTo(BigDecimal.ZERO) > 0) {
            final View vline = new View(this);
            vline.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, 2));
            ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) vline.getLayoutParams();
            mlp.setMargins(0, 15, 0, 0);
            vline.setBackgroundColor(getColor(R.color.white));

            billTable.addView(vline); // add line below data

            TableRow tr = new TableRow(this);

            tr.setLayoutParams(new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT));
            mlp = (ViewGroup.MarginLayoutParams) tr.getLayoutParams();
            mlp.setMargins(0, 5, 0, 5);

            TextView b3 = new TextView(this);
            b3.setText("Ukupno");
            b3.setTextColor(getColor(R.color.white));
            b3.setTextSize(FONT_SIZE);
            b3.setTypeface(b3.getTypeface(), Typeface.BOLD);
            tr.addView(b3);

            TextView b5 = new TextView( this);
            b5.setGravity(Gravity.RIGHT);
            b5.setText(formatAmount(billTotal, true));
            b5.setTextColor(getColor(R.color.white));
            b5.setTextSize(FONT_SIZE);
            b5.setTypeface(b5.getTypeface(), Typeface.BOLD);
            tr.addView(b5);
            billTable.addView(tr);
        }
    }

    void returnToMainScreen(){
        View welcomeScreen = findViewById(R.id.welcome_screen);
        welcomeScreen.setVisibility(View.VISIBLE);
        View screen = findViewById(R.id.categories_screen);
        screen.setVisibility(View.GONE);
        screen = findViewById(R.id.charges_screen);
        screen.setVisibility(View.GONE);
        screen = findViewById(R.id.bill_screen);
        screen.setVisibility(View.GONE);
        screen = findViewById(R.id.donation_screen);
        screen.setVisibility(View.GONE);
        resultScreen.setVisibility(View.GONE);
        systemScreen.setVisibility(View.GONE);

        if (resultScreenTimer != null) {
            resultScreenTimer.cancel();
        }

        billDataEntryList.clear();
        billTotal = BigDecimal.ZERO;
        btnPay.setVisibility(View.INVISIBLE);

        billTable.removeAllViewsInLayout();
    }

    @Override
    public void onClick(View v){
        int id = v.getId();

        if (id == R.id.button_back){
            View billScreen = findViewById(R.id.bill_screen);
            billScreen.setVisibility(View.INVISIBLE);
            View chargesScreen = findViewById(R.id.charges_screen);
            chargesScreen.setVisibility(View.VISIBLE);
        } else if(id == R.id.go_to_welcome_screen_button){
            View categoriesScreen = findViewById(R.id.categories_screen);
            categoriesScreen.setVisibility(View.INVISIBLE);
            View welcomeScreen = findViewById(R.id.welcome_screen);
            welcomeScreen.setVisibility(View.VISIBLE);

        } else if(id == R.id.button_1 || id == R.id.button_2 || id == R.id.button_3 || id == R.id.button_4) {
            View categoriesScreen = findViewById(R.id.categories_screen);
            categoriesScreen.setVisibility(View.INVISIBLE);
            View chargesScreen = findViewById(R.id.charges_screen);
            btnCategory1.setVisibility(View.INVISIBLE);
            btnCategory2.setVisibility(View.INVISIBLE);
            btnCategory3.setVisibility(View.INVISIBLE);
            btnCategory4.setVisibility(View.INVISIBLE);
            chargesScreen.setVisibility(View.VISIBLE);

            switch (id){
                case R.id.button_1:
                    setKazneScreen();
                    break;
                case R.id.button_2:
                    setPoreziScreen();
                    break;
                case R.id.button_3:
                    setRegScreen();
                    break;
                case R.id.button_4:
                    setOstaloScreen();
                    break;
            }

        } else if (id == R.id.go_to_categories_screen_button || id == R.id.back_to_categories_button) {
            View welcomeScreen = findViewById(R.id.welcome_screen);
            welcomeScreen.setVisibility(View.INVISIBLE);
            View chargesScreen = findViewById(R.id.charges_screen);
            chargesScreen.setVisibility(View.INVISIBLE);
            View categoriesScreen = findViewById(R.id.categories_screen);
            categoriesScreen.setVisibility(View.VISIBLE);
            btnCategory1.setVisibility(View.VISIBLE);
            btnCategory2.setVisibility(View.VISIBLE);
            btnCategory3.setVisibility(View.VISIBLE);
            btnCategory4.setVisibility(View.VISIBLE);
        } else if (id == R.id.accept_button) {
            View donationScreen = findViewById(R.id.donation_screen);
            donationScreen.setVisibility(View.INVISIBLE);
            Stavka donacija = new Stavka("Donacija", donation.floatValue() - billTotal.floatValue(), "");
            addItemToBill(donacija);
            showResultScreen(true);
            performPayment();
        } else if (id == R.id.reject_button) {
            View donationScreen = findViewById(R.id.donation_screen);
            donationScreen.setVisibility(View.INVISIBLE);
            showResultScreen(true);
            performPayment();
        } else if (id == R.id.go_to_bill_screen_button) {
            View billScreen = findViewById(R.id.bill_screen);
            billScreen.setVisibility(View.VISIBLE);
            View chargesScreen = findViewById(R.id.charges_screen);
            chargesScreen.setVisibility(View.INVISIBLE);
        }
        else if (id == R.id.button_pay){
            calculateDonation();
            View billScreen = findViewById(R.id.bill_screen);
            billScreen.setVisibility(View.INVISIBLE);
            View donationScreen = findViewById(R.id.donation_screen);
            donationScreen.setVisibility(View.VISIBLE);
        }
        else if (id == R.id.button_Print){
            ArrayList<EcrJsonReq.PrintLines> lines = new ArrayList<>();
            prepareBillDataLines(lines);
            prepareTransactionReceiptLines(lines, MyApp.transactionList.get(MyApp.transactionList.size()-1));

            printReceipt(lines);
        }
        else if (id == R.id.btnOk || id == R.id.btnSystemClose ){
            MyApp.selectedNewAppType = MyApp.selectedAppType;
            returnToMainScreen();
        }
        else if (id == R.id.btnSystemOk){
            if (!MyApp.selectedNewAppType.equals(MyApp.selectedAppType)) {
                MyApp.selectedAppType = MyApp.selectedNewAppType;
                recreate();
            }
            else{
                returnToMainScreen();
            }
        } else if (chargesButtons.contains(v)) {
            Stavka data = chargesData.get(chargesButtons.indexOf(v));
            if (billDataEntryList.contains(data)) {
                v.setBackgroundColor(Color.argb(0, 120, 120, 120));
                removeItemFromBill(data);
            } else {
                v.setBackgroundColor(Color.argb(50, 120, 120, 120));
                addItemToBill(data);
            }
        } else if (voidButtons.contains(v)) {
            // Void selected transaction
            showResultScreen(true);
            performVoid(MyApp.transactionList.get(voidButtons.indexOf(v)));
        } else if (id == R.id.settlement) {
            showResultScreen(true);
            performSettlement();
        } else {
            if (settingsClickTimer > 0) {
                long currentTime = SystemClock.uptimeMillis();
                if (currentTime > settingsClickTimer + 1000) {
                    settingsClickTimer = 0;
                    settingsClickCount = 0;
                } else {
                    settingsClickCount++;
                }

                if (settingsClickCount >= 5) {
                    settingsClickTimer = 0;
                    settingsClickCount = 0;

                    // Enter settings menu
                    showSystemScreen();
                }
            } else {
                settingsClickTimer = SystemClock.uptimeMillis();
                settingsClickCount = 1;
            }
        }

        if (billTotal.compareTo(BigDecimal.ZERO) > 0) {
            btnPay.setVisibility(View.VISIBLE);
            setBillTableHeader();
            setBillTableData();
        }
        else {
            billTable.removeAllViewsInLayout();

            btnPay.setVisibility(View.INVISIBLE);
            billTotal = BigDecimal.ZERO;
            billDataEntryList.clear();
        }
        billText.fullScroll(View.FOCUS_DOWN);
    }

    private void printReceipt(ArrayList<EcrJsonReq.PrintLines> lines) {
        EcrJsonReq ecrJsonReq = new EcrJsonReq();
        ecrJsonReq.header = new EcrJsonReq.Header();
        ecrJsonReq.request = new EcrJsonReq.Request();
        ecrJsonReq.request.command = new EcrJsonReq.Command();
        ecrJsonReq.request.command.printer = new EcrJsonReq.Printer();
        ecrJsonReq.request.command.printer.type = EcrDef.printerTypeJson;
        ecrJsonReq.request.command.printer.printLines = lines;

        String tempRequest = "\"request\":"+new GsonBuilder().disableHtmlEscaping().create().toJson(ecrJsonReq.request);
        String generatedSHA512 = HashUtils.performSHA512(tempRequest);

        ecrJsonReq.header.version = "01";
        ecrJsonReq.header.length = tempRequest.length();
        ecrJsonReq.header.hash = generatedSHA512;

        String jsonRequest = new GsonBuilder().disableHtmlEscaping().create().toJson(ecrJsonReq);
        Log.d("PRINT", jsonRequest);

        try {
            serviceOrPayment = false;
            resultScreen.setVisibility(View.GONE);
            showResultScreen(true);
            sendJsonStringToApos(jsonRequest, false);
            serviceOrPayment = true;
            returnToMainScreen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void addLine(ArrayList<EcrJsonReq.PrintLines> lines, String type, String style, String content){
        EcrJsonReq.PrintLines line = new EcrJsonReq.PrintLines();
        line.type = type;
        line.style = style;
        line.content = content;
        lines.add(line);
    }

    private void prepareBillDataLines(ArrayList<EcrJsonReq.PrintLines> lines) {
        addLine(lines, EcrDef.lineTypeText, EcrDef.lineStyleNormal, "                                ".substring(0, (32 - mainConfig.title.length())/2) + mainConfig.title);
        addLine(lines, EcrDef.lineTypeText, EcrDef.lineStyleNormal, " ");
        addLine(lines, EcrDef.lineTypeText, EcrDef.lineStyleNormal, " ");

        addLine(lines, EcrDef.lineTypeText, EcrDef.lineStyleNormal, "================================");
        addLine(lines, EcrDef.lineTypeText, EcrDef.lineStyleNormal, "ITEM");
        addLine(lines, EcrDef.lineTypeText, EcrDef.lineStyleNormal, "  QUANTITY                AMOUNT");
        addLine(lines, EcrDef.lineTypeText, EcrDef.lineStyleNormal, "================================");

        for (Stavka bd: billDataEntryList) {
            BigDecimal itemTotal = new BigDecimal(bd.price);

            addLine(lines, EcrDef.lineTypeText, EcrDef.lineStyleNormal, String.valueOf(bd.name));
            addLine(lines, EcrDef.lineTypeText, EcrDef.lineStyleNormal, "  " + formatAmount(itemTotal, true));
        }

        addLine(lines, EcrDef.lineTypeText, EcrDef.lineStyleNormal, "________________________________");
        addLine(lines, EcrDef.lineTypeText, EcrDef.lineStyleNormal, "TOTAL                           ".substring(0, 32 - formatAmount(billTotal, true).length()) + formatAmount(billTotal, true));
        addLine(lines, EcrDef.lineTypeText, EcrDef.lineStyleNormal, " ");
    }

    void prepareTransactionReceiptLines(ArrayList<EcrJsonReq.PrintLines> lines, TransactionData transactionData){
        addLine(lines, EcrDef.lineTypeText, EcrDef.lineStyleNormal, " ");
        addLine(lines, EcrDef.lineTypeText, EcrDef.lineStyleNormal, "================================");
        addLine(lines, EcrDef.lineTypeText, EcrDef.lineStyleNormal, "          PAID BY CARD");
        addLine(lines, EcrDef.lineTypeText, EcrDef.lineStyleNormal, "================================");
        addLine(lines, EcrDef.lineTypeText, EcrDef.lineStyleNormal, "INVOICE: " + transactionData.invoice);
        addLine(lines, EcrDef.lineTypeText, EcrDef.lineStyleNormal, "PAN:     " + transactionData.pan);
        addLine(lines, EcrDef.lineTypeText, EcrDef.lineStyleNormal, "AMOUNT:  " + transactionData.base + " " + transactionData.currencyCode);
        addLine(lines, EcrDef.lineTypeText, EcrDef.lineStyleNormal, "AUTH #:  " + transactionData.authorization);
        addLine(lines, EcrDef.lineTypeText, EcrDef.lineStyleNormal, "________________________________");
        addLine(lines, EcrDef.lineTypeText, EcrDef.lineStyleNormal, " ");
    }

    @SuppressLint("SetTextI18n")
    private void setKazneScreen(){
        TextView categoryTitle = findViewById(R.id.category_name);
        categoryTitle.setText("Kazne");
        if (chargesKazne.size() > 0) {
            TextView chargesText = findViewById(R.id.charges_text);
            chargesText.setText("Odaberite stavku:");

            LinearLayout chargesView = findViewById(R.id.charges_view);
            ScrollView chargesScrollView = findViewById(R.id.charges_scrollView);
            chargesScrollView.setVisibility(View.VISIBLE);
            btnGoToBillScreen.setVisibility(View.VISIBLE);
            chargesView.removeAllViewsInLayout();

            for (Stavka stavka : chargesKazne) {
                addChargeItemToView(stavka, chargesView);
            }
        } else {
            TextView chargesText = findViewById(R.id.charges_text);
            chargesText.setText("Nema neplaćenih kazni.");

            ScrollView chargesScrollView = findViewById(R.id.charges_scrollView);
            chargesScrollView.setVisibility(View.GONE);
            btnGoToBillScreen.setVisibility(View.INVISIBLE);
        }
    }

    private void setPoreziScreen(){
        TextView categoryTitle = findViewById(R.id.category_name);
        categoryTitle.setText("Porezi");
        if (chargesPorezi.size() > 0) {
            TextView chargesText = findViewById(R.id.charges_text);
            chargesText.setText("Odaberite stavku:");

            LinearLayout chargesView = findViewById(R.id.charges_view);
            ScrollView chargesScrollView = findViewById(R.id.charges_scrollView);
            chargesScrollView.setVisibility(View.VISIBLE);
            btnGoToBillScreen.setVisibility(View.VISIBLE);
            chargesView.removeAllViewsInLayout();

            for (Stavka stavka : chargesPorezi) {
                addChargeItemToView(stavka, chargesView);
            }
        } else {
            TextView chargesText = findViewById(R.id.charges_text);
            chargesText.setText("Nema neplaćenih poreza.");

            ScrollView chargesScrollView = findViewById(R.id.charges_scrollView);
            chargesScrollView.setVisibility(View.GONE);
            btnGoToBillScreen.setVisibility(View.INVISIBLE);
        }
    }

    private void setRegScreen(){
        TextView categoryTitle = findViewById(R.id.category_name);
        categoryTitle.setText("Registracija");
        if (chargesRegistracije.size() > 0) {
            TextView chargesText = findViewById(R.id.charges_text);
            chargesText.setText("Odaberite stavku:");

            LinearLayout chargesView = findViewById(R.id.charges_view);
            ScrollView chargesScrollView = findViewById(R.id.charges_scrollView);
            chargesScrollView.setVisibility(View.VISIBLE);
            btnGoToBillScreen.setVisibility(View.VISIBLE);
            chargesView.removeAllViewsInLayout();

            for (Stavka stavka : chargesRegistracije) {
                addChargeItemToView(stavka, chargesView);
            }
        } else {
            TextView chargesText = findViewById(R.id.charges_text);
            chargesText.setText("Nema neplaćenih registracija.");

            ScrollView chargesScrollView = findViewById(R.id.charges_scrollView);
            chargesScrollView.setVisibility(View.GONE);
            btnGoToBillScreen.setVisibility(View.INVISIBLE);
        }
    }

    private void setOstaloScreen(){
        TextView categoryTitle = findViewById(R.id.category_name);
        categoryTitle.setText("Ostale takse");
        if (chargesOstaleTakse.size() > 0) {
            TextView chargesText = findViewById(R.id.charges_text);
            chargesText.setText("Odaberite stavku:");

            LinearLayout chargesView = findViewById(R.id.charges_view);
            ScrollView chargesScrollView = findViewById(R.id.charges_scrollView);
            chargesScrollView.setVisibility(View.VISIBLE);
            btnGoToBillScreen.setVisibility(View.VISIBLE);
            chargesView.removeAllViewsInLayout();

            for (Stavka stavka : chargesOstaleTakse) {
                addChargeItemToView(stavka, chargesView);
            }
        } else {
            TextView chargesText = findViewById(R.id.charges_text);
            chargesText.setText("Nema neplaćenih taksi iz ove kategorije.");

            ScrollView chargesScrollView = findViewById(R.id.charges_scrollView);
            chargesScrollView.setVisibility(View.GONE);
            btnGoToBillScreen.setVisibility(View.INVISIBLE);
        }
    }

    private void addChargeItemToView(Stavka stavka, LinearLayout chargesView){
        LinearLayout chargeItem = new LinearLayout(getApplicationContext());
        chargeItem.setOrientation(LinearLayout.VERTICAL);
        chargeItem.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) chargeItem.getLayoutParams();
        mlp.setMargins(0, 5, 0, 5);


        LinearLayout linearLayout = new LinearLayout(getApplicationContext());
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView itemName = new TextView(getApplicationContext());
        itemName.setText(stavka.name);
        itemName.setTextColor(getResources().getColor(R.color.white));
        itemName.setTextSize(18);
        itemName.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        linearLayout.addView(itemName);

        LinearLayout linearLayout1 = new LinearLayout(getApplicationContext());
        linearLayout1.setOrientation(LinearLayout.HORIZONTAL);
        linearLayout1.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView dateText = new TextView(getApplicationContext());
        dateText.setText("\tDatum: ");
        dateText.setTextColor(getResources().getColor(R.color.white));
        dateText.setTextSize(16);
        dateText.setTypeface(null, Typeface.ITALIC);
        dateText.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        linearLayout1.addView(dateText);

        TextView itemDate = new TextView(getApplicationContext());
        itemDate.setText(stavka.date);
        itemDate.setTextColor(getResources().getColor(R.color.white));
        itemDate.setTextSize(16);
        itemDate.setTypeface(null, Typeface.ITALIC);
        itemDate.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        linearLayout1.addView(itemDate);

        linearLayout.addView(linearLayout1);

        TextView itemPrice = new TextView(getApplicationContext());
        itemPrice.setText(stavka.price.toString());
        itemPrice.setTextColor(getResources().getColor(R.color.white));
        itemPrice.setTextSize(18);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.gravity = Gravity.END;
        itemPrice.setLayoutParams(layoutParams);

        chargeItem.addView(linearLayout);
        chargeItem.addView(itemPrice);

        Button button = new Button(getApplicationContext());
        button.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        if (billDataEntryList.contains(stavka))
            button.setBackgroundColor(Color.argb(50, 120, 120, 120));
        else
            button.setBackgroundColor(Color.argb(0, 120, 120, 120));

        button.setOnClickListener(this);
        chargesButtons.add(button);
        chargesData.add(stavka);

        FrameLayout frameLayout = new FrameLayout(getApplicationContext());

        frameLayout.addView(chargeItem);
        frameLayout.addView(button);

        chargesView.addView(frameLayout);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(connection);
    }

    public static boolean isForeground(Context ctx, String myPackage){
        ActivityManager manager = (ActivityManager) ctx.getSystemService(ACTIVITY_SERVICE);
        List< ActivityManager.RunningTaskInfo > runningTaskInfo = manager.getRunningTasks(1);

        ComponentName componentInfo = runningTaskInfo.get(0).topActivity;
        if(componentInfo.getPackageName().equals(myPackage)) {
            return true;
        }
        return false;
    }

    public static boolean isAppActive(Context context, String packageName) {
        PackageManager pm = context.getPackageManager();
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);

        String value = null;
        for (ApplicationInfo packageInfo : packages) {
            //system apps! get out
            if (!isSTOPPED(packageInfo) && !isSYSTEM(packageInfo)) {

                if(packageInfo.packageName.equals(packageName))
                    return true;
            }
        }
        return false;
    }

    private static boolean isSTOPPED(ApplicationInfo pkgInfo) {

        return ((pkgInfo.flags & ApplicationInfo.FLAG_STOPPED) != 0);
    }

    private static boolean isSYSTEM(ApplicationInfo pkgInfo) {

        return ((pkgInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0);
    }

    // dummy dispatch key event to avoid physical red key press
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        return true;
    }
}

