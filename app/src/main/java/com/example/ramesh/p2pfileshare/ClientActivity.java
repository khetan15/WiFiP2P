/*
 WiFi Direct File Transfer is an open source application that will enable sharing 
 of data between Android devices running Android 4.0 or higher using a WiFi direct
 connection without the use of a separate WiFi access point.This will enable data 
 transfer between devices without relying on any existing network infrastructure. 
 This application is intended to provide a much higher speed alternative to Bluetooth
 file transfers. 

 Copyright (C) 2012  Teja R. Pitla
 Contact: teja.pitla@gmail.com

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.example.ramesh.p2pfileshare;

import java.io.File;
import java.util.ArrayList;

import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
//import android.support.v4.app.NavUtils;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Toast;

import edu.pdx.cs410.wifi.direct.file.transfer.R;


public class ClientActivity extends FragmentActivity implements ServerListFragment.onServerSelectedListener {
	
	public final int fileRequestID = 98;
	public final int port = 7950;


	
	private WifiP2pManager wifiManager;
	private Channel wifichannel;
	private BroadcastReceiver wifiClientReceiver;

	private IntentFilter wifiClientReceiverIntentFilter;

	private boolean connectedAndReadyToSendFile;
	
	private boolean filePathProvided;
	private File fileToSend;
	private boolean transferActive;
    private Button sendFileButton;
    private Button discoverPeersButton;
	
	private Intent clientServiceIntent; 
	private WifiP2pDevice targetDevice;
	private WifiP2pInfo wifiInfo;

		
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        sendFileButton = (Button) findViewById(R.id.send_file_button);
        discoverPeersButton = (Button) findViewById(R.id.discover_peers_button);

        wifiManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        
        wifichannel = wifiManager.initialize(this, getMainLooper(), null);
        wifiClientReceiver = new WiFiClientBroadcastReceiver(wifiManager, wifichannel, this);
        
        wifiClientReceiverIntentFilter = new IntentFilter();;
        wifiClientReceiverIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        wifiClientReceiverIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        wifiClientReceiverIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        wifiClientReceiverIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        
        connectedAndReadyToSendFile = false;
        filePathProvided = false;
        fileToSend = null;
        transferActive = false;
        clientServiceIntent = null;
        targetDevice = null;
        wifiInfo = null;
        
        registerReceiver(wifiClientReceiver, wifiClientReceiverIntentFilter);

        setClientFileTransferStatus("Client is currently idle");
                
        //setTargetFileStatus("testing");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_client, menu);
        
        return true;
    }

    
    public void setTransferStatus(boolean status)
    {
    	connectedAndReadyToSendFile = status;
    }
    
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                //NavUtils.navigateUpFromSameTask(this);

                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    
    public void setNetworkToReadyState(boolean status, WifiP2pInfo info, WifiP2pDevice device)
    {
    	wifiInfo = info;
    	targetDevice = device;
    	connectedAndReadyToSendFile = status;
    }
    
    private void stopClientReceiver()
    {        
       	try
    	{
            unregisterReceiver(wifiClientReceiver);
    	}
    	catch(IllegalArgumentException e)
    	{
    		//This will happen if the server was never running and the stop button was pressed.
    		//Do nothing in this case.
    	}
    }
        
    public void searchForPeers(View view) {

		//Discover peers, no call back method given
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



    public void browseForFile(View view) {
        Intent clientStartIntent = new Intent(this, FileSend.class);
        startActivityForResult(clientStartIntent, fileRequestID);  
        
    }
    
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	
    	//fileToSend

    	if (resultCode == Activity.RESULT_OK && requestCode == fileRequestID) {
    		//Fetch result
    		File targetDir = (File) data.getExtras().get("file");
    		
    		if(targetDir.isFile())
    		{
    			if(targetDir.canRead())
    			{
    				fileToSend = targetDir;
    				filePathProvided = true;
    				
    				setTargetFileStatus(targetDir.getName() + " selected for file transfer");
                    sendFileButton.setVisibility(View.INVISIBLE);
    		        sendFile();
                    sendFileButton.setVisibility(View.VISIBLE);
    			}
    			else
    			{
    				filePathProvided = false;
    				setTargetFileStatus("You do not have permission to read the file " + targetDir.getName());
    			}

    		}
    		else
    		{
				filePathProvided = false;
    			setTargetFileStatus("You may not transfer a directory, please select a single file");
    		}

        }
    }
    
    
    public void sendFile() {
        
    	//Only try to send file if there isn't already a transfer active
    	if(!transferActive)
    	{
	        if(!filePathProvided)
	        {
	        	setClientFileTransferStatus("Select a file to send before pressing send");
	        }
	        else if(!connectedAndReadyToSendFile)
	        {
	        	setClientFileTransferStatus("You must be connected to a server before attempting to send a file");
	        }
	        /*
	        else if(targetDevice == null)
	        {
	        	setClientFileTransferStatus("Target Device network information unknown");
	        }
	        */
	        else if(wifiInfo == null)
	        {
	        	setClientFileTransferStatus("Missing Wifi P2P information");
	        }
	        else
	        {
	        	//Launch client service
	        	clientServiceIntent = new Intent(this, ClientService.class);
	        	clientServiceIntent.putExtra("fileToSend", fileToSend);
	        	clientServiceIntent.putExtra("port", new Integer(port));
	        	//clientServiceIntent.putExtra("targetDevice", targetDevice);
	        	clientServiceIntent.putExtra("wifiInfo", wifiInfo);
	        	clientServiceIntent.putExtra("clientResult", new ResultReceiver(null) {
		    	    @Override
		    	    protected void onReceiveResult(int resultCode, final Bundle resultData) {
		    	    	
		    	    	if(resultCode == port )
		    	    	{
			    	        if (resultData == null) {
			    	           //Client service has shut down, the transfer may or may not have been successful. Refer to message 
			    	        	transferActive = false;				    	        				    	       			    	        				    	        			    	        	
			    	        }
			    	        else
			    	        {    	        	
			    	        	final TextView client_status_text = (TextView) findViewById(R.id.file_transfer_status);

			    	        	client_status_text.post(new Runnable() {
			    	                public void run() {
			    	                	client_status_text.setText((String)resultData.get("message"));
			    	                }
			    	        	});
			    	        }
		    	    	}
		    	           	        
		    	    }
		    	});
	        	
	        	transferActive = true;
		        startService(clientServiceIntent);

	        	
	        	
	        	//end
	        }
    	}
    }
    
    @Override
    protected void onResume() {
        super.onResume();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        //Continue to listen for wifi related system broadcasts even when paused
        //stopClientReceiver();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        //Kill thread that is transferring data 
        
        //Unregister broadcast receiver
        stopClientReceiver();
    }
    
    
    public void setClientWifiStatus(String message, boolean enabled)
    {
    	TextView connectionStatusText = (TextView) findViewById(R.id.client_wifi_status_text);
    	connectionStatusText.setText(message);
        if (enabled) {
            discoverPeersButton.setEnabled(true);
            connectionStatusText.setVisibility(View.INVISIBLE);
        } else {
            discoverPeersButton.setEnabled(false);
            connectionStatusText.setVisibility(View.VISIBLE);
        }
    }
    
    public void setClientStatus(String message)
    {
    	TextView clientStatusText = (TextView) findViewById(R.id.client_status_text);
    	clientStatusText.setText(message);
        clientStatusText.setVisibility(View.VISIBLE);
    }
    
    public void setClientFileTransferStatus(String message)
    {
    	TextView fileTransferStatusText = (TextView) findViewById(R.id.file_transfer_status);
    	fileTransferStatusText.setText(message);	
    }
    
    public void setTargetFileStatus(String message)
    {
    	TextView targetFileStatus = (TextView) findViewById(R.id.selected_filename);
    	targetFileStatus.setText(message);
        targetFileStatus.setVisibility(View.VISIBLE);
    }
    
     
    public void displayPeers(final WifiP2pDeviceList peers)
    {
    	//Dialog to show errors/status
		final AlertDialog.Builder dialog = new AlertDialog.Builder(this);
		dialog.setTitle("WiFi Direct File Transfer");
		
		//Get list view
    	//ListView peerView = (ListView) findViewById(R.id.peers_listview); //--version2
    	
    	//Make array list
    	ArrayList<String> peersStringArrayList = new ArrayList<String>();

    	//Fill array list with strings of peer names
    	for(WifiP2pDevice wd : peers.getDeviceList())
    	{
    		peersStringArrayList.add(wd.deviceName);
    	}
    	
    	//Set list view as clickable
    	//peerView.setClickable(true); --version2

        String[] peersList = new String[peersStringArrayList.size()];
        peersList = peersStringArrayList.toArray(peersList);

        Log.v("WIFIP2PNEW","Total servers showing "+peersList.length);
        Bundle bundle = new Bundle();
        bundle.putStringArray("server_names",peersList);

        ServerListFragment serverListFragment = new ServerListFragment();
        serverListFragment.setArguments(bundle);

        FragmentManager fm = getSupportFragmentManager();

        ServerListFragment prevFrag = (ServerListFragment)fm.findFragmentById(R.id.myclientlayout);
        if (prevFrag != null) {
            fm.beginTransaction().remove(prevFrag).commit();
            Log.v("WIFIP2PNEW","Prevs fargment found so removing");
        } else {
            Log.v("WIFIP2PNEW","No Prevs fargment found so creating new one");
        }

        fm.beginTransaction().add(R.id.myclientlayout, serverListFragment, "myfragement").commit();
    	//Make adapter to connect peer data to list view
    	/*ArrayAdapter arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, peersStringArrayList.toArray()); //--version2

    	//Show peer data in listview
    	peerView.setAdapter(arrayAdapter);
    		
    	
		peerView.setOnItemClickListener(new OnItemClickListener() {

			public void onItemClick(AdapterView<?> arg0, View view, int arg2,long arg3) {
				
				//Get string from textview
				TextView tv = (TextView) view;
				
				WifiP2pDevice device = null;
				
				//Search all known peers for matching name
		    	for(WifiP2pDevice wd : peers.getDeviceList())
		    	{
		    		if(wd.deviceName.equals(tv.getText()))
		    			device = wd;		    			
		    	}
				
				if(device != null)
				{
					//Connect to selected peer
					connectToPeer(device);
										
				}
				else
				{
					dialog.setMessage("Failed");
					dialog.show();
										
				}							
			}			
				// TODO Auto-generated method stub				
			}); //--version2
  	*/
    }
        
    public void connectToPeer(final WifiP2pDevice wifiPeer)
    {
    	this.targetDevice = wifiPeer;
    	
    	WifiP2pConfig config = new WifiP2pConfig();
    	config.deviceAddress = wifiPeer.deviceAddress;
    	wifiManager.connect(wifichannel, config, new WifiP2pManager.ActionListener()  {
    	    public void onSuccess() {
    	    	
    	    	setClientStatus("Connection to " + targetDevice.deviceName + " sucessful");

                sendFileButton.setVisibility(View.VISIBLE);

    	    }

    	    public void onFailure(int reason) {
    	    	setClientStatus("Connection to " + targetDevice.deviceName + " failed");

    	    }
    	});    	
    
    }

    @Override
    public void onServerSelected(int position) {
        Toast.makeText(this, "Selected server at index" + position + "for connect", Toast.LENGTH_LONG).show();
    }
}
