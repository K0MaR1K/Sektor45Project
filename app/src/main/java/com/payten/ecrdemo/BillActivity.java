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
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
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


public class BillActivity extends AppCompatActivity implements View.OnClickListener/*, View.OnTouchListener*/ {
    public static Resources.Theme theme;

    public static final int ECR_NONE = 0;
    public static final int ECR_TRANSACTION = 1;
    public static final int ECR_SETTLE = 2;
    public static final int ECR_VOID = 3;

    public static int ecrAction = ECR_NONE;
    public static int voidTranIndex;

    public class OnSwipeTouchListener implements View.OnTouchListener {

        private final GestureDetector gestureDetector;

        public OnSwipeTouchListener (Context ctx){
            gestureDetector = new GestureDetector(ctx, new GestureListener());
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            return gestureDetector.onTouchEvent(event);
        }

        private final class GestureListener extends GestureDetector.SimpleOnGestureListener {

            private static final int SWIPE_THRESHOLD = 100;
            private static final int SWIPE_VELOCITY_THRESHOLD = 100;

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                boolean result = false;
                try {
                    float diffY = e2.getY() - e1.getY();
                    float diffX = e2.getX() - e1.getX();
                    if (Math.abs(diffX) > Math.abs(diffY)) {
                        if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                            if (diffX > 0) {
                                onSwipeRight();
                            } else {
                                onSwipeLeft();
                            }
                            result = true;
                        }
                    }
                    else if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffY > 0) {
                            onSwipeBottom();
                        } else {
                            onSwipeTop();
                        }
                        result = true;
                    }
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
                return result;
            }
        }

        public void onSwipeRight() {
            if (mainConfig.items.size() > 9){
                if (buttonOffset > 0){
                    buttonOffset -= 9;
                    setButtons();
                }
            }
        }

        public void onSwipeLeft() {
            if (mainConfig.items.size() > 9){
                if (buttonOffset < mainConfig.items.size() - 9){
                    buttonOffset += 9;
                    setButtons();
                }
            }
        }

        public void onSwipeTop() {
        }

        public void onSwipeBottom() {
        }
    }

    class BillDataEntry{
        public Integer itemId;
        public Integer quantity;
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

    int[] buttonItems = new int[9];

    int buttonOffset = 0;
    ImageButton  btnLeft;
    ImageButton  btnRight;
    ImageButton  btn1;
    ImageButton  btn2;
    ImageButton  btn3;
    ImageButton  btn4;
    ImageButton  btn5;
    ImageButton  btn6;
    ImageButton  btn7;
    ImageButton  btn8;
    ImageButton  btn9;
    ScrollView billText;
    ImageButton  btnPay;
    ImageButton  btnCancel;
    TableLayout billTable;

    ArrayList<BillDataEntry> billDataEntryList = new ArrayList<>();

    BigDecimal billTotal = BigDecimal.ZERO;

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

    RadioGroup appTypes;
    ArrayList<String> appTypesNames = new ArrayList<>();
    ArrayList<RadioButton> appTypesButtons = new ArrayList<>();

    TableLayout transTable;
    ScrollView transScroll;

    ArrayList<ImageButton> voidButtons = new ArrayList<>();
    ArrayList<ImageButton> itemButtons = new ArrayList<>();

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

    void setButtonImage(ImageButton btn, int index){
        String imageName = mainConfig.items.get(index).itemImage;
        int id = getResources().getIdentifier(imageName.substring(0, imageName.length()-4),"drawable", getPackageName());
        btn.setImageResource(id);
    }

    void setButtons(){
        int numItemsToDisplay = 9;
        if (mainConfig.items.size() < 9){
            numItemsToDisplay = mainConfig.items.size();
            btnLeft.setVisibility(View.GONE);
            btnRight.setVisibility(View.GONE);
        }
        else {
            if (buttonOffset == 0) {
                btnLeft.setVisibility(View.INVISIBLE);
            }
            else{
                btnLeft.setVisibility(View.VISIBLE);
            }
            if (buttonOffset >= mainConfig.items.size() - 9) {
                btnRight.setVisibility(View.INVISIBLE);
            }
            else{
                btnRight.setVisibility(View.VISIBLE);
            }
        }
        switch (numItemsToDisplay){
            case 1:
                btn1.setVisibility(View.VISIBLE);
                btn2.setVisibility(View.GONE);
                btn3.setVisibility(View.GONE);
                btn4.setVisibility(View.GONE);
                btn5.setVisibility(View.GONE);
                btn6.setVisibility(View.GONE);
                btn7.setVisibility(View.GONE);
                btn8.setVisibility(View.GONE);
                btn9.setVisibility(View.GONE);

                buttonItems[0] = buttonOffset + 0;
                setButtonImage(btn1, buttonOffset + 0);
                break;
            case 2:
                btn1.setVisibility(View.VISIBLE);
                btn2.setVisibility(View.GONE);
                btn3.setVisibility(View.GONE);
                btn4.setVisibility(View.VISIBLE);
                btn5.setVisibility(View.GONE);
                btn6.setVisibility(View.GONE);
                btn7.setVisibility(View.GONE);
                btn8.setVisibility(View.GONE);
                btn9.setVisibility(View.GONE);

                buttonItems[0] = buttonOffset + 0;
                setButtonImage(btn1, buttonOffset + 0);
                buttonItems[3] = buttonOffset + 1;
                setButtonImage(btn4, buttonOffset + 1);
                break;
            case 3:
                btn1.setVisibility(View.VISIBLE);
                btn2.setVisibility(View.GONE);
                btn3.setVisibility(View.GONE);
                btn4.setVisibility(View.VISIBLE);
                btn5.setVisibility(View.GONE);
                btn6.setVisibility(View.GONE);
                btn7.setVisibility(View.VISIBLE);
                btn8.setVisibility(View.GONE);
                btn9.setVisibility(View.GONE);

                buttonItems[0] = buttonOffset + 0;
                setButtonImage(btn1, buttonOffset + 0);
                buttonItems[3] = buttonOffset + 1;
                setButtonImage(btn4, buttonOffset + 1);
                buttonItems[6] = buttonOffset + 2;
                setButtonImage(btn7, buttonOffset + 2);
                break;
            case 4:
                btn1.setVisibility(View.VISIBLE);
                btn2.setVisibility(View.VISIBLE);
                btn3.setVisibility(View.GONE);
                btn4.setVisibility(View.VISIBLE);
                btn5.setVisibility(View.VISIBLE);
                btn6.setVisibility(View.GONE);
                btn7.setVisibility(View.GONE);
                btn8.setVisibility(View.GONE);
                btn9.setVisibility(View.GONE);

                buttonItems[0] = buttonOffset + 0;
                setButtonImage(btn1, buttonOffset + 0);
                buttonItems[1] = buttonOffset + 1;
                setButtonImage(btn2, buttonOffset + 1);
                buttonItems[3] = buttonOffset + 2;
                setButtonImage(btn4, buttonOffset + 2);
                buttonItems[4] = buttonOffset + 3;
                setButtonImage(btn5, buttonOffset + 3);
                break;
            case 5:
                btn1.setVisibility(View.VISIBLE);
                btn2.setVisibility(View.VISIBLE);
                btn3.setVisibility(View.VISIBLE);
                btn4.setVisibility(View.VISIBLE);
                btn5.setVisibility(View.VISIBLE);
                btn6.setVisibility(View.INVISIBLE);
                btn7.setVisibility(View.GONE);
                btn8.setVisibility(View.GONE);
                btn9.setVisibility(View.GONE);

                buttonItems[0] = buttonOffset + 0;
                setButtonImage(btn1, buttonOffset + 0);
                buttonItems[1] = buttonOffset + 1;
                setButtonImage(btn2, buttonOffset + 1);
                buttonItems[2] = buttonOffset + 2;
                setButtonImage(btn3, buttonOffset + 2);
                buttonItems[3] = buttonOffset + 3;
                setButtonImage(btn4, buttonOffset + 3);
                buttonItems[4] = buttonOffset + 4;
                setButtonImage(btn5, buttonOffset + 4);
                break;
            case 6:
                btn1.setVisibility(View.VISIBLE);
                btn2.setVisibility(View.VISIBLE);
                btn3.setVisibility(View.VISIBLE);
                btn4.setVisibility(View.VISIBLE);
                btn5.setVisibility(View.VISIBLE);
                btn6.setVisibility(View.VISIBLE);
                btn7.setVisibility(View.GONE);
                btn8.setVisibility(View.GONE);
                btn9.setVisibility(View.GONE);

                buttonItems[0] = buttonOffset + 0;
                setButtonImage(btn1, buttonOffset + 0);
                buttonItems[1] = buttonOffset + 1;
                setButtonImage(btn2, buttonOffset + 1);
                buttonItems[2] = buttonOffset + 2;
                setButtonImage(btn3, buttonOffset + 2);
                buttonItems[3] = buttonOffset + 3;
                setButtonImage(btn4, buttonOffset + 3);
                buttonItems[4] = buttonOffset + 4;
                setButtonImage(btn5, buttonOffset + 4);
                buttonItems[5] = buttonOffset + 5;
                setButtonImage(btn6, buttonOffset + 5);
                break;
            case 7:
                btn1.setVisibility(View.VISIBLE);
                btn2.setVisibility(View.VISIBLE);
                btn3.setVisibility(View.VISIBLE);
                btn4.setVisibility(View.VISIBLE);
                btn5.setVisibility(View.VISIBLE);
                btn6.setVisibility(View.VISIBLE);
                btn7.setVisibility(View.VISIBLE);
                btn8.setVisibility(View.INVISIBLE);
                btn9.setVisibility(View.INVISIBLE);

                buttonItems[0] = buttonOffset + 0;
                setButtonImage(btn1, buttonOffset + 0);
                buttonItems[1] = buttonOffset + 1;
                setButtonImage(btn2, buttonOffset + 1);
                buttonItems[2] = buttonOffset + 2;
                setButtonImage(btn3, buttonOffset + 2);
                buttonItems[3] = buttonOffset + 3;
                setButtonImage(btn4, buttonOffset + 3);
                buttonItems[4] = buttonOffset + 4;
                setButtonImage(btn5, buttonOffset + 4);
                buttonItems[5] = buttonOffset + 5;
                setButtonImage(btn6, buttonOffset + 5);
                buttonItems[6] = buttonOffset + 6;
                setButtonImage(btn7, buttonOffset + 6);
                break;
            case 8:
                btn1.setVisibility(View.VISIBLE);
                btn2.setVisibility(View.VISIBLE);
                btn3.setVisibility(View.VISIBLE);
                btn4.setVisibility(View.VISIBLE);
                btn5.setVisibility(View.VISIBLE);
                btn6.setVisibility(View.VISIBLE);
                btn7.setVisibility(View.VISIBLE);
                btn8.setVisibility(View.VISIBLE);
                btn9.setVisibility(View.INVISIBLE);

                buttonItems[0] = buttonOffset + 0;
                setButtonImage(btn1, buttonOffset + 0);
                buttonItems[1] = buttonOffset + 1;
                setButtonImage(btn2, buttonOffset + 1);
                buttonItems[2] = buttonOffset + 2;
                setButtonImage(btn3, buttonOffset + 2);
                buttonItems[3] = buttonOffset + 3;
                setButtonImage(btn4, buttonOffset + 3);
                buttonItems[4] = buttonOffset + 4;
                setButtonImage(btn5, buttonOffset + 4);
                buttonItems[5] = buttonOffset + 5;
                setButtonImage(btn6, buttonOffset + 5);
                buttonItems[6] = buttonOffset + 6;
                setButtonImage(btn7, buttonOffset + 6);
                buttonItems[7] = buttonOffset + 7;
                setButtonImage(btn8, buttonOffset + 7);
                break;
            case 9:
            default:
                if (mainConfig.items.size() > buttonOffset + 0) {
                    btn1.setVisibility(View.VISIBLE);
                    buttonItems[0] = buttonOffset + 0;
                    setButtonImage(btn1, buttonOffset + 0);
                }
                else{
                    btn1.setVisibility(View.INVISIBLE);
                }
                if (mainConfig.items.size() > buttonOffset + 1) {
                    btn2.setVisibility(View.VISIBLE);
                    buttonItems[1] = buttonOffset + 1;
                    setButtonImage(btn2, buttonOffset + 1);
                }
                else{
                    btn2.setVisibility(View.INVISIBLE);
                }
                if (mainConfig.items.size() > buttonOffset + 2) {
                    btn3.setVisibility(View.VISIBLE);
                    buttonItems[2] = buttonOffset + 2;
                    setButtonImage(btn3, buttonOffset + 2);
                }
                else{
                    btn3.setVisibility(View.INVISIBLE);
                }
                if (mainConfig.items.size() > buttonOffset + 3) {
                    btn4.setVisibility(View.VISIBLE);
                    buttonItems[3] = buttonOffset + 3;
                    setButtonImage(btn4, buttonOffset + 3);
                }
                else{
                    btn4.setVisibility(View.INVISIBLE);
                }
                if (mainConfig.items.size() > buttonOffset + 4) {
                    btn5.setVisibility(View.VISIBLE);
                    buttonItems[4] = buttonOffset + 4;
                    setButtonImage(btn5, buttonOffset + 4);
                }
                else{
                    btn5.setVisibility(View.INVISIBLE);
                }
                if (mainConfig.items.size() > buttonOffset + 5) {
                    btn6.setVisibility(View.VISIBLE);
                    buttonItems[5] = buttonOffset + 5;
                    setButtonImage(btn6, buttonOffset + 5);
                }
                else{
                    btn6.setVisibility(View.INVISIBLE);
                }
                if (mainConfig.items.size() > buttonOffset + 6) {
                    btn7.setVisibility(View.VISIBLE);
                    buttonItems[6] = buttonOffset + 6;
                    setButtonImage(btn7, buttonOffset + 6);
                }
                else{
                    btn7.setVisibility(View.INVISIBLE);
                }
                if (mainConfig.items.size() > buttonOffset + 7) {
                    btn8.setVisibility(View.VISIBLE);
                    buttonItems[7] = buttonOffset + 7;
                    setButtonImage(btn8, buttonOffset + 7);
                }
                else{
                    btn8.setVisibility(View.INVISIBLE);
                }
                if (mainConfig.items.size() > buttonOffset + 8) {
                    btn9.setVisibility(View.VISIBLE);
                    buttonItems[8] = buttonOffset + 8;
                    setButtonImage(btn9, buttonOffset + 8);
                }
                else{
                    btn9.setVisibility(View.INVISIBLE);
                }
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        settingsClickTimer = 0;
        settingsClickCount = 0;

        getAppTypes();
        if (MyApp.selectedAppType.equals("")) {
            MyApp.selectedAppType = "config/RecordStore.json";
            MyApp.selectedNewAppType = "config/RecordStore.json";
        }

        loadConfig(MyApp.selectedAppType);
        setAppTheme();
        theme = getTheme();

        MyApp.transactionList = TransactionUtils.loadTransactions();

        setContentView(R.layout.activity_main);

        btnPay = findViewById(R.id.button_Pay);
        btnCancel = findViewById(R.id.button_Clear);

        btnLeft.setOnClickListener(this);
        btnRight.setOnClickListener(this);

        btn1.setOnClickListener(this);
        btn2.setOnClickListener(this);
        btn3.setOnClickListener(this);
        btn4.setOnClickListener(this);
        btn5.setOnClickListener(this);
        btn6.setOnClickListener(this);
        btn7.setOnClickListener(this);
        btn8.setOnClickListener(this);
        btn9.setOnClickListener(this);

        btnPay.setOnClickListener(this);
        btnCancel.setOnClickListener(this);

        billText = findViewById(R.id.BillView);
        billTable = findViewById(R.id.BillTable);

        findViewById(R.id.mainScreen).setOnClickListener(this);

        findViewById(R.id.mainScreen).setOnTouchListener(new OnSwipeTouchListener(this));

        setButtons();
        btnPay.setVisibility(View.INVISIBLE);
        btnCancel.setVisibility(View.INVISIBLE);
        billTable.removeAllViewsInLayout();

        mainScreen = findViewById(R.id.mainScreen);
        resultScreen = findViewById(R.id.resultScreen);
        systemScreen = findViewById(R.id.systemScreen);
        mainScreen.setVisibility(View.VISIBLE);
        resultScreen.setVisibility(View.GONE);
        systemScreen.setVisibility(View.GONE);

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

        appTypes = findViewById(R.id.appTypes);

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
    }

    private void getAppTypes() {
        appTypesNames = new ArrayList<>();
        appTypesButtons = new ArrayList<>();

        try {
            String[] files = MyApp.appContext.getAssets().list("config");
            for (String file: files) {
                appTypesNames.add("config/" + file);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setAppTheme() {
        int theme = R.style.MainTheme;
        String themeName = mainConfig.colorScheme;
        if (themeName != null){
            if (themeName.equalsIgnoreCase("ORANGE")){
                theme = R.style.OrangeTheme;
            }
            else if (themeName.equalsIgnoreCase("MAGENTA")){
                theme = R.style.MagentaTheme;
            }
            else if (themeName.equalsIgnoreCase("GREEN") || themeName.equalsIgnoreCase("LIZZARD")){
                theme = R.style.GreenTheme;
            }
            else if (themeName.equalsIgnoreCase("TEAL")){
                theme = R.style.TealTheme;
            }
            else if (themeName.equalsIgnoreCase("PURPLE")){
                theme = R.style.PurpleTheme;
            }
            else if (themeName.equalsIgnoreCase("YELLOW")){
                theme = R.style.YellowTheme;
            }
            else if (themeName.equalsIgnoreCase("NAVY")){
                theme = R.style.NavyTheme;
            }
            else if (themeName.equalsIgnoreCase("RED")){
                theme = R.style.RedTheme;
            }
            else if (themeName.equalsIgnoreCase("GRAY") || themeName.equalsIgnoreCase("GREY") || themeName.equalsIgnoreCase("SILVER")){
                theme = R.style.GrayTheme;
            }
            else if (themeName.equalsIgnoreCase("LILAC") || themeName.equalsIgnoreCase("VIOLET")){
                theme = R.style.LilacTheme;
            }
            else if (themeName.equalsIgnoreCase("CYAN")  || themeName.equalsIgnoreCase("SKY")){
                theme = R.style.CyanTheme;
            }
        }

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

    void addItemToBill(Integer itemIndex){
        settingsClickTimer = 0;
        settingsClickCount = 0;

        btnPay.setVisibility(View.VISIBLE);
        btnCancel.setVisibility(View.VISIBLE);
        Integer quantity = 1;

        for (BillDataEntry b: billDataEntryList) {
            if (b.itemId.equals(itemIndex)){
                // Item already added - increment quantity, delete from list
                quantity = b.quantity + 1;
                billDataEntryList.remove(b);
                break;
            }
        }

        // Add item at the bottom of the list
        BillDataEntry newItem = new BillDataEntry();
        newItem.itemId = itemIndex;
        newItem.quantity = quantity;
        billDataEntryList.add(newItem);
        BigDecimal amt = new BigDecimal(mainConfig.items.get(itemIndex).itemPrice);
        billTotal = billTotal.add(amt);
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
    }

    void showSystemScreen(){
        mainScreen.setVisibility(View.GONE);
        systemScreen.setVisibility(View.VISIBLE);
        resultScreen.setVisibility(View.GONE);

        appTypesButtons = new ArrayList<>();
        appTypes.removeAllViewsInLayout();
        for (String configFile: appTypesNames) {
            String configJson;
            configJson = getAssetJsonData(getApplicationContext(), configFile);

            try {
                MainConfig tmpConfig = new Gson().fromJson(configJson, MainConfig.class);
                if (tmpConfig.items != null && tmpConfig.items.size() > 0 && tmpConfig.title != ""){
                    RadioButton rb = new RadioButton(this);
                    rb.setText(tmpConfig.title);
                    rb.setTextColor(Utils.getColorFromAttribute(R.attr.colorOnSecondary));
                    if (configFile.equals(MyApp.selectedAppType)){
                        rb.setChecked(true);
                    }
                    rb.setOnClickListener(this);
                    appTypes.addView(rb);
                    appTypesButtons.add(rb);
                }
                else{
                    appTypesNames.remove(configFile);
                }
            }
            catch (Exception e){
            }
        }

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
        b3.setTextSize(12);
        b3.setTypeface(b3.getTypeface(), Typeface.BOLD);
        tr.addView(b3);

        TextView b4=new TextView(this);
        b4.setPadding(20, 0, 0, 0);
        b4.setGravity(Gravity.RIGHT);
        b4.setTextSize(12);
        b4.setText("Invoice");
        b4.setTextColor(getColor(R.color.anthracite));
        b4.setTypeface(b4.getTypeface(), Typeface.BOLD);
        tr.addView(b4);

        TextView b5=new TextView(this);
        b5.setPadding(40, 0, 0, 0);
        b5.setGravity(Gravity.RIGHT);
        b5.setText("Amount");
        b5.setTextColor(getColor(R.color.anthracite));
        b5.setTextSize(12);
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
        b6.setTextSize(12);
        b6.setTypeface(b6.getTypeface(), Typeface.BOLD);
        tr2.addView(b6);

        TextView b7=new TextView(this);
        b7.setPadding(20, 0, 0, 0);
        b7.setTextSize(12);
        b7.setText("Card");
        b7.setTextColor(getColor(R.color.anthracite));
        b7.setTypeface(b7.getTypeface(), Typeface.BOLD);
        tr2.addView(b7);

        TextView b8=new TextView(this);
        b8.setPadding(40, 0, 0, 0);
        b8.setGravity(Gravity.RIGHT);
        b8.setText("Auth #");
        b8.setTextColor(getColor(R.color.anthracite));
        b8.setTextSize(12);
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
                b3.setTextSize(12);
                if (td.voided){
                    b3.setTextColor(getColor(R.color.red));
                }
                tr.addView(b3);

                TextView b4 = new TextView(this);
                b4.setPadding(20, 0, 0, 0);
                b4.setGravity(Gravity.RIGHT);
                b4.setTextSize(12);
                b4.setText(td.invoice);
                if (td.voided){
                    b4.setTextColor(getColor(R.color.red));
                }
                tr.addView(b4);

                TextView b5 = new TextView(this);
                b5.setPadding(40, 0, 0, 0);
                b5.setGravity(Gravity.RIGHT);
                b5.setText(td.base + " " + td.currencyCode);
                b5.setTextSize(12);
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
                tr2.setLayoutParams(new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT));
                tr2.setBackgroundColor(color);

                TextView b6 = new TextView(this);
                b6.setText(td.pan.substring(td.pan.length() - 8));
                b6.setTextSize(12);
                if (td.voided){
                    b6.setTextColor(getColor(R.color.red));
                }
                tr2.addView(b6);

                TextView b7 = new TextView(this);
                b7.setPadding(20, 0, 0, 0);
                b7.setTextSize(12);
                b7.setText(td.cardName);
                if (td.voided){
                    b7.setTextColor(getColor(R.color.red));
                }
                tr2.addView(b7);

                TextView b8 = new TextView(this);
                b8.setPadding(40, 0, 0, 0);
                b8.setGravity(Gravity.RIGHT);
                b8.setText(td.authorization);
                b8.setTextSize(12);
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

        tr.setLayoutParams(new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT));

        TextView b3=new TextView(this);
        b3.setText("Item");
        b3.setTextColor(getColor(R.color.anthracite));
        b3.setTextSize(12);
        b3.setTypeface(b3.getTypeface(), Typeface.BOLD);
        tr.addView(b3);

        TextView b4=new TextView(this);
        b4.setPadding(20, 0, 0, 0);
        b4.setGravity(Gravity.RIGHT);
        b4.setTextSize(12);
        b4.setText("Quantity");
        b4.setTextColor(getColor(R.color.anthracite));
        b4.setTypeface(b4.getTypeface(), Typeface.BOLD);
        tr.addView(b4);

        TextView b5=new TextView(this);
        b5.setPadding(40, 0, 0, 0);
        b5.setGravity(Gravity.RIGHT);
        b5.setText("Amount");
        b5.setTextColor(getColor(R.color.anthracite));
        b5.setTextSize(12);
        b5.setTypeface(b5.getTypeface(), Typeface.BOLD);
        tr.addView(b5);
        billTable.addView(tr);

        final View vline = new View(this);
        vline.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, 2));
        vline.setBackgroundColor(getColor(R.color.anthracite));
        billTable.addView(vline); // add line below heading

        billText.fullScroll(View.FOCUS_DOWN);
    }

    void setBillTableData(){
        itemButtons = new ArrayList<>();
        int i = 0;
        for (BillDataEntry bd: billDataEntryList) {
            int color;
            if (i % 2 == 0) {
                color = getColor(R.color.white);
            } else {
                color = getColor(R.color.light_gray);
            }
            i++;
            BigDecimal itemTotal = new BigDecimal(mainConfig.items.get(bd.itemId).itemPrice);
            itemTotal = itemTotal.multiply(new BigDecimal(bd.quantity));

            TableRow tr=new TableRow(this);
            tr.setLayoutParams(new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT));
            tr.setBackgroundColor(color);

            TextView b=new TextView(this);
            String str=String.valueOf(mainConfig.items.get(bd.itemId).itemName);
            b.setText(str);
            b.setTextSize(12);
            tr.addView(b);

            TextView b1=new TextView(this);
            b1.setPadding(20, 0, 0, 0);
            b1.setGravity(Gravity.RIGHT);
            b1.setTextSize(12);
            b1.setText(String.valueOf(bd.quantity));
            tr.addView(b1);

            TextView b2=new TextView(this);
            b2.setPadding(40, 0, 0, 0);
            b2.setGravity(Gravity.RIGHT);
            b2.setText(formatAmount(itemTotal, true));
            b2.setTextSize(12);
            tr.addView(b2);

            ImageButton i1 =new ImageButton(this);
            i1.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.colorCancel)));
            i1.setImageResource(R.drawable.ic_cancel);
            i1.setScaleType(ImageView.ScaleType.FIT_CENTER);
            tr.addView(i1);

            i1.setOnClickListener(this);
            itemButtons.add(i1);

            billTable.addView(tr);
        }

        if (billTotal.compareTo(BigDecimal.ZERO) > 0) {
            final View vline = new View(this);
            vline.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, 2));
            vline.setBackgroundColor(getColor(R.color.anthracite));
            billTable.addView(vline); // add line below data

            TableRow tr = new TableRow(this);

            tr.setLayoutParams(new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT));

            TextView b3 = new TextView(this);
            b3.setText("Total");
            b3.setTextColor(getColor(R.color.anthracite));
            b3.setTextSize(12);
            b3.setTypeface(b3.getTypeface(), Typeface.BOLD);
            tr.addView(b3);

            TextView b4 = new TextView(this);
            b4.setPadding(20, 0, 0, 0);
            b4.setGravity(Gravity.RIGHT);
            b4.setTextSize(12);
            b4.setText("");
            b4.setTypeface(b4.getTypeface(), Typeface.BOLD);
            b4.setTextColor(getColor(R.color.anthracite));
            tr.addView(b4);

            TextView b5 = new TextView(this);
            b5.setPadding(40, 0, 0, 0);
            b5.setGravity(Gravity.RIGHT);
            b5.setText(formatAmount(billTotal, true));
            b5.setTextColor(getColor(R.color.anthracite));
            b5.setTextSize(12);
            b5.setTypeface(b5.getTypeface(), Typeface.BOLD);
            tr.addView(b5);
            billTable.addView(tr);
        }
    }

    void returnToMainScreen(){
        mainScreen.setVisibility(View.VISIBLE);
        resultScreen.setVisibility(View.GONE);
        systemScreen.setVisibility(View.GONE);

        if (resultScreenTimer != null) {
            resultScreenTimer.cancel();
        }

        billDataEntryList.clear();
        billTotal = BigDecimal.ZERO;
        btnPay.setVisibility(View.INVISIBLE);
        btnCancel.setVisibility(View.INVISIBLE);

        billTable.removeAllViewsInLayout();
        itemButtons = new ArrayList<>();
    }

    @Override
    public void onClick(View v){
        int id = v.getId();

        if (id == R.id.button_Clear){
            btnPay.setVisibility(View.INVISIBLE);
            btnCancel.setVisibility(View.INVISIBLE);
            itemButtons = new ArrayList<>();
            billTotal = BigDecimal.ZERO;
            billDataEntryList.clear();
        }
        else if (id == R.id.button_Pay){
            showResultScreen(true);
            performPayment();
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
        }
        else if (appTypesButtons.contains(v)){
            MyApp.selectedNewAppType = appTypesNames.get(appTypesButtons.indexOf(v));
            if (!MyApp.selectedNewAppType.equals(MyApp.selectedAppType)){
                appTypesButtons.get(appTypesNames.indexOf(MyApp.selectedAppType)).setChecked(false);
                appTypesButtons.get(appTypesNames.indexOf(MyApp.selectedNewAppType)).setChecked(true);
            }
        }
        else if (voidButtons.contains(v)){
            // Void selected transaction
            showResultScreen(true);
            performVoid(MyApp.transactionList.get(voidButtons.indexOf(v)));
        }
        else if (itemButtons.contains(v)){
            // Remove selected item
            billTotal = billTotal.subtract(new BigDecimal(mainConfig.items.get(billDataEntryList.get(itemButtons.indexOf(v)).itemId).itemPrice).multiply(new BigDecimal(billDataEntryList.get(itemButtons.indexOf(v)).quantity)));
            billDataEntryList.remove(itemButtons.indexOf(v));
            itemButtons.remove(v);
        }
        else if (id == R.id.settlement){
            showResultScreen(true);
            performSettlement();
        }
        else {
            if (settingsClickTimer > 0){
                long currentTime = SystemClock.uptimeMillis();
                if (currentTime > settingsClickTimer + 1000){
                    settingsClickTimer = 0;
                    settingsClickCount = 0;
                }
                else{
                    settingsClickCount ++;
                }

                if (settingsClickCount >= 5){
                    settingsClickTimer = 0;
                    settingsClickCount = 0;

                    // Enter settings menu
                    showSystemScreen();
                }
            }
            else {
                settingsClickTimer = SystemClock.uptimeMillis();
                settingsClickCount = 1;
            }
        }

        if (billTotal.compareTo(BigDecimal.ZERO) > 0) {
            setBillTableHeader();
            setBillTableData();
        }
        else {
            billTable.removeAllViewsInLayout();
            itemButtons = new ArrayList<>();

            btnPay.setVisibility(View.INVISIBLE);
            btnCancel.setVisibility(View.INVISIBLE);
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

        for (BillDataEntry bd: billDataEntryList) {
            BigDecimal itemTotal = new BigDecimal(mainConfig.items.get(bd.itemId).itemPrice).multiply(new BigDecimal(bd.quantity));

            addLine(lines, EcrDef.lineTypeText, EcrDef.lineStyleNormal, String.valueOf(mainConfig.items.get(bd.itemId).itemName));
            addLine(lines, EcrDef.lineTypeText, EcrDef.lineStyleNormal, "  " + String.valueOf(bd.quantity) + "                                ".substring(0, 30 - String.valueOf(bd.quantity).length() - formatAmount(itemTotal, true).length()) + formatAmount(itemTotal, true));
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
