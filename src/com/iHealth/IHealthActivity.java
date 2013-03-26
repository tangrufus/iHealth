package com.iHealth;

import java.util.Calendar;
import java.util.Date;
import java.util.Observable;
import java.util.Observer;
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


public class IHealthActivity extends  TabActivity {


	private XYPlot dynamicPlot;
	private MyPlotUpdater plotUpdater;
	WebView wv;
	private TabHost myTabhost;
	Button zoom_in = null;
	Button zoom_out = null;
	TextView samplesize = null;
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
	public static TextView textdata = null;// 
	// private BluetoothChatService bltservice = null;
	// use bluetooth simulator
	private BluetoothSimulator bltservice = null;
	
	SampleDynamicXYDatasource data = null;
	public static final int resultHR = 0;
	public static final int resultBP = 1;
	public static final int resultSBP = 2;
	public static final int resultDBP = 3;
	Calculation CalHRBP =null;
	Button rec = null;
	//===========================================================================================	
	public PipedInputStream pinDecode,  pinPlotECG, pinPlotPPG, pinCalECG, pinCalPPG;
	public PipedOutputStream poutBLT, poutDecodeECG,poutDecodePPG,poutCECG, poutCPPG;
	// ===========================================================================================
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		myTabhost=this.getTabHost();

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
		new Thread(CalHRBP).start();
		//		
		//===================================================================================================

		LayoutInflater.from(this).inflate(R.layout.main, myTabhost.getTabContentView(), true);
		wv = (WebView) findViewById(R.id.wv);

		myTabhost.addTab(
				myTabhost.newTabSpec("Monitor")
				.setIndicator("Monitor", getResources().getDrawable(R.drawable.png1))
				.setContent(R.id.linearLayout01)
				); 
		myTabhost.addTab(
				myTabhost.newTabSpec("Calibration")
				.setIndicator("Calibration", getResources().getDrawable(R.drawable.png2))
				.setContent(R.id.linearLayout02)
				);
		myTabhost.addTab(

				myTabhost.newTabSpec("About Us")
				.setIndicator("About Us", getResources().getDrawable(R.drawable.png3))
				.setContent(R.id.linearLayout03)

				);    
		myTabhost.addTab(

				myTabhost.newTabSpec("blt config")
				.setIndicator("Options")
				.setContent(R.id.linearLayout05)


				);    


		wv = (WebView) findViewById(R.id.wv);
		wv.setWebViewClient(new WebViewClient() {			
			public void onReceivedError(WebView view, 
					int errorCode, String description, String failingUrl) {
				Toast.makeText(IHealthActivity.this, "Sorry!" + description, Toast.LENGTH_SHORT).show();
			}
		});

		wv.loadUrl("http://bme.ee.cuhk.edu.hk/jcbme/index.htm");
		final Button OkButton = (Button) findViewById(R.id.Button01);		
		final EditText CEditText06=(EditText)findViewById(R.id.CEditText06);			
		final TextView MTextView9 =(TextView)findViewById(R.id.MTextView9);	


		//		  ===================================================================================================
		// get handles to our View defined in layout.xml:
		dynamicPlot = (XYPlot) findViewById(R.id.dynamicPlot);        
		plotUpdater = new MyPlotUpdater(dynamicPlot);

		dynamicPlot.getGraphWidget().setDomainValueFormat(new DecimalFormat("0"));

		data = new SampleDynamicXYDatasource(pinPlotECG, pinPlotPPG);
		final SampleDynamicSeries sine1Series = new SampleDynamicSeries(data, 0, "Sine 1");

		dynamicPlot.addSeries(sine1Series, new LineAndPointFormatter(Color.BLUE, null, Color.TRANSPARENT));
		dynamicPlot.setGridPadding(0, 0, 0, 0);
		data.addObserver(plotUpdater);

		dynamicPlot.setDomainStepMode(XYStepMode.SUBDIVIDE);
		dynamicPlot.setDomainStepValue(sine1Series.size());

		// thin out domain/range tick labels so they dont overlap each other:
		dynamicPlot.setTicksPerDomainLabel(5);
		dynamicPlot.setTicksPerRangeLabel(3);
		dynamicPlot.disableAllMarkup();

		// freeze the range boundaries:
		dynamicPlot.setRangeBoundaries(0, 1000, BoundaryMode.AUTO);

		dynamicPlot.getGraphWidget().getDomainLabelPaint().setAlpha(0);
		dynamicPlot.getGraphWidget().setDomainLabelTickExtension(0);
		dynamicPlot.getGraphWidget().getDomainOriginLinePaint().setAlpha(0);
		dynamicPlot.getGraphWidget().getDomainLabelPaint().setAlpha(0);

		dynamicPlot.getGraphWidget().getRangeLabelPaint().setAlpha(0);
		dynamicPlot.getGraphWidget().getDomainLabelPaint().setAlpha(0);
		dynamicPlot.getGraphWidget().setRangeLabelTickExtension(0);
		dynamicPlot.getGraphWidget().getRangeOriginLinePaint().setAlpha(0);


		dynamicPlot.getGraphWidget().getRangeLabelPaint().setAlpha(0);

		dynamicPlot.getGraphWidget().getCursorLabelBackgroundPaint().setAlpha(0);
		dynamicPlot.getDomainLabelWidget().setVisible(false);
		dynamicPlot.getRangeLabelWidget().setVisible(false);
		dynamicPlot.getBorderPaint().setAlpha(0);
		dynamicPlot.getLegendWidget().setVisible(false);
		dynamicPlot.getGraphWidget().setGridBackgroundPaint(null);
		dynamicPlot.setBorderStyle(BorderStyle.NONE, null, null);
		dynamicPlot.getBorderPaint().setColor(Color.WHITE);

		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		zoom_in = (Button) findViewById(R.id.zoom_in);
		zoom_out = (Button) findViewById(R.id.zoom_out);
		samplesize = (TextView)findViewById(R.id.sampsize);
		zoom_in.setOnClickListener(
				new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						// TODO Auto-generated method stub
						if (size > 6)
							size -=2;
						bltservice.Changesamplesize(size);
						samplesize.setText("Sampling size = "+ size);

					}
				});
		zoom_out.setOnClickListener(
				new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						// TODO Auto-generated method stub
						if (size <50)
							size +=2;
						bltservice.Changesamplesize(size);
						samplesize.setText("Sampling size = "+ size);
					}
				});
		dynamicPlot.setOnClickListener(
				new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						// TODO Auto-generated method stub
						data.channelswitch();
						if(data.chan)
						{
							dynamicPlot.setTitle("ECG");


						}
						else{
							dynamicPlot.setTitle("PPG");
						}

					}
				});

		//		  ===================================================================================================

		new Thread(data).start();


		//		  ===================================================================================================
		OkButton.setOnClickListener(
				new View.OnClickListener()
				{ 
					public void onClick(View v){ 					
						String CEditText06str=CEditText06.getText().toString();		
						MTextView9.setText(CEditText06str);
					} 
				}); 
		//		====================================================================================================

		/* Bluetooth connection */
		// use bluetooth simulator
		
		new Thread(bltservice).start();
		
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


		rec = (Button) findViewById(R.id.Record);

		rec.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				String text = rec.getText().toString();
				if(text.equals("Record Data")){
					rec.setText("Stop Recording");
					String filename = getTime("yyyyMMMddHmm");

					try {
						bltservice.saveData.CreatFile(filename);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					bltservice.Records = true;}
				else 
				{
					rec.setText("Record");
					bltservice.saveData.EndSave();
					bltservice.Records = false;
				}	
			}
		});
		Button Ecall = (Button) findViewById(R.id.Dial);
		Ecall.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Intent dialIntent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:66983226"));

				startActivity(dialIntent);

			}
		});

		Button Email = (Button) findViewById(R.id.Email);
		Email.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Intent i = new Intent(Intent.ACTION_SEND);
				i.setType("text/plain");
				i.putExtra(Intent.EXTRA_EMAIL  , new String[]{"waadiox@gmail.com"});
				i.putExtra(Intent.EXTRA_SUBJECT, "subject of email");
				i.putExtra(Intent.EXTRA_TEXT   , "body of email");
			
				try {
					startActivity(Intent.createChooser(i, "Send mail..."));
				} catch (android.content.ActivityNotFoundException ex) {

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
					final TextView SBPV = (TextView)findViewById(R.id.SBPV);
					final TextView DBPV = (TextView)findViewById(R.id.DBPV);
					final TextView HRV = (TextView)findViewById(R.id.HRV);
					SBPV.setText("--");
					DBPV.setText("--");
					HRV.setText("--");
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
	// redraws a plot whenever an update is received:
	private class MyPlotUpdater implements Observer {
		Plot plot;
		public MyPlotUpdater(Plot plot) {
			this.plot = plot;
		}
		public void update(Observable o, Object arg) {
			plot.redraw();
		}
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
	int count=0;
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
		count++;
		if (count%size==0);
		data.update(ecg, ppg);
		checksum = 0;
	}
	public final Handler calHandler = new Handler()
	{


		@Override
		public void handleMessage(Message msg)
		{
			int[] results = (int[]) msg.obj;
			final TextView SBPV = (TextView)findViewById(R.id.SBPV);
			final TextView DBPV = (TextView)findViewById(R.id.DBPV);
			final TextView HRV = (TextView)findViewById(R.id.HRV);
			
			switch(msg.what)
			{case resultHR:				
				if(results[0]!=-1 && results[0]>=40){
					HRV.setText(results[0]+"");}
				else{HRV.setText("--");}

				if(results[2]!=-1 && results[2]!=0 && results[2]>=70 && results[3]!=-1 && results[3]!=0 && results[3]>=50){
					SBPV.setText(results[2]+"");DBPV.setText(results[3]+"");}
				else{
					SBPV.setText("--");DBPV.setText("--");	
				}
				break;

			}

		}
	};

}