package com.example.gamehostlib;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;


import com.unity3d.player.UnityPlayer;

import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static android.bluetooth.BluetoothDevice.ACTION_BOND_STATE_CHANGED;
import static android.bluetooth.BluetoothDevice.BOND_BONDED;
import static android.bluetooth.BluetoothDevice.EXTRA_BOND_STATE;

public class cGameMainActivity extends PlayerActivity {
    // Setup activity layout
    private BluetoothAdapter mBluetoothAdapter;
    private static final int REQUEST_ENABLE_BT = 1;
    // stop scan after 10 second
    private static final long SCAN_PERIOD = 100000;
    private Handler mHandler;

    public static String mRingDeviceNameSpace = "BLETEST";


    private static cGameMainActivity m_instance;
    public static cGameMainActivity instance() {
        if (m_instance == null) {
            m_instance = new cGameMainActivity();
        }
        return m_instance;
    }



    public enum ConnectionMode{
        WifiMode,
        BLEMode
    }
    public static ConnectionMode mConnectionMode = ConnectionMode.WifiMode;

    protected ArrayList<BluetoothDevice> mDevicesList = new  ArrayList<BluetoothDevice>();
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        m_instance = this;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                String[] permissions = new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION};
                requestPermissions( permissions, 10);
                return;
            }
        }

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this,"Not supporting ble", Toast.LENGTH_SHORT).show();
        }
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "not supporting ble", Toast.LENGTH_SHORT).show();
        }
        mHandler = new Handler();

        this.registerReceiver(bondStateReceiver, new IntentFilter(ACTION_BOND_STATE_CHANGED));

    }


    @Override
    protected void onResume() {
        super.onResume();
        // 为了确保设备上蓝牙能使用, 如果当前蓝牙设备没启用,弹出对话框向用户要求授予权限来启用
        if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                return;
        }

    }


    protected void unregisterdBLEListener(){
        try {
            mBluetoothAdapter.cancelDiscovery();
            unregisterReceiver(mReceiver);
            UnityPlayer.UnitySendMessage("Main Camera", "BLEScanStop", "");
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.unregisterReceiver(bondStateReceiver);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterdBLEListener();
    //this.unregisterReceiver(bondStateReceiver);
}

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            Toast.makeText(this, "Ble is not allowed", Toast.LENGTH_SHORT).show();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }





    public void BLEConnectionScan(){
        mConnectionMode = ConnectionMode.BLEMode;
        cSocketHelper.printLogForUnity("BLEConnectionScan");

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                String[] permissions = new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION};
                requestPermissions( permissions, 10);
                return;
            }
        }


        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mReceiver, filter);
        UnityPlayer.UnitySendMessage("Main Camera", "BLEScanStart", "");
        mBluetoothAdapter.startDiscovery();
        mDevicesList.clear();
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                //Do something after 100ms
                unregisterdBLEListener();
            }
        }, 10000);

    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent
                        .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                cSocketHelper.printLogForUnity("BLE:: "+ device.getName() );
                if( device.getName() !=null && device.getName().contains(cGameMainActivity.mRingDeviceNameSpace)){
                    unregisterdBLEListener();
                    mDevicesList.add(device);
                    startConnectionToRing(device);
                }
            }
        }
    };



    public BlueToothDeviceRingObject crtRingObj = null;
    public void startConnectionToRing(BluetoothDevice device){

        crtRingObj = new BlueToothDeviceRingObject();
        crtRingObj.address = device.getAddress();
        if(device.getBondState() == BOND_BONDED) {
            try {
                cSocketHelper.printLogForUnity("unbond");
                Method m = device.getClass().getMethod("removeBond", (Class[]) null);
                m.invoke(device, (Object[]) null);
                crtRingObj.isFirstConnected = true;
            } catch (Exception e) {
                cSocketHelper.printLogForUnity("unbond exception");
            }
        }else {
            mBlueToothSocket = null;
            device.createBond();
            cSocketHelper.printLogForUnity("create bond");
        }
    }



    private final BroadcastReceiver bondStateReceiver = new BroadcastReceiver(){

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if(!device.getAddress().equals(crtRingObj.address)){
                cSocketHelper.printLogForUnity("not a ring obj");
                return;
            }
            if (action.equals(ACTION_BOND_STATE_CHANGED)) {
                final int bondState = intent.getIntExtra(EXTRA_BOND_STATE, -999);
                switch (bondState) {
                    case BluetoothDevice.BOND_BONDING:
                        cSocketHelper.printLogForUnity("bonding");
                        break;
                    case BluetoothDevice.BOND_BONDED:
                        cSocketHelper.printLogForUnity("bonded");
                        setupBlueToothSocketConnection(device,new blueToothSocketCallBack() {
                            @Override
                            public void onSocketConnected(BluetoothDevice _device) {
                                sendWelcomeMsgToRing();
                            }

                            @Override
                            public void onSocketReciveData(String dataStr) {
                                handleBlueToothData(dataStr);
                            }

                            @Override
                            public void onSocketError() {

                            }
                        });
                        break;
                    case BluetoothDevice.BOND_NONE:
                        cSocketHelper.printLogForUnity("bond_none");
                        if(crtRingObj.isFirstConnected == true){
                            crtRingObj.isFirstConnected = false;
                            BLEConnectionScan();
                        }else{
                            crtRingObj = null;
                            mBlueToothSocket = null;
                        }
                        break;
                }
            }
        }
    };

    public void handleBlueToothData(String dataStr){

        try {

            JSONObject obj = new JSONObject(dataStr);
            String action = obj.getString("action");
            if(action.equalsIgnoreCase("DisplayCalibration")){
                UnityPlayer.UnitySendMessage("Main Camera", "displayCalibration", "");
            }


            if(action.equalsIgnoreCase("InGameType")){
                UnityPlayer.UnitySendMessage("Main Camera", "onWebSocketReceiveData", dataStr);
            }

            if(action.equalsIgnoreCase("StartCalibrating")){
                UnityPlayer.UnitySendMessage("Main Camera", "StartCalibration", dataStr);
            }

            if(action.equalsIgnoreCase("CompleteCalibrating")){
                UnityPlayer.UnitySendMessage("Main Camera", "StopCalibration", dataStr);
            }
        } catch (Exception c) {
            cSocketHelper.printLogForUnity("cannot parse response");
        }
    }

    interface blueToothSocketCallBack {
        void onSocketConnected(BluetoothDevice _device);
        void onSocketReciveData(String dataStr);
        void onSocketError();
    }

    protected void setupBlueToothSocketConnection(final BluetoothDevice device,final blueToothSocketCallBack _finishcallBack){
        new Thread(new Runnable() {
            public void run() {

                if(mBlueToothSocket==null) {
                    try {
                        mBlueToothSocket = device.createInsecureRfcommSocketToServiceRecord(MY_UUID_INSECURE);
                    } catch (Exception e) {
                        cSocketHelper.printLogForUnity("create socket except");
                    }
                }

                if(!mBlueToothSocket.isConnected()) {
                    try {
                        mBlueToothSocket.connect();
                    } catch (Exception e) {
                        cSocketHelper.printLogForUnity("socket connect");
                        e.printStackTrace();
                        try {
                            Log.e("", "trying fallback...");
                            mBlueToothSocket = (BluetoothSocket) device.getClass().getMethod("createRfcommSocket", new Class[]{int.class}).invoke(device, 1);
                            mBlueToothSocket.connect();
                            Log.e("", "Connected");
                        } catch (Exception e1) {
                            cSocketHelper.printLogForUnity("socket connect 22 ");
                            mBlueToothSocket = null;
                        }
                    }
                }

                Runnable myRunnable = new Runnable() {
                    @Override
                    public void run() {
                        _finishcallBack.onSocketConnected(device);
                    } // This is your code
                };
                mHandler.post(myRunnable);

                byte[] buffer = new byte[0];  // buffer store for the stream

                while (true && mBlueToothSocket!=null) {
                    // Read from the InputStream
                    try {
                        buffer = new byte[mBlueToothSocket.getInputStream().available()];
                        mBlueToothSocket.getInputStream().read(buffer);
                        final String incomingMessage = new String(buffer);
                        if(incomingMessage.length()>0) {
                            Runnable myRunnable2 = new Runnable() {
                                @Override
                                public void run() {
                                    _finishcallBack.onSocketReciveData(incomingMessage);
                                } // This is your code
                            };
                            mHandler.post(myRunnable2);
                            cSocketHelper.printLogForUnity("incoming bluetoothMsg " + incomingMessage);
                        }
                    } catch (Exception e) {
                        cSocketHelper.printLogForUnity("get msg fail");
                        mBlueToothSocket = null;
                        return;
                    }
                }
            }
        }).start();

    }

    protected void sendWelcomeMsgToRing(){
        sendMsg("welcomeMsg");
    }

    protected void sendRequestCalibration(){
        sendMsg("RequestCalibration");
    }

    protected BluetoothSocket mBlueToothSocket = null;
    private  final UUID MY_UUID_INSECURE =
            UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");

    public void sendMsg(final String dataStr){
        new Thread(new Runnable() {
            public void run() {
                if(mBlueToothSocket!=null && mBlueToothSocket.isConnected()){
                    try {
                        OutputStream tmpOut = mBlueToothSocket.getOutputStream();
                        byte[] bytes = dataStr.getBytes(Charset.defaultCharset());
                        tmpOut.write(bytes);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }else{
                    cSocketHelper.printLogForUnity("socket is null or not connected");
                }
            }
        }).start();
    }

}



