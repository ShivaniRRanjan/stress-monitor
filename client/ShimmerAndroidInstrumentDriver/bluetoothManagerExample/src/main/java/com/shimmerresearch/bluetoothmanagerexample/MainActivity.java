package com.shimmerresearch.bluetoothmanagerexample;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.shimmerresearch.android.Shimmer;
import com.shimmerresearch.android.guiUtilities.ShimmerBluetoothDialog;
import com.shimmerresearch.android.guiUtilities.ShimmerDialogConfigurations;
import com.shimmerresearch.android.manager.ShimmerBluetoothManagerAndroid;
import com.shimmerresearch.bluetooth.ShimmerBluetooth;
import com.shimmerresearch.driver.CallbackObject;
import com.shimmerresearch.driver.Configuration;
import com.shimmerresearch.driver.FormatCluster;
import com.shimmerresearch.driver.ObjectCluster;
import com.shimmerresearch.driver.ShimmerDevice;
import com.shimmerresearch.driverUtilities.ChannelDetails;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
//import org.apache.http.HttpEntity;

import org.apache.http.util.EntityUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.util.Collection;
import java.util.Date;

import static com.shimmerresearch.android.guiUtilities.ShimmerBluetoothDialog.EXTRA_DEVICE_ADDRESS;


public class MainActivity extends AppCompatActivity {
    private final String logName = "StressMonitor";


    ShimmerBluetoothManagerAndroid btManager;
    ShimmerDevice shimmerDevice;

    String shimmerBtAdd = "00:06:66:D7:C6:F4";  //Put the address of the Shimmer device you want to connect here
    private String macAdd = "";
    final static String LOG_TAG = "BluetoothManagerExample";
    TextView textView;
    private final static int PERMISSIONS_REQUEST_WRITE_STORAGE = 5;

    //Write to CSV variables
    private FileWriter fw;
    private BufferedWriter bw;
    private File file;
    boolean firstTimeWrite = true;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            btManager = new ShimmerBluetoothManagerAndroid(this, mHandler);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Couldn't create ShimmerBluetoothManagerAndroid. Error: " + e);
        }

        //Check if permission to write to external storage has been granted
        if (Build.VERSION.SDK_INT >= 23) {
            if(checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE )!= PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        PERMISSIONS_REQUEST_WRITE_STORAGE);
            }
        }

    }

    @Override
    protected void onStart() {
        //Connect the Shimmer using its Bluetooth Address
        try {
            btManager.connectShimmerThroughBTAddress(shimmerBtAdd);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error. Shimmer device not paired or Bluetooth is not enabled");
            Toast.makeText(this, "Error. Shimmer device not paired or Bluetooth is not enabled. " +
                            "Please close the app and pair or enable Bluetooth", Toast.LENGTH_LONG).show();
        }
        textView = (TextView) findViewById(R.id.textView);
        super.onStart();
    }

    @Override
    protected void onStop() {
        //Disconnect the Shimmer device when app is stopped
        if(shimmerDevice != null) {
            if(shimmerDevice.isSDLogging()) {
                shimmerDevice.stopSDLogging();
                Log.d(LOG_TAG, "Stopped Shimmer Logging");
            }
            else if(shimmerDevice.isStreaming()) {
                shimmerDevice.stopStreaming();

                Log.d(LOG_TAG, "Stopped Shimmer Streaming");


            }
            else {
                shimmerDevice.stopStreamingAndLogging();
                Log.d(LOG_TAG, "Stopped Shimmer Streaming and Logging");
            }
        }
        btManager.disconnectAllDevices();
        Log.i(LOG_TAG, "Shimmer DISCONNECTED");
        super.onStop();
    }

    /**
     * Messages from the Shimmer device including sensor data are received here
     */
    Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case ShimmerBluetooth.MSG_IDENTIFIER_DATA_PACKET:
                    if ((msg.obj instanceof ObjectCluster)) {

//                        ObjectCluster objectCluster = (ObjectCluster) msg.obj;
//
//                        //Retrieve all possible formats for the current sensor device:
//                        Collection<FormatCluster> allFormats = objectCluster.getCollectionOfFormatClusters(Configuration.Shimmer3.ObjectClusterSensorName.TIMESTAMP);
//                        FormatCluster timeStampCluster = ((FormatCluster)ObjectCluster.returnFormatCluster(allFormats,"CAL"));
//                        double timeStampData = timeStampCluster.mData;
//                        Log.i(LOG_TAG, "Time Stamp: " + timeStampData);
//                        allFormats = objectCluster.getCollectionOfFormatClusters(Configuration.Shimmer3.ObjectClusterSensorName.ACCEL_LN_X);
//                        FormatCluster accelXCluster = ((FormatCluster)ObjectCluster.returnFormatCluster(allFormats,"CAL"));
//                        if (accelXCluster!=null) {
//                            double accelXData = accelXCluster.mData;
//                            Log.i(LOG_TAG, "Accel LN X: " + accelXData);
//                        }


                        ObjectCluster objc = (ObjectCluster) msg.obj;

                        /**
                         * ---------- Printing a channel to Logcat ----------
                         */
                        //Method 1 - retrieve data from the ObjectCluster using get method
                        double data = objc.getFormatClusterValue(Configuration.Shimmer3.ObjectClusterSensorName.ACCEL_WR_X, ChannelDetails.CHANNEL_TYPE.CAL.toString());
                        Log.i(LOG_TAG, Configuration.Shimmer3.ObjectClusterSensorName.ACCEL_WR_X + " data: " + data);

                        //Method 2a - retrieve data from the ObjectCluster by manually parsing the arrays
                        int index = -1;
                        for(int i=0; i<objc.sensorDataArray.mSensorNames.length; i++) {
                            if(objc.sensorDataArray.mSensorNames[i] != null) {
                                if (objc.sensorDataArray.mSensorNames[i].equals(Configuration.Shimmer3.ObjectClusterSensorName.ACCEL_WR_X)) {
                                    index = i;
                                }
                            }
                        }
                        if(index != -1) {
                            //Index was found
                            data = objc.sensorDataArray.mCalData[index];
                            Log.w(LOG_TAG, Configuration.Shimmer3.ObjectClusterSensorName.ACCEL_WR_X + " data: " + data);
                        }

                        //Method 2b - retrieve data from the ObjectCluster by getting the index, then accessing the arrays
                        index = objc.getIndexForChannelName(Configuration.Shimmer3.ObjectClusterSensorName.ACCEL_WR_X);
                        if(index != -1) {
                            data = objc.sensorDataArray.mCalData[index];
                            Log.e(LOG_TAG, Configuration.Shimmer3.ObjectClusterSensorName.ACCEL_WR_X + " data: " + data);
                        }

                        /**
                         * ---------- Writing all channels of CAL data to CSV file ----------
                         */
                        if(firstTimeWrite) {
                            //Write headers on first-time
                            for(String channelName : objc.sensorDataArray.mSensorNames) {
                                try {
                                    bw.write(channelName + ",");
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                            try {
                                bw.write("\n");
                            } catch(IOException e2) {
                                e2.printStackTrace();
                            }
                            firstTimeWrite = false;
                        }
                        for(double calData : objc.sensorDataArray.mCalData) {
                            String dataString = String.valueOf(calData);
                            try {
                                bw.write(dataString + ",");
                            } catch(IOException e3) {
                                e3.printStackTrace();
                            }
                        }
                        try {
                            bw.write("\n");
                        } catch(IOException e2) {
                            e2.printStackTrace();
                        }



                    }
                    break;
                case ShimmerBluetooth.MSG_IDENTIFIER_STATE_CHANGE:
                    ShimmerBluetooth.BT_STATE state = null;
                    String macAddress = "";

                    if (msg.obj instanceof ObjectCluster) {
                        state = ((ObjectCluster) msg.obj).mState;
                        macAddress = ((ObjectCluster) msg.obj).getMacAddress();
                    } else if (msg.obj instanceof CallbackObject) {
                        state = ((CallbackObject) msg.obj).mState;
                        macAddress = ((CallbackObject) msg.obj).mBluetoothAddress;
                    }

                    Log.d(LOG_TAG, "Shimmer state changed! Shimmer = " + macAddress + ", new state = " + state);

                    switch (state) {
                        case CONNECTED:
                            Log.i(LOG_TAG, "Shimmer [" + macAddress + "] is now CONNECTED");
                            shimmerDevice = btManager.getShimmerDeviceBtConnectedFromMac(shimmerBtAdd);
                            if(shimmerDevice != null) { Log.i(LOG_TAG, "Got the ShimmerDevice!"); }
                            else { Log.i(LOG_TAG, "ShimmerDevice returned is NULL!"); }
                            break;
                        case CONNECTING:
                            Log.i(LOG_TAG, "Shimmer [" + macAddress + "] is CONNECTING");
                            break;
                        case STREAMING:
                            Log.i(LOG_TAG, "Shimmer [" + macAddress + "] is now STREAMING");
                            break;
                        case STREAMING_AND_SDLOGGING:
                            Log.i(LOG_TAG, "Shimmer [" + macAddress + "] is now STREAMING AND LOGGING");
                            break;
                        case SDLOGGING:
                            Log.i(LOG_TAG, "Shimmer [" + macAddress + "] is now SDLOGGING");
                            break;
                        case DISCONNECTED:
                            Log.i(LOG_TAG, "Shimmer [" + macAddress + "] has been DISCONNECTED");
                            break;
                    }
                    break;
            }

            super.handleMessage(msg);
        }
    };

    public void stopStreaming(View v){
//        shimmerDevice.stopStreaming();

        if(btManager.getShimmer(macAdd) != null) {
            try {   //Stop CSV writing
                bw.flush();
                bw.close();
                fw.close();
                firstTimeWrite = true;

            } catch (IOException e) {
                e.printStackTrace();
            }

            btManager.stopStreaming(macAdd);
//            new UploadFile().execute(new File(filePath));
//            Log.d(LOG_TAG,filePath);

        } else {
            Toast.makeText(this, "Can't stop streaming\nShimmer device is not connected", Toast.LENGTH_SHORT).show();
        }
    }
String filePath="";
    public void startStreaming(View v){
        //shimmerDevice.startStreaming();
        Shimmer shimmer = (Shimmer) btManager.getShimmer(macAdd);
        if(shimmer != null) {   //this is null if Shimmer device is not connected
            //Setup CSV writing
            //Context context = null;
            String StressMonitoring="StressMonitor";
            File baseDir = new File(Environment.getExternalStorageDirectory() +
                    File.separator + StressMonitoring);
            if (!baseDir.exists()) {
                baseDir.mkdir();
            }
//
           // String baseDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();

           // String fileName = "ObjectClusterExample Data " + DateFormat.getDateTimeInstance().format(new Date()) + ".csv";
            String fileName = "StressData"+".csv";
            filePath = baseDir + File.separator + fileName;
            file = new File(filePath);
            try {
                if (!file.exists()) {
                    file.createNewFile();
                }
                fw = new FileWriter(file.getAbsoluteFile());
                bw = new BufferedWriter(fw);
            } catch (IOException e) {
                e.printStackTrace();
            }
            //Disable PC timestamps for better performance. Disabling this takes the timestamps on every full packet received instead of on every byte received.
            shimmer.enablePCTimeStamps(false);
            //Enable the arrays data structure. Note that enabling this will disable the Multimap/FormatCluster data structure
            shimmer.enableArraysDataStructure(true);
            btManager.startStreaming(macAdd);
        } else {
            Toast.makeText(this, "Can't start streaming\nShimmer device is not connected", Toast.LENGTH_SHORT).show();
        }




        }
   String url="http://ec2-18-216-89-105.us-east-2.compute.amazonaws.com:8080/";
   // String url1="169.234.104.33:8080/";
    DownloadManager downloadManager;

    public void processing(View view) throws IOException {
      new UploadFile().execute(new File(Environment.getExternalStorageDirectory() +File.separator + "StressMonitor"+ File.separator+"StressData.csv"));

}


    public class UploadFile extends AsyncTask<File, Void, Void>

    {


        @Override
        protected Void doInBackground(File... file) {
            String[] hr;
            String[] gsr;
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost(url);
            HttpGet request = new HttpGet(url);
            try {
                FileBody bin = new FileBody(file[0]);
                MultipartEntity reqEntity = new MultipartEntity();
                reqEntity.addPart("file", bin);
                httppost.setEntity(reqEntity);
                System.out.println("Requesting : " + httppost.getRequestLine());
                ResponseHandler<String> responseHandler = new BasicResponseHandler();
                HttpResponse response=httpclient.execute(httppost);
//                InputStream is = response.getEntity().getContent();
//                FileOutputStream fos = new FileOutputStream(new File(Environment.getExternalStorageDirectory() + File.separator + "StressMonitor", "log.csv"));
                 //String responseBody = httpclient.execute(httppost, responseHandler);
                //System.out.println("responseBody : " + responseBody);
                //download
                HttpEntity entity = response.getEntity();

                int responseCode = response.getStatusLine().getStatusCode();
                InputStream is = entity.getContent();
                FileOutputStream fos = new FileOutputStream(new File(Environment.getExternalStorageDirectory() + File.separator + "StressMonitor", "result.csv"));
                int inByte;
                while ((inByte = is.read()) != -1) {
                    fos.write(inByte);
                }

                is.close();
                fos.close();
//
            }
            catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                httpclient.getConnectionManager().shutdown();
            }
            return null;
        }
    }

    /**
     * Called when the configurations button is clicked
     * @param v
     */
    public void openConfigMenu(View v){
        if(shimmerDevice != null) {
            if(!shimmerDevice.isStreaming() && !shimmerDevice.isSDLogging()) {
                ShimmerDialogConfigurations.buildShimmerConfigOptions(shimmerDevice, MainActivity.this, btManager);
            }
            else {
                Log.e(LOG_TAG, "Cannot open menu! Shimmer device is STREAMING AND/OR LOGGING");
                Toast.makeText(MainActivity.this, "Cannot open menu! Shimmer device is STREAMING AND/OR LOGGING", Toast.LENGTH_SHORT).show();
            }
        }
        else {
            Log.e(LOG_TAG, "Cannot open menu! Shimmer device is not connected");
            Toast.makeText(MainActivity.this, "Cannot open menu! Shimmer device is not connected", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Called when the menu button is clicked
     * @param v
     * @throws IOException
     */
    public void openMenu(View v) throws IOException {

        if(shimmerDevice != null) {
            if(!shimmerDevice.isStreaming() && !shimmerDevice.isSDLogging()) {
                //ShimmerDialogConfigurations.buildShimmerSensorEnableDetails(shimmerDevice, MainActivity.this);
                ShimmerDialogConfigurations.buildShimmerSensorEnableDetails(shimmerDevice, MainActivity.this, btManager);
            }
            else {
                Log.e(LOG_TAG, "Cannot open menu! Shimmer device is STREAMING AND/OR LOGGING");
                Toast.makeText(MainActivity.this, "Cannot open menu! Shimmer device is STREAMING AND/OR LOGGING", Toast.LENGTH_SHORT).show();
            }
        }
        else {
            Log.e(LOG_TAG, "Cannot open menu! Shimmer device is not connected");
            Toast.makeText(MainActivity.this, "Cannot open menu! Shimmer device is not connected", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Called when the connect button is clicked
     * @param v
     */
    public void connectDevice(View v) {
        Intent intent = new Intent(getApplicationContext(), ShimmerBluetoothDialog.class);
        startActivityForResult(intent, ShimmerBluetoothDialog.REQUEST_CONNECT_SHIMMER);
    }

    public void startSDLogging(View v) {
        ((ShimmerBluetooth)shimmerDevice).writeConfigTime(System.currentTimeMillis());
        shimmerDevice.startSDLogging();
    }

    public void stopSDLogging(View v) {
        shimmerDevice.stopSDLogging();
    }


    /**
     * Get the result from the paired devices dialog
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == 2) {
            if (resultCode == Activity.RESULT_OK) {
                btManager.disconnectAllDevices();   //Disconnect all devices first
                //Get the Bluetooth mac address of the selected device:
                macAdd = data.getStringExtra(EXTRA_DEVICE_ADDRESS);
                btManager.connectShimmerThroughBTAddress(macAdd);   //Connect to the selected device
                shimmerBtAdd = macAdd;
            }

        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * Permission request callback
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == PERMISSIONS_REQUEST_WRITE_STORAGE) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(this, "Error! Permission not granted. App will now close", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

}
