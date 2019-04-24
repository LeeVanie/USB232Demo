package org.loop.usb232demo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.ftdi.j2xx.D2xxManager;
import com.ftdi.j2xx.FT_Device;
import com.ftdi.j2xx.FT_EEPROM;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    FT_Device ftDev = null;
    int DevCount = -1;
    int currentIndex = -1;
    int openIndex = 0;
    public static D2xxManager ftD2xx = null;
    public static final int readLength = 512;
    public int readcount = 0;
    public int iavailable = 0;
    byte[] readData;
    char[] readDataToText;
    String reciveData;
    static int iEnableReadFlag = 1; /*local variables*/
    int baudRate = 9600; /*baud rate*/
    byte stopBit = 1; /*1:1stop bits, 2:2 stop bits*/
    byte dataBit = 8; /*8:8bit, 7: 7bit*/
    byte parity = 0;  /* 0: none, 1: odd, 2: even, 3: mark, 4: space*/
    byte flowControl = 1; /*0:none, 1: flow control(CTS,RTS)*/
    ArrayList<CharSequence> portNumberList;

    public boolean bReadThreadGoing = false;
    public readThread read_thread;

    boolean uart_configured = false;

    private TextView accept;
    private EditText editText;
    private Button send, takephoto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        CrashHandler.getInstance().init(this);
        try {
            ftD2xx = D2xxManager.getInstance(this);
        } catch (D2xxManager.D2xxException ex) {
            ex.printStackTrace();
        }

        accept = findViewById(R.id.accept);
        editText = findViewById(R.id.edit_txt);
        send = findViewById(R.id.btn_send);
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SendMessage();
            }
        });

        SetupD2xxLibrary();

        readData = new byte[readLength];
        readDataToText = new char[readLength];

        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        filter.setPriority(500);
        this.registerReceiver(mUsbReceiver, filter);

    }

    public void SendMessage() {
        if (ftDev.isOpen() == false) {
            Log.e("j2xx", "SendMessage: device not open");
            return;
        }
        ftDev.setLatencyTimer((byte) 16);
//		ftDev.purge((byte) (D2xxManager.FT_PURGE_TX | D2xxManager.FT_PURGE_RX));
        String writeData = editText.getText().toString();
        byte[] OutData = writeData.getBytes();
        ftDev.write(OutData, writeData.length());
    }

    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    public void onResume() {
        super.onResume();
        DevCount = 0;
        createDeviceList();

        connectFunction();
        SetConfig(baudRate, dataBit, stopBit, parity, flowControl);

    }

    @Override
    public void onStop() {
        disconnectFunction();
        super.onStop();
    }

    public void createDeviceList() {
        int tempDevCount = ftD2xx.createDeviceInfoList(this);

        if (tempDevCount > 0) {
            if( DevCount != tempDevCount ) {
                DevCount = tempDevCount;
            }
        } else {
            DevCount = -1;
            currentIndex = -1;
        }
    }

    public void disconnectFunction()
    {
        DevCount = -1;
        currentIndex = -1;
        bReadThreadGoing = false;
        try {
            Thread.sleep(50);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }

        if(ftDev != null)
        {
            synchronized(ftDev)
            {
                if( true == ftDev.isOpen())
                {
                    ftDev.close();
                }
            }
        }
    }


    public void connectFunction() {
        int tmpProtNumber = openIndex + 1;

        if( currentIndex != openIndex ) {
            if(null == ftDev) {
                try {
                    ftDev = ftD2xx.openByIndex(this, openIndex);
                } catch (Exception e){
                    ftDev = null;
                }
            } else {
                synchronized(ftDev) {
                    ftDev = ftD2xx.openByIndex(this, openIndex);
                }
            }
            uart_configured = false;
        } else {
            Toast.makeText(this,"Device port " + tmpProtNumber + " is already opened",Toast.LENGTH_LONG).show();
            return;
        }

        if(ftDev == null) {
            Toast.makeText(this,"open device port("+tmpProtNumber+") NG, ftDev == null", Toast.LENGTH_LONG).show();
            return;
        }

        if (true == ftDev.isOpen()) {
            currentIndex = openIndex;
            Toast.makeText(this, "open device port(" + tmpProtNumber + ") OK", Toast.LENGTH_SHORT).show();

            if(false == bReadThreadGoing) {
                read_thread = new readThread(handler);
                read_thread.start();
                bReadThreadGoing = true;
            }
        } else {
            FT_EEPROM ft_Data = null;
            if(ftD2xx.createDeviceInfoList(this) <= 0)
                return;
            ftDev = ftD2xx.openByIndex(this, 0);
            ft_Data = ftDev.eepromRead();
            if (ft_Data == null) {
                Toast.makeText(this, "Not supported device",
                        Toast.LENGTH_SHORT).show();
            } else {
                final FT_EEPROM finalFt_Data = ft_Data;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ((TextView)findViewById(R.id.vid)).setText(Integer.toHexString(finalFt_Data.VendorId));
                        ((TextView)findViewById(R.id.pid)).setText(Integer.toHexString(finalFt_Data.ProductId));
                    }
                });

            }
            int iVid = Integer.parseInt(Integer.toHexString(ft_Data.VendorId),16);
            int iPid = Integer.parseInt(Integer.toHexString(ft_Data.ProductId),16);
            ftD2xx.setVIDPID(iVid,iPid);

            Toast.makeText(this, "open device port(" + tmpProtNumber + ") NG", Toast.LENGTH_LONG).show();
            //Toast.makeText(this, "Need to get permission!", Toast.LENGTH_SHORT).show();			
        }
    }


    private class readThread  extends Thread {
        Handler mHandler;

        readThread(Handler h){
            mHandler = h;
            this.setPriority(Thread.MIN_PRIORITY);
        }

        @Override
        public void run() {
            int i;

            while(true == bReadThreadGoing) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {

                }

                synchronized(ftDev) {
                    iavailable = ftDev.getQueueStatus();
                    if (iavailable > 0) {

                        if(iavailable > readLength){
                            iavailable = readLength;
                        }

                        ftDev.read(readData, iavailable);
                        reciveData = "";
                        for (i = 0; i < iavailable; i++) {
                            readDataToText[i] = (char) readData[i];
                            reciveData += HexUtils.bytesToHexString(readData) + " ";
//                            LogUtil.open().appendMethodB(
//                                    "{rs232:---readDataToText---" + HexUtils.bytesToHexString(readData) +
//                                            "},");
                        }
                        Message msg = mHandler.obtainMessage();
                        mHandler.sendMessage(msg);
                    }
                }
            }
        }

    }

    final Handler handler =  new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if(iavailable > 0) {
                accept.append(String.copyValueOf(readDataToText, 0, iavailable));
                accept.setText(reciveData);
                LogUtil.open().appendMethodB(
                        "{rs232:---reciveData---" + accept.getText().toString().trim() +
                                "},");

            }
        }
    };

    public void SetConfig(int baud, byte dataBits, byte stopBits, byte parity, byte flowControl) {
        if (ftDev == null){
            return;
        }
        if (ftDev.isOpen() == false) {
            Log.e("j2xx", "SetConfig: device not open");
            return;
        }

        // configure our port
        // reset to UART mode for 232 devices
        ftDev.setBitMode((byte) 0, D2xxManager.FT_BITMODE_RESET);

        ftDev.setBaudRate(baud);

        switch (dataBits) {
            case 7:
                dataBits = D2xxManager.FT_DATA_BITS_7;
                break;
            case 8:
                dataBits = D2xxManager.FT_DATA_BITS_8;
                break;
            default:
                dataBits = D2xxManager.FT_DATA_BITS_8;
                break;
        }

        switch (stopBits) {
            case 1:
                stopBits = D2xxManager.FT_STOP_BITS_1;
                break;
            case 2:
                stopBits = D2xxManager.FT_STOP_BITS_2;
                break;
            default:
                stopBits = D2xxManager.FT_STOP_BITS_1;
                break;
        }

        switch (parity) {
            case 0:
                parity = D2xxManager.FT_PARITY_NONE;
                break;
            case 1:
                parity = D2xxManager.FT_PARITY_ODD;
                break;
            case 2:
                parity = D2xxManager.FT_PARITY_EVEN;
                break;
            case 3:
                parity = D2xxManager.FT_PARITY_MARK;
                break;
            case 4:
                parity = D2xxManager.FT_PARITY_SPACE;
                break;
            default:
                parity = D2xxManager.FT_PARITY_NONE;
                break;
        }

        ftDev.setDataCharacteristics(dataBits, stopBits, parity);

        short flowCtrlSetting;
        switch (flowControl) {
            case 0:
                flowCtrlSetting = D2xxManager.FT_FLOW_NONE;
                break;
            case 1:
                flowCtrlSetting = D2xxManager.FT_FLOW_RTS_CTS;
                break;
            case 2:
                flowCtrlSetting = D2xxManager.FT_FLOW_DTR_DSR;
                break;
            case 3:
                flowCtrlSetting = D2xxManager.FT_FLOW_XON_XOFF;
                break;
            default:
                flowCtrlSetting = D2xxManager.FT_FLOW_NONE;
                break;
        }
        // TODO : flow ctrl: XOFF/XOM
        // TODO : flow ctrl: XOFF/XOM
        ftDev.setFlowControl(flowCtrlSetting, (byte) 0x0b, (byte) 0x0d);

        uart_configured = true;
        Toast.makeText(this, "Config done", Toast.LENGTH_SHORT).show();
    }

    private void SetupD2xxLibrary () {
        if(!ftD2xx.setVIDPID(0x0403, 0x6001))
            Log.i("ftd2xx-java","setVIDPID Error");
    }

    /***********USB broadcast receiver*******************************************/
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String TAG = "FragL";
            String action = intent.getAction();
            if(UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                Log.i(TAG,"DETACHED...");
                LogUtil.open().appendMethodB("{rs232:---DETACHED---},");
                disconnectFunction();

            }
        }
    };

}
