package com.cooper.wheellog;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.os.Handler;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final int DEVICE_SCAN_REQUEST = 10;

    Button buttonScan;
    Button buttonConnect;
    Button buttonPebbleService;
    Button buttonDataService;

    TextView textViewSpeed;
    TextView textViewTemperature;
    TextView textViewCurrent;
    TextView textViewVoltage;
    TextView textViewBattery;
    TextView textViewFanStatus;
    TextView textViewMaxSpeed;
    TextView textViewCurrentDistance;
    TextView textViewType;
    TextView textViewName;
    TextView textViewVersion;
    TextView textViewSerial;
    TextView textViewTotalDistance;
    TextView textViewRideTime;

    Handler mHandler = new Handler();
    private BluetoothLeService mBluetoothLeService;
    String mDeviceAddress;
    private int mConnectionState = BluetoothLeService.STATE_DISCONNECTED;
    private boolean doubleBackToExitPressedOnce = false;
    private boolean launchedFromPebble = false;
    private final String TAG = "MainActivity";
    private static final int PERMISSION_REQUEST_CODE = 10;

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            if (mBluetoothLeService.isConnected()) {
                mConnectionState = BluetoothLeService.STATE_CONNECTED;
                buttonConnect.setText(R.string.disconnect);
                buttonConnect.setEnabled(true);
                buttonPebbleService.setEnabled(true);
                buttonDataService.setEnabled(true);
            }

            if (launchedFromPebble && !"".equals(mDeviceAddress))
                mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    private final BroadcastReceiver mBluetoothUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (Constants.ACTION_BLUETOOTH_CONNECTED.equals(action) && mConnectionState != BluetoothLeService.STATE_CONNECTED) {
                Log.d(TAG, "Bluetooth connected");
                mConnectionState = BluetoothLeService.STATE_CONNECTED;
                buttonConnect.setText(R.string.disconnect);
                SettingsManager.setLastAddr(getApplicationContext(), mDeviceAddress);
                buttonPebbleService.setEnabled(true);
                buttonDataService.setEnabled(true);
                buttonConnect.setEnabled(true);

                byte[] data = new byte[20];
                data[0] = (byte) -86;
                data[1] = (byte) 85;
                data[16] = (byte) -101;
                data[17] = (byte) 20;
                data[18] = (byte) 90;
                data[19] = (byte) 90;
                mBluetoothLeService.writeBluetoothGattCharacteristic(data);
            } else if (Constants.ACTION_BLUETOOTH_DISCONNECTED.equals(action)) {
                mConnectionState = BluetoothLeService.STATE_DISCONNECTED;
                Toast.makeText(MainActivity.this, "DISCONNECTED", Toast.LENGTH_SHORT).show();
                buttonConnect.setText(R.string.connect);
                buttonConnect.setEnabled(true);
            } else if (Constants.ACTION_WHEEL_DATA_AVAILABLE.equals(action)) {
                textViewSpeed.setText(String.format("%s KPH", Wheel.getInstance().getSpeedDouble()));
                textViewVoltage.setText(String.format("%sV", Wheel.getInstance().getVoltageDouble()));
                textViewTemperature.setText(String.format("%d°C", Wheel.getInstance().getTemperature()));
                textViewCurrent.setText(String.format("%sW", Wheel.getInstance().getCurrentDouble()));
                textViewBattery.setText(String.format("%d%%", Wheel.getInstance().getBatteryLevel()));
                textViewFanStatus.setText(Wheel.getInstance().getFanStatus() == 0 ? "Off" : "On");
                textViewMaxSpeed.setText(String.format("%s KPH", Wheel.getInstance().getMaxSpeedDouble()));
                textViewCurrentDistance.setText(String.format("%s KM", Wheel.getInstance().getCurrentDistanceDouble()));
                textViewTotalDistance.setText(String.format("%s KM", Wheel.getInstance().getTotalDistanceDouble()));
                textViewVersion.setText(String.format("%d", Wheel.getInstance().getVersion()));
                textViewName.setText(Wheel.getInstance().getName());
                textViewType.setText(Wheel.getInstance().getType());
                textViewSerial.setText(Wheel.getInstance().getSerial());
                textViewRideTime.setText(Wheel.getInstance().getCurrentTimeString());
            } else if (Constants.ACTION_REQUEST_SERIAL_DATA.equals(action)) {
                byte[] data = new byte[20];
                data[0] = (byte) -86;
                data[1] = (byte) 85;
                data[16] = (byte) 99;
                data[17] = (byte) 20;
                data[18] = (byte) 90;
                data[19] = (byte) 90;
                mBluetoothLeService.writeBluetoothGattCharacteristic(data);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = getIntent();
        launchedFromPebble = intent.getBooleanExtra(Constants.LAUNCHED_FROM_PEBBLE, false);

        Intent gattServiceIntent = new Intent(getApplicationContext(), BluetoothLeService.class);
        gattServiceIntent.putExtra(com.cooper.wheellog.Constants.LAUNCHED_FROM_PEBBLE, launchedFromPebble);
        startService(gattServiceIntent);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        mDeviceAddress = SettingsManager.getLastAddr(getApplicationContext());

        buttonScan = (Button) findViewById(R.id.buttonBluetoothScan);
        buttonConnect = (Button) findViewById(R.id.buttonBluetoothConnect);
        buttonPebbleService = (Button) findViewById(R.id.buttonPebbleService);
        buttonDataService = (Button) findViewById(R.id.buttonDataLoggingService);

        textViewSpeed = (TextView) findViewById(R.id.tvSpeed);
        textViewCurrent = (TextView) findViewById(R.id.tvCurrent);
        textViewTemperature = (TextView) findViewById(R.id.tvTemperature);
        textViewVoltage = (TextView) findViewById(R.id.tvVoltage);
        textViewBattery = (TextView) findViewById(R.id.tvBattery);
        textViewFanStatus = (TextView) findViewById(R.id.tvFanStatus);
        textViewMaxSpeed = (TextView) findViewById(R.id.tvMaxSpeed);
        textViewCurrentDistance = (TextView) findViewById(R.id.tvCurrentDistance);
        textViewTotalDistance = (TextView) findViewById(R.id.tvTotalDistance);
        textViewType = (TextView) findViewById(R.id.tvType);
        textViewName = (TextView) findViewById(R.id.tvName);
        textViewVersion = (TextView) findViewById(R.id.tvVersion);
        textViewSerial = (TextView) findViewById(R.id.tvSerial);
        textViewRideTime = (TextView) findViewById(R.id.tvRideTime);

        buttonScan.setOnClickListener(clickListener);
        buttonConnect.setOnClickListener(clickListener);
        buttonPebbleService.setOnClickListener(clickListener);
        buttonDataService.setOnClickListener(clickListener);

        if (PebbleConnectivity.isInstanceCreated()) {
            buttonPebbleService.setEnabled(true);
            buttonPebbleService.setText(R.string.stop_pebble_service);
        } else
            buttonPebbleService.setText(R.string.start_pebble_service);

        if (DataLogger.isInstanceCreated()) {
            buttonDataService.setEnabled(true);
            buttonDataService.setText(R.string.stop_data_service);
        } else
            buttonDataService.setText(R.string.start_data_service);

        if (!mDeviceAddress.isEmpty())
            buttonConnect.setEnabled(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        registerReceiver(mBluetoothUpdateReceiver, BluetoothLeService.makeBluetoothUpdateIntentFilter());
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(mBluetoothUpdateReceiver);
        mHandler.removeCallbacksAndMessages(null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            close();
            super.onBackPressed();
            return;
        }

        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, "Please BACK again to exit", Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                doubleBackToExitPressedOnce=false;
            }
        }, 2000);
    }

    private void close() {
        if (PebbleConnectivity.isInstanceCreated()) {
            Intent PebbleServiceIntent = new Intent(getApplicationContext(), PebbleConnectivity.class);
            stopService(PebbleServiceIntent);
        }

        if (DataLogger.isInstanceCreated()) {
            Intent DataServiceIntent = new Intent(getApplicationContext(), DataLogger.class);
            stopService(DataServiceIntent);
        }

        Intent bluetoothServiceIntent = new Intent(getApplicationContext(), BluetoothLeService.class);
        stopService(bluetoothServiceIntent);
    }

    private View.OnClickListener clickListener = new View.OnClickListener()
    {

        @Override
        public void onClick(View view)
        {
            switch (view.getId())
            {
                case R.id.buttonBluetoothScan:
                    Intent scanActivityIntent = new Intent(MainActivity.this, ScanActivity.class);
                    startActivityForResult(scanActivityIntent, DEVICE_SCAN_REQUEST);
                    break;
                case R.id.buttonBluetoothConnect:
                    buttonConnect.setText(R.string.connecting);
                    buttonConnect.setEnabled(false);
                    if (mConnectionState == BluetoothLeService.STATE_DISCONNECTED)
                    {
                        Boolean result = mBluetoothLeService.connect(mDeviceAddress);
                        if (!result)
                        {
                            buttonConnect.setEnabled(true);
                            buttonConnect.setText(R.string.connect);
                            Toast.makeText(MainActivity.this, "Connection Failed", Toast.LENGTH_SHORT).show();
                        }
                    }
                    else
                        mBluetoothLeService.disconnect();
                    break;
                case R.id.buttonPebbleService:
                    Intent pebbleServiceIntent = new Intent(getApplicationContext(), PebbleConnectivity.class);

                    if (PebbleConnectivity.isInstanceCreated())
                    {
                        buttonPebbleService.setText(R.string.start_pebble_service);
                        stopService(pebbleServiceIntent);
                    }
                    else
                    {
                        buttonPebbleService.setText(R.string.stop_pebble_service);
                        startService(pebbleServiceIntent);
                    }
                    break;
                case R.id.buttonDataLoggingService:

                    if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !checkExternalFilePermission())
                        requestExternalFilePermission();
                    else
                    {
                        Intent dataLoggerServiceIntent = new Intent(getApplicationContext(), DataLogger.class);
                        if (DataLogger.isInstanceCreated())
                        {
                            buttonDataService.setText(R.string.start_data_service);
                            stopService(dataLoggerServiceIntent);
                        }
                        else
                        {
                            buttonDataService.setText(R.string.stop_data_service);
                            startService(dataLoggerServiceIntent);
                        }
                        break;
                    }
                    break;
            }
        }
    };

    private boolean checkExternalFilePermission(){
        int result = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (result == PackageManager.PERMISSION_GRANTED){
            return true;
        } else {
            return false;
        }
    }

    private void requestExternalFilePermission(){
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)){
            Toast.makeText(this, "External write permission is required to write logs. Please allow in App Settings for additional functionality.", Toast.LENGTH_LONG).show();
        } else {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Intent dataLoggingIntent = new Intent(MainActivity.this, DataLogger.class);
                    startService(dataLoggingIntent);
                } else {
                    Toast.makeText(this, "External write permission is required to write logs. Please allow in App Settings for additional functionality.", Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == DEVICE_SCAN_REQUEST) {
            if (resultCode == RESULT_OK) {
                mDeviceAddress = data.getStringExtra("MAC");
                buttonConnect.setEnabled(true);
            }
        }
    }
}