package a2dp.connect2;

import java.util.Set;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.IBluetooth;
import android.bluetooth.IBluetoothA2dp;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import static a2dp.connect2.Bt_iadl.c1;
import static a2dp.connect2.Bt_iadl.filter_1_string;
import static a2dp.connect2.Bt_iadl.ibta2;
import static a2dp.connect2.Bt_iadl.mIsBound;


public class Connector extends Service {

    private static Context application;
    private static String DeviceToConnect;

    @Override
    public void onDestroy() {
        //this.unregisterReceiver(receiver);

        super.onDestroy();
    }

    @Override
    protected void finalize() throws Throwable {


        super.finalize();
    }

    static final int ENABLE_BLUETOOTH = 1;
    private String PREFS = "bluetoothlauncher";
    private static String LOG_TAG = "A2DP_Connect";
    private BluetoothDevice device = null;
    private String dname;
    private String bt_mac;
    boolean serviceRegistered = false;
    boolean receiverRegistered = false;

    int w_id;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Bundle extras = intent.getExtras();

        application = getApplicationContext();

        if (extras != null) {
            w_id = extras.getInt("ID", 0);

            Log.i(LOG_TAG, "connecting " + w_id);
        } else {
            Toast.makeText(application, "Oops", Toast.LENGTH_LONG).show();
            done();
        }

        SharedPreferences preferences = getSharedPreferences(PREFS, 0);
        bt_mac = preferences.getString(String.valueOf(w_id), "");
        dname = preferences.getString(w_id + "_name", "oops");
        DeviceToConnect = bt_mac;
        Log.i(LOG_TAG, "Device MAC = " + bt_mac);

        if (bt_mac != null)
            if (bt_mac.length() == 17) {

                BluetoothAdapter bta = BluetoothAdapter.getDefaultAdapter();

                if (!bta.isEnabled()) {
                    Intent btIntent = new Intent(
                            BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    btIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    application.startActivity(btIntent);
                    Log.i(LOG_TAG, "Bluetooth was not enabled, starting...");
                    return START_REDELIVER_INTENT;
                }

                BluetoothAdapter mBTA = BluetoothAdapter.getDefaultAdapter();
                if (mBTA == null || !mBTA.isEnabled()) {
                    Log.i(LOG_TAG, "Bluetooth issue");
                    return START_REDELIVER_INTENT;
                }

                Set<BluetoothDevice> pairedDevices = bta.getBondedDevices();
                for (BluetoothDevice dev : pairedDevices) {
                    if (dev.getAddress().equalsIgnoreCase(bt_mac))
                        device = dev;
                }
                if (device == null) {
                    Log.i(LOG_TAG, "Device was NULL");
                    return START_REDELIVER_INTENT;
                }

                getIBluetoothA2dp(application);

                if (!receiverRegistered) {
                    String filter_1_string = "a2dp.connect2.Connector.INTERFACE";
                    IntentFilter filter1 = new IntentFilter(filter_1_string);
                    this.registerReceiver(receiver, filter1);
                    receiverRegistered = true;
                }

                sendIntent();
                //connectBluetoothA2dp(bt_mac);

            } else {
                Toast.makeText(application,
                        getString(R.string.InvalidDevice) + " " + bt_mac,
                        Toast.LENGTH_LONG).show();
                Log.i(LOG_TAG, "Invalid device = " + bt_mac);
                done();
            }

        else {
            Log.e(LOG_TAG, "Device to connect was NULL");
            Toast.makeText(application, getString(R.string.NullDevice),
                    Toast.LENGTH_LONG).show();
            done();
        }
        return START_NOT_STICKY;
        // super.onStart(intent, startId);
    }

    private static void sendIntent() {
        Intent intent = new Intent();
        intent.setAction(filter_1_string);
        application.sendBroadcast(intent);
    };

    private final BroadcastReceiver receiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context arg0, Intent arg1) {
            IBluetoothA2dp ibta = ibta2;

            Log.i(LOG_TAG,"Received broadcast ");

            try {
                if (ibta != null && ibta.getConnectionState(device) == 0) {
                    Toast.makeText(application,
                            getString(R.string.Connecting) + "  " + dname,
                            Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(application,
                            getString(R.string.Disconnecting) + "  " + dname,
                            Toast.LENGTH_LONG).show();
                }
            } catch (RemoteException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            connectBluetoothA2dp(bt_mac);
        }

    };

    /**
     * @see android.app.Activity#onCreate(Bundle)
     */

    public void onCreate() {
        // super.onCreate();
        application = getApplication();
        String filter_1_string = "a2dp.connect2.Connector.INTERFACE";
        IntentFilter filter1 = new IntentFilter(filter_1_string);
        this.registerReceiver(receiver, filter1);
        receiverRegistered = true;

        getIBluetoothA2dp(application);
        serviceRegistered = true;
    }

    private void connectBluetoothA2dp(String device) {
        Log.i(LOG_TAG, "Device = " + device);
        new ConnectBt().execute(device);
    }


    public static ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            mIsBound = true;
            ibta2 = IBluetoothA2dp.Stub.asInterface(service);
            BluetoothAdapter mBTA = BluetoothAdapter.getDefaultAdapter();

            Set<BluetoothDevice> pairedDevices = mBTA.getBondedDevices();
            BluetoothDevice device = null;
            for (BluetoothDevice dev : pairedDevices) {
                if (dev.getAddress().equalsIgnoreCase(DeviceToConnect))
                    device = dev;
            }
            if (device != null)
                try {
                    Log.i(LOG_TAG, "Service connecting " + device);
                    Intent intent = new Intent();
                    intent.setAction(filter_1_string);
                    application.sendBroadcast(intent);

                    //ibta2.connect(device);
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(LOG_TAG, "Error connecting Bluetooth device " + e.getLocalizedMessage());
                }

            /*
             * mBTA.cancelDiscovery(); mBTA.startDiscovery();
             */
            IBluetoothA2dp ibta = ibta2;
            try {
                Log.d(LOG_TAG, "Connecting/disconnecting: " + ibta.getPriority(device));
                if (ibta != null && ibta.getConnectionState(device) == 0)
                    ibta.connect(device);
                else
                    ibta.disconnect(device);

            } catch (Exception e) {
                Log.e(LOG_TAG, "Error " + e.getMessage());
            }

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mIsBound = false;
            doUnbind();
        }
    };

    static void doUnbind() {
        if (mIsBound) {
            try {
                application.unbindService(mConnection);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void getIBluetoothA2dp(Context context) {

        Intent i = new Intent(IBluetoothA2dp.class.getName());

        String filter;
        filter = getPackageManager().resolveService(i, PackageManager.GET_RESOLVED_FILTER).serviceInfo.packageName;
        i.setPackage(filter);

        if (context.bindService(i, mConnection, Context.BIND_AUTO_CREATE)) {
            Log.i(LOG_TAG, "mConnection service bound");
            //Toast.makeText(context, "started service connection", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, "Bluetooth start service connection failed", Toast.LENGTH_SHORT).show();
            Log.e(LOG_TAG, "Could not bind to Bluetooth A2DP Service");
        }

    }

    private class ConnectBt extends AsyncTask<String, Void, Boolean> {

        /*
         * (non-Javadoc)
         *
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(Boolean result) {

            super.onPostExecute(result);
            done();
        }

        BluetoothAdapter bta = BluetoothAdapter.getDefaultAdapter();

        protected void onPreExecute() {
            Log.i(LOG_TAG, "Running background task with ");
        }

        @Override
        protected Boolean doInBackground(String... arg0) {

            BluetoothAdapter mBTA = BluetoothAdapter.getDefaultAdapter();
            if (mBTA == null || !mBTA.isEnabled())
                return false;

            Set<BluetoothDevice> pairedDevices = bta.getBondedDevices();
            BluetoothDevice device = null;
            for (BluetoothDevice dev : pairedDevices) {
                if (dev.getAddress().equalsIgnoreCase(arg0[0]))
                    device = dev;
            }
            if (device == null)
                return false;
            /*
             * mBTA.cancelDiscovery(); mBTA.startDiscovery();
             */
            IBluetoothA2dp ibta = ibta2;
            try {
                Log.d(LOG_TAG, "Here: " + ibta.getPriority(device));
                if (ibta != null && ibta.getConnectionState(device) == 0)
                    ibta.connect(device);
                else
                    ibta.disconnect(device);

            } catch (Exception e) {
                Log.e(LOG_TAG, "Error " + e.getMessage());
            }

            return true;
        }

    }

    private void done() {
        if (receiverRegistered) {
            try {
                this.unregisterReceiver(receiver);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (serviceRegistered) {
            try {
                //doUnbindService(application);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        this.stopSelf();

    }

}
