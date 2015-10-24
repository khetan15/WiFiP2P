package com.example.ramesh.p2pfileshare;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import java.io.File;

import edu.pdx.cs410.wifi.direct.file.transfer.R;

public class ServerActivity extends Activity {

    public final int fileRequestID = 55;
    public final int port = 7950;


    private WifiP2pManager wifiManager;
    private WifiP2pManager.Channel wifichannel;
    private BroadcastReceiver wifiServerReceiver;

    private IntentFilter wifiServerReceiverIntentFilter;

    private String path;
    private File downloadTarget;

    private Intent serverServiceIntent;

    private boolean serverThreadActive;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);

        //Block auto opening keyboard
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        wifiManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        wifichannel = wifiManager.initialize(this, getMainLooper(), null);
        wifiServerReceiver = new WiFiServerBroadcastReceiver(wifiManager, wifichannel, this);

        wifiServerReceiverIntentFilter = new IntentFilter();;
        wifiServerReceiverIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        wifiServerReceiverIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        wifiServerReceiverIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        wifiServerReceiverIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        //set status to stopped
        TextView serverServiceStatus = (TextView) findViewById(R.id.server_status_text);
        serverServiceStatus.setText(R.string.server_stopped);

        path = "/";
        downloadTarget = new File(path);

        serverServiceIntent = null;
        serverThreadActive = false;

        setServerFileTransferStatus("No File being transfered");

        registerReceiver(wifiServerReceiver, wifiServerReceiverIntentFilter);

        serverServiceIntent = new Intent(this, ServerService.class);
        serverServiceIntent.putExtra("saveLocation", downloadTarget);
        serverServiceIntent.putExtra("port", new Integer(port));
        serverServiceIntent.putExtra("serverResult", new ResultReceiver(null) {
            @Override
            protected void onReceiveResult(int resultCode, final Bundle resultData) {

                if(resultCode == port )
                {
                    if (resultData == null) {
                        //Server service has shut down. Download may or may not have completed properly.
                        serverThreadActive = false;


                        final TextView server_status_text = (TextView) findViewById(R.id.server_status_text);
                        server_status_text.post(new Runnable() {
                            public void run() {
                                server_status_text.setText(R.string.server_stopped);
                            }
                        });


                    }
                    else
                    {
                        final TextView server_file_status_text = (TextView) findViewById(R.id.server_file_transfer_status);

                        server_file_status_text.post(new Runnable() {
                            public void run() {
                                server_file_status_text.setText((String)resultData.get("message"));
                            }
                        });
                    }
                }

            }
        });

        serverThreadActive = true;
        startService(serverServiceIntent);

        //Set status to running
        serverServiceStatus = (TextView) findViewById(R.id.server_status_text);
        serverServiceStatus.setText(R.string.server_running);

        wifiManager.discoverPeers(wifichannel, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                Log.v("WIFITAG", "Successfully initiated discover peers");
                // Code for when the discovery initiation is successful goes here.
                // No services have actually been discovered yet, so this method
                // can often be left blank.  Code for peer discovery goes in the
                // onReceive method, detailed below.
            }

            @Override
            public void onFailure(int reasonCode) {
                Log.v("WIFITAG", "Discover peers initiation failed\n"+reasonCode);
                // Code for when the discovery initiation fails goes here.
                // Alert the user that something went wrong.
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_server, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void startFileBrowseActivity(View view) {

        Intent clientStartIntent = new Intent(this, FileBrowser.class);
        startActivityForResult(clientStartIntent, fileRequestID);
        //Path returned to onActivityResult

    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode == Activity.RESULT_OK && requestCode == fileRequestID) {
            //Fetch result
            File targetDir = (File) data.getExtras().get("file");

            if(targetDir.isDirectory())
            {
                if(targetDir.canWrite())
                {
                    downloadTarget = targetDir;
                    TextView filePath = (TextView) findViewById(R.id.server_file_path);
                    filePath.setText(targetDir.getPath());
                    setServerFileTransferStatus("Download directory set to " + targetDir.getName());

                }
                else
                {
                    setServerFileTransferStatus("You do not have permission to write to " + targetDir.getName());
                }

            }
            else
            {
                setServerFileTransferStatus("The selected file is not a directory. Please select a valid download directory.");
            }

        }
    }


    public void stopServer(View view) {


        //stop download thread
        if(serverServiceIntent != null)
        {
            stopService(serverServiceIntent);

        }

    }



    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //stopServer(null);
        //unregisterReceiver(wifiServerReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        stopServer(null);

        //stopService(serverServiceIntent);

        //Unregister broadcast receiver
        try {
            unregisterReceiver(wifiServerReceiver);
        } catch (IllegalArgumentException e) {
            // This will happen if the server was never running and the stop
            // button was pressed.
            // Do nothing in this case.
        }
    }



    public void setServerWifiStatus(String message)
    {
        TextView server_wifi_status_text = (TextView) findViewById(R.id.server_wifi_status_text);
        server_wifi_status_text.setText(message);
    }

    public void setServerStatus(String message)
    {
        TextView server_status_text = (TextView) findViewById(R.id.server_status_text_2);
        server_status_text.setText(message);
    }


    public void setServerFileTransferStatus(String message)
    {
        TextView server_status_text = (TextView) findViewById(R.id.server_file_transfer_status);
        server_status_text.setText(message);
    }


}
