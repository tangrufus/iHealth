package com.iHealth;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.sql.DriverManager;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Set;
import java.util.UUID;
import java.io.*;
import java.util.Vector;


import android.app.TabActivity;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;

import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Color;
import android.app.Activity;
import android.view.View;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;

import com.androidplot.Plot;
import com.androidplot.Plot.BorderStyle;
import com.androidplot.ui.SizeLayoutType;
import com.androidplot.xy.*;
import com.iHealth.R;
import com.iHealth.SampleDynamicSeries;
import com.iHealth.SampleDynamicXYDatasource;
import com.mysql.jdbc.Connection;
import com.mysql.jdbc.PreparedStatement;


public class IHealthActivity extends  TabActivity {

	private TabHost myTabhost;
	private String filename;
	//	===================================================================================
	private static final int REQUEST_ENABLE_BT = 3;
	private String mac = "";
	private Vector<String> deviceMacs = new Vector<String>();
	private Vector<String> deviceNames = new Vector<String>();
	private BluetoothAdapter mAdaptor = null;
	BluetoothDevice mDevice = null;
	BluetoothSocket mSocket = null;
	ListView list = null;
	TextView tv = null;
	@SuppressWarnings("unused")
	InputStream is;
	byte buffer[] = new byte[1024];
	int bytes;
	String str= "";
	String toastText = "";
	//	=========================================================================================
	// Message types sent from the BluetoothChatService Handler
	public static final int MESSAGE_STATE_CHANGE = 1;
	public static final int MESSAGE_READ = 2;
	public static final int MESSAGE_WRITE = 3;
	public static final int MESSAGE_DEVICE_NAME = 4;
	public static final int MESSAGE_TOAST = 5;
	public static TextView textdata = null;
	
	
	// private BluetoothChatService bltservice = null;
	// use bluetooth simulator
	private BluetoothSimulator bltservice = null;
	
	public static final int resultHR = 0;
	public static final int resultBP = 1;
	public static final int resultSBP = 2;
	public static final int resultDBP = 3;
	Calculation CalHRBP = null;
	Button rec = null;
	//===========================================================================================	
	public PipedInputStream pinDecode,  pinPlotECG, pinPlotPPG, pinCalECG, pinCalPPG;
	public PipedOutputStream poutBLT, poutDecodeECG,poutDecodePPG,poutCECG, poutCPPG;
	// ===========================================================================================
	private int[] SBPV = new int[3000];
	private int[] DBPV = new int[3000];
	private int[] HRV = new int[3000];
	
	private int iSBPV =0;
	private int iDBPV =0;
	private int iHRV =0;
	
	private int mSBPV =0;
	private int mDBPV =0;
	private int mHRV =0;
	
//	Thread TCalHRBP = new Thread(CalHRBP); 
//	Thread Tbltservice = new Thread(bltservice);
	
	
	// ===========================================================================================
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d("MAIN__", "onCreat started");
		
		poutBLT = new PipedOutputStream();
		poutDecodeECG = new PipedOutputStream();
		poutDecodePPG =  new PipedOutputStream();
		poutCECG = new PipedOutputStream();
		poutCPPG = new PipedOutputStream();
		try {
			pinDecode = new PipedInputStream(poutBLT);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			pinPlotECG = new PipedInputStream(poutDecodeECG);

			pinPlotPPG = new PipedInputStream(poutDecodePPG);
			pinCalECG = new PipedInputStream(poutCECG);
			pinCalPPG = new PipedInputStream(poutCPPG);

		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		//		  ===================================================================================================
		
		//bltservice = new BluetoothChatService(this, mHandler,poutDecodeECG, poutDecodePPG, poutCECG,poutCPPG);
		// use bluetooth simulator
		bltservice = new BluetoothSimulator(this, mHandler,poutDecodeECG, poutDecodePPG, poutCECG,poutCPPG);

		CalHRBP = new Calculation(this, calHandler, 125, 75, 4000, pinCalECG, pinCalPPG);

		
		/*
		mAdaptor = BluetoothAdapter.getDefaultAdapter();
		if(!mAdaptor.isEnabled())
		{
			try{
				toastText = "Bluethood not Enabled";
				Toast.makeText(this, toastText, Toast.LENGTH_LONG);
				Intent enableBtIntent = new Intent(
						BluetoothAdapter.ACTION_REQUEST_ENABLE);
				startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
				Toast.makeText(this, "BlueTooth Enabled", Toast.LENGTH_LONG).show();
			}
			catch(Exception e){};
		}
		Set<BluetoothDevice> pairedDevices = mAdaptor.getBondedDevices();
		if (pairedDevices.size() > 0) {
			// Loop through paired devices
			for (BluetoothDevice device : pairedDevices) {
				// Add the name and address to an array adapter to show in a
				// ListView
				deviceMacs.add(device.getAddress());
				deviceNames.add(device.getName());

			}
		} else {
			Toast.makeText(this, "Size=" + pairedDevices.size(),
					Toast.LENGTH_LONG).show();
		}
		list = (ListView)findViewById(R.id.listView1);
		list.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1 , deviceNames));

		list.setOnItemClickListener(new OnItemClickListener()
		{
			public void onItemClick(AdapterView<?> a, View v, int position, long id){
				mac = deviceMacs.get(deviceNames.indexOf(list.getItemAtPosition(position)));
				mDevice = mAdaptor.getRemoteDevice(mac);
				try{

					bltservice.connect(mDevice);


				}
				catch(Exception e)
				{
					getAlertDialog("state","connection fail");

				};
			}
		});
		*/
		//		=================================================================================================

		myTabhost=this.getTabHost();
		LayoutInflater.from(this).inflate(R.layout.main, myTabhost.getTabContentView(), true);
		myTabhost.addTab(

				myTabhost.newTabSpec("blt config")
				.setIndicator("Options")
				.setContent(R.id.linearLayout05)
				);
		
		rec = (Button) findViewById(R.id.Record);

		rec.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				String text = rec.getText().toString();
				if(text.equals("Start")){
					try {
						filename = getTime("yyyyMMMddHmm");
						bltservice.saveData.CreatFile(filename);
						rec.setText("Recording...");
						bltservice.Records = true;
						Thread TCalHRBP = new Thread(CalHRBP); 
						TCalHRBP.start();
						Thread Tbltservice = new Thread(bltservice);
						Tbltservice.start();
						Log.d("MAIN__", "bltservice.Records ="+bltservice.Records);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						Log.e("MAIN__", "Createfile failed");
						e.printStackTrace();
					}
				}
				else if(text.equals("Recording...")){
					Log.d("MAIN__", "local save done");
					rec.setText("mSBPV, mDBPV, mHRV = " + mSBPV +", " + mDBPV +", " + mHRV);
					bltservice.saveData.EndSave();
					bltservice.Records = false;
					Log.d("MAIN__", "bltservice.Records ="+bltservice.Records);
					CalHRBP.Continue = false;
					//TCalHRBP.join();
					//
					/* for real bluetooth connection only
					try {
						Tbltservice.Continue = false;
						Tbltservice.join();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					*/
					MySQL.access(filename);
					Log.d("MAIN__", "mysql saved");
					
				}	
			}
		});

		//===================================================================================================================
	}
	public String getTime(String format)
	{
		Calendar currentDate = Calendar.getInstance();
		SimpleDateFormat formatter= 
				new SimpleDateFormat(format);
		String dateNow = formatter.format(currentDate.getTime());
		return dateNow;
	}
	private AlertDialog getAlertDialog(String title,String message){
		//Builder
		Builder builder = new AlertDialog.Builder(IHealthActivity.this);
		//wDialog
		builder.setTitle(title);
		//wDialog
		builder.setMessage(message);
		//wPositive
		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				//
				//				Toast.makeText(IHealthActivity.this, "�z���UOK���s", Toast.LENGTH_SHORT).show();
			}
		});
		//wNegative
		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				//
				//				Toast.makeText(IHealthActivity.this, "�z���UCancel���s", Toast.LENGTH_SHORT).show();
			}
		});
		//BuilderAlertDialog
		builder.show();
		return builder.create();
	}


	//	=====================================================================================================
	// The Handler that gets information back from the BluetoothChatService
	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MESSAGE_STATE_CHANGE:
				//                if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
				switch (msg.arg1) {
				case BluetoothChatService.STATE_CONNECTED:
					break;
				case BluetoothChatService.STATE_CONNECTING:
					break;
				case BluetoothChatService.STATE_LISTEN:
				case BluetoothChatService.STATE_NONE:
					//final TextView SBPV = (TextView)findViewById(R.id.SBPV);
					//final TextView DBPV = (TextView)findViewById(R.id.DBPV);
					//final TextView HRV = (TextView)findViewById(R.id.HRV);
					//SBPV.setText("--");
					//DBPV.setText("--");
					//HRV.setText("--");
					break;
				}
				break;
			case MESSAGE_WRITE:
				byte[] writeBuf = (byte[]) msg.obj;
				// construct a string from the buffer
				String writeMessage = new String(writeBuf);
				break;
			case MESSAGE_READ:

				// construct a string from the valid bytes in the buffer
				int[] buff = (int[]) msg.obj;
				break;
			case MESSAGE_DEVICE_NAME:
				break;
			case MESSAGE_TOAST:
				break;
			}
		}
	};
	String tmp ="";
	int size =8;
	public int[] ECG = new int[3000];
	public int[] PPG = new int[3000];

	//	  ===================================================================================================
	static final String HEXES = "0123456789ABCDEF";
	public static String getHex( byte [] raw ) {
		if ( raw == null ) {
			return null;
		}
		final StringBuilder hex = new StringBuilder( 2 * raw.length );
		for ( final byte b : raw ) {
			hex.append(HEXES.charAt((b & 0xF0) >> 4))
			.append(HEXES.charAt((b & 0x0F)));
		}
		return hex.toString();
	}

	//	  ===================================================================================================
	DataQueue DQ = new DataQueue(5);
	public void CheckPackage(byte[] buffer)
	{

		int header;
		
		for(int i = 0;i<=buffer.length-1;i++)
		{
			DQ.push(buffer[i]);
			header = DQ.getHead();
			if( (header & 0x80) == 0x80)
			{
				decode();
				DQ.Clear();
			}
		}

	}
	int ecg , ppg;
	//int count=0;
	short checksum;
	public void decode()
	{
		for(int i = 1;i< DQ.queue.length-1;i++)
		{
			checksum+=DQ.queue[i];
		}

		int k = (255 -checksum);
		int f = DQ.queue[DQ.queue.length-1];
		if( (255 -checksum) == DQ.queue[DQ.queue.length-1])
		{



			ecg = ((DQ.queue[1] & 0x7F) << 3) +((DQ.queue[2] & 0x70) >> 4);
			ppg = ((DQ.queue[2] & 0x07) << 7) + (DQ.queue[3] & 0x7F);
			
		}
		//count++;
		checksum = 0;
	}
	public final Handler calHandler = new Handler()
	{
		@Override
		public void handleMessage(Message msg)
		{
			int[] results = (int[]) msg.obj;
	
			switch(msg.what) {
				case resultHR:				
					if(results[0]!=-1 && results[0]>=40){
						HRV[iHRV++]=results[0];
						Log.e("MAIN__", "HRV =" + results[0]);
					} else {
						Log.e("MAIN__", "HRV = ERROR");
					}

					if(results[2]!=-1 && results[2]!=0 && results[2]>=70 && results[3]!=-1 && results[3]!=0 && results[3]>=50) {
						SBPV[iSBPV++] = results[2];
						DBPV[iDBPV++] = results[3];
						Log.e("MAIN__", "SBPV, DBPV =" + results[2] +", " + results[3]);
					} else {
						Log.e("MAIN__", "SBPV = ERROR && DBPV = ERROR");
					}
				break;
			}
		}
	};

	
	
	private void getResults() {
		try {
			mSBPV = getMean(iSBPV, SBPV);
			mDBPV = getMean(iDBPV, DBPV);
			mHRV = getMean(iHRV, HRV);
			Log.d("MAIN__", "mSBPV, mDBPV, mHRV = " + mSBPV +", " + mDBPV +", " + mHRV);
			
		} catch (Exception e) {
			Log.e("MAIN__", "can't get mean result");
			e.printStackTrace();
		}

	}
	
	private int getMean(int count, int[] array) {
		int sum=0;
		for (int i=0; i<count; i++) {
			sum += array[i];
		}
		
		return sum/count;
		
	}

}