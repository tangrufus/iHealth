package com.iHealth;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.io.*;
import java.util.Vector;

import android.annotation.SuppressLint;
import android.app.TabActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;

import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TextView;
import android.view.View;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
//import android.bluetooth.BluetoothAdapter;
//import android.bluetooth.BluetoothDevice;
//import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Message;

import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.iHealth.R;

@SuppressLint("SimpleDateFormat")
public class IHealthActivity extends TabActivity {

	private TabHost myTabhost;
	private String filename, uid;
	// ===================================================================================
	// private static final int REQUEST_ENABLE_BT = 3;
	// private String mac = "";
	// private Vector<String> deviceMacs = new Vector<String>();
	// private Vector<String> deviceNames = new Vector<String>();
	// private BluetoothAdapter mAdaptor = null;
	// BluetoothDevice mDevice = null;
	// BluetoothSocket mSocket = null;
	// ListView list = null;
	TextView tv = null;
	InputStream is;
	byte buffer[] = new byte[1024];
	int bytes;
	String str = "";
	String toastText = "";
	// =========================================================================================
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
	TextView display = null;
	// ===========================================================================================
	public PipedInputStream pinDecode, pinPlotECG, pinPlotPPG, pinCalECG,
			pinCalPPG;
	public PipedOutputStream poutBLT, poutDecodeECG, poutDecodePPG, poutCECG,
			poutCPPG;
	// ===========================================================================================
	private int[] SBPV = new int[3000];
	private int[] DBPV = new int[3000];
	private int[] HRV = new int[3000];

	private int iSBPV = 0;
	private int iDBPV = 0;
	private int iHRV = 0;

	private int mSBPV = 0;
	private int mDBPV = 0;
	private int mHRV = 0;
	
	private int simMode = 1;

	private int bsSBPV = getRandomInt(90, 119);
	private int bsDBPV = getRandomInt(60, 79);
	private int bsHRV = getRandomInt(65, 95);

	// Thread TCalHRBP = new Thread(CalHRBP);
	// Thread Tbltservice = new Thread(bltservice);

	private Timer timer = new Timer();
	private int second = 30;

	// ===========================================================================================
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d("MAIN__", "onCreat started");
		poutBLT = new PipedOutputStream();
		poutDecodeECG = new PipedOutputStream();
		poutDecodePPG = new PipedOutputStream();
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

		// ===================================================================================================

		// bltservice = new BluetoothChatService(this, mHandler,poutDecodeECG,
		// poutDecodePPG, poutCECG,poutCPPG);
		// use bluetooth simulator
		// bltservice = new BluetoothSimulator(this, mHandler,poutDecodeECG,
		// poutDecodePPG, poutCECG,poutCPPG);

		// CalHRBP = new Calculation(this, calHandler, 125, 75, 4000, pinCalECG,
		// pinCalPPG);

		/*
		 * mAdaptor = BluetoothAdapter.getDefaultAdapter();
		 * if(!mAdaptor.isEnabled()) { try{ toastText = "Bluethood not Enabled";
		 * Toast.makeText(this, toastText, Toast.LENGTH_LONG); Intent
		 * enableBtIntent = new Intent( BluetoothAdapter.ACTION_REQUEST_ENABLE);
		 * startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		 * Toast.makeText(this, "BlueTooth Enabled", Toast.LENGTH_LONG).show();
		 * } catch(Exception e){}; } Set<BluetoothDevice> pairedDevices =
		 * mAdaptor.getBondedDevices(); if (pairedDevices.size() > 0) { // Loop
		 * through paired devices for (BluetoothDevice device : pairedDevices) {
		 * // Add the name and address to an array adapter to show in a //
		 * ListView deviceMacs.add(device.getAddress());
		 * deviceNames.add(device.getName()); } } else { Toast.makeText(this,
		 * "Size=" + pairedDevices.size(), Toast.LENGTH_LONG).show(); } list =
		 * (ListView)findViewById(R.id.listView1); list.setAdapter(new
		 * ArrayAdapter<String>(this,android.R.layout.simple_list_item_1 ,
		 * deviceNames));
		 * 
		 * list.setOnItemClickListener(new OnItemClickListener() { public void
		 * onItemClick(AdapterView<?> a, View v, int position, long id){ mac =
		 * deviceMacs
		 * .get(deviceNames.indexOf(list.getItemAtPosition(position))); mDevice
		 * = mAdaptor.getRemoteDevice(mac); try{ bltservice.connect(mDevice); }
		 * catch(Exception e) { getAlertDialog("state","connection fail"); }; }
		 * });
		 */
		// =================================================================================================

		myTabhost = this.getTabHost();
		LayoutInflater.from(this).inflate(R.layout.main,
				myTabhost.getTabContentView(), true);
		myTabhost.addTab(

		myTabhost.newTabSpec("blt config").setIndicator("iHealth 2")
				.setContent(R.id.linearLayout05));

		rec = (Button) findViewById(R.id.RecordBtn);
		display = (TextView) findViewById(R.id.textView1);

		rec.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				String text = rec.getText().toString();

				if (text.equals("Start")) {
					rec.setText("Measure");
					display.setText("Sit Still For 5 Min Before Measurement");
					ShowAlertDialogAnd3Button();
				} else if (text.equals("Measure")) {
					/*
					 * try { filename = getTime("dd-MM-yyyy--HH-mm"); uid =
					 * getTime("dd/MM/yyyy  HH:mm");
					 * bltservice.saveData.CreatFile(filename);
					 * display.setText("Initializing"); bltservice.Records =
					 * true; Thread TCalHRBP = new Thread(CalHRBP);
					 * TCalHRBP.start(); Thread Tbltservice = new
					 * Thread(bltservice); Tbltservice.start();
					 * display.setText("Recording");
					 * rec.setText("Stop Recording"); Log.d("MAIN__",
					 * "bltservice.Records ="+bltservice.Records); } catch
					 * (IOException e) { // TODO Auto-generated catch block
					 * Log.e("MAIN__", "Createfile failed");
					 * e.printStackTrace(); }
					 */
					display.setText("Recording");
					rec.setText("Stop Recording");
					startCountdown();

				} else if (text.equals("Stop Recording")) {
					/* 
					 * try {
						rec.setText("Upload");
						display.setText("Press The Button To Upload");
						//bltservice.saveData.EndSave();
						//bltservice.Records = false;
						//Log.d("MAIN__", "bltservice.Records ="
						//		+ bltservice.Records);
						//CalHRBP.Continue = false;
						//
						/*
						 * for real bluetooth connection only try {
						 * Tbltservice.Continue = false; Tbltservice.join(); }
						 * catch (InterruptedException e) { // TODO
						 * Auto-generated catch block e.printStackTrace(); }
						 */
						//Log.d("MAIN__", "local save done");
					/*} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					*/
					
				//} else if (text.equals("Upload")) {
		/*
					try {
						display.setText("Uploading...\n Please wait for a while");
						rec.setText("Uploading...");
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} finally {

					}
				} else if (text.equals("Uploading...")) {
				*/	
					timer.cancel();
					filename = getTime("dd-MM-yyyy--HH-mm"); 
					uid = getTime("dd/MM/yyyy  HH:mm");
					MySQL.access(filename, uid, second, bsSBPV, bsDBPV, bsHRV, simMode);
					Log.d("MAIN__", "mysql saved");
					display.setText("Uploaded!");
					rec.setText("Done");
				} else if (text.equals("Done")) {
					rec.setText("Next");
					XYPlot xy = (XYPlot) findViewById(R.id.aprHistoryPlot);
					xy.setVisibility(0);
					inCreate();
				} else if (text.equals("Next")) {
					indicator = 3;
					UpperBound = 200;
					LowerBound = 50;
					aprHistoryPlot.setRangeBoundaries(LowerBound, UpperBound,
							BoundaryMode.FIXED);
					rec.setEnabled(false);

				} else {
					rec.setText("Start");
					display.setText("iHealth 2");
				}
			}
		});

		// ===================================================================================================================
	}

	public String getTime(String format) {
		Calendar currentDate = Calendar.getInstance();
		SimpleDateFormat formatter = new SimpleDateFormat(format);
		String dateNow = formatter.format(currentDate.getTime());
		return dateNow;
	}

	private int getRandomInt(int Min, int Max) {
		return Min + (int) (Math.random() * ((Max - Min) + 1));
	}

	// 三個按鈕的對話方塊展示
	private void ShowAlertDialogAnd3Button() {
		Builder MyAlertDialog = new AlertDialog.Builder(this);
		MyAlertDialog.setTitle("Bluetooth Simulator");
		MyAlertDialog.setMessage("Choose Simulation Mode");
		// 建立按下按鈕
		DialogInterface.OnClickListener OkClick = new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				// 如果不做任何事情 就會直接關閉 對話方塊
				Log.d("MAIN__", "Ans: " + which);
				switch (which) {
				case -2:
					
					simMode = 0;
					break;
				case -3:
					
					simMode = 1;
					break;
				case -1:
			
					simMode = 2;
					break;
				default:
					Log.d("MAIN__", "Ans: default");
					simMode = 1;
					break;
				}

				bsHRV = getRandomInt(65, 95);
			}
		};
		MyAlertDialog.setPositiveButton("Hypertension", OkClick);
		MyAlertDialog.setNeutralButton("Normal", OkClick);
		MyAlertDialog.setNegativeButton("Hypotension", OkClick);
		MyAlertDialog.show();
	}

	// =====================================================================================================
	// The Handler that gets information back from the BluetoothChatService
	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MESSAGE_STATE_CHANGE:
				// if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
				switch (msg.arg1) {
				case BluetoothChatService.STATE_CONNECTED:
					break;
				case BluetoothChatService.STATE_CONNECTING:
					break;
				case BluetoothChatService.STATE_LISTEN:
				case BluetoothChatService.STATE_NONE:
					// final TextView SBPV = (TextView)findViewById(R.id.SBPV);
					// final TextView DBPV = (TextView)findViewById(R.id.DBPV);
					// final TextView HRV = (TextView)findViewById(R.id.HRV);
					// SBPV.setText("--");
					// DBPV.setText("--");
					// HRV.setText("--");
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
	String tmp = "";
	int size = 8;
	public int[] ECG = new int[3000];
	public int[] PPG = new int[3000];

	// ===================================================================================================
	static final String HEXES = "0123456789ABCDEF";

	public static String getHex(byte[] raw) {
		if (raw == null) {
			return null;
		}
		final StringBuilder hex = new StringBuilder(2 * raw.length);
		for (final byte b : raw) {
			hex.append(HEXES.charAt((b & 0xF0) >> 4)).append(
					HEXES.charAt((b & 0x0F)));
		}
		return hex.toString();
	}

	// ===================================================================================================
	DataQueue DQ = new DataQueue(5);

	public void CheckPackage(byte[] buffer) {

		int header;

		for (int i = 0; i <= buffer.length - 1; i++) {
			DQ.push(buffer[i]);
			header = DQ.getHead();
			if ((header & 0x80) == 0x80) {
				decode();
				DQ.Clear();
			}
		}

	}

	int ecg, ppg;
	short checksum;

	public void decode() {
		for (int i = 1; i < DQ.queue.length - 1; i++) {
			checksum += DQ.queue[i];
		}

		int k = (255 - checksum);
		int f = DQ.queue[DQ.queue.length - 1];
		if ((255 - checksum) == DQ.queue[DQ.queue.length - 1]) {

			ecg = ((DQ.queue[1] & 0x7F) << 3) + ((DQ.queue[2] & 0x70) >> 4);
			ppg = ((DQ.queue[2] & 0x07) << 7) + (DQ.queue[3] & 0x7F);

		}
		// count++;
		checksum = 0;
	}

	public final Handler calHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			int[] results = (int[]) msg.obj;

			switch (msg.what) {
			case resultHR:
				if (results[0] != -1 && results[0] >= 40) {
					HRV[iHRV++] = results[0];
					Log.e("MAIN__", "HRV =" + results[0]);
				} else {
					Log.e("MAIN__", "HRV = ERROR");
				}

				if (results[2] != -1 && results[2] != 0 && results[2] >= 70
						&& results[3] != -1 && results[3] != 0
						&& results[3] >= 50) {
					SBPV[iSBPV++] = results[2];
					DBPV[iDBPV++] = results[3];
					Log.d("MAIN__", "SBPV, DBPV =" + results[2] + ", "
							+ results[3]);
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
			Log.d("MAIN__", "mSBPV, mDBPV, mHRV = " + mSBPV + ", " + mDBPV
					+ ", " + mHRV);

		} catch (Exception e) {
			Log.e("MAIN__", "can't get mean result");
			e.printStackTrace();
		}

	}

	private int getMean(int count, int[] array) {
		int sum = 0;
		for (int i = 0; i < count; i++) {
			sum += array[i];
		}

		return sum / count;

	}

	/**
	 * A simple formatter to convert bar indexes into sensor names.
	 */
	private class APRIndexFormat extends Format {
		@Override
		public StringBuffer format(Object obj, StringBuffer toAppendTo,
				FieldPosition pos) {
			Number num = (Number) obj;

			// using num.intValue() will floor the value, so we add 0.5 to round
			// instead:
			int roundNum = (int) (num.floatValue() + 0.5f);
			switch (roundNum) {
			case 0:
				toAppendTo.append("ECG / HRV");
				break;
			case 1:
				toAppendTo.append("PPG / SBPV");
				break;
			case 2:
				toAppendTo.append("DBPV");
				break;
			default:
				toAppendTo.append("Unknown");
			}
			return toAppendTo;
		}

		@Override
		public Object parseObject(String source, ParsePosition pos) {
			return null; // We don't use this so just return null for now.
		}
	}

	private static final int HISTORY_SIZE = 30; // number of points to plot in
												// history
	private SensorManager sensorMgr = null;
	private Sensor orSensor = null;

	private XYPlot aprHistoryPlot = null;

	private CheckBox hwAcceleratedCb;
	private CheckBox showFpsCb;
	private SimpleXYSeries aprLevelsSeries = null;
	private SimpleXYSeries azimuthHistorySeries = null;
	private SimpleXYSeries pitchHistorySeries = null;
	private SimpleXYSeries rollHistorySeries = null;
	private int i = 0;

	private List<Float> list = new ArrayList<Float>();
	private List<Float> list1 = new ArrayList<Float>();
	private List<Float> list2 = new ArrayList<Float>();
	private List<Float> list3 = new ArrayList<Float>();
	private List<Float> list4 = new ArrayList<Float>();
	private int indicator = 2;
	private double LowerBound = 0;
	private double UpperBound = 1000;

	/** Called when the activity is first created. */
	public void inCreate() {
		// super.onCreate(savedInstanceState);
		// setContentView(R.layout.activity_main);

		// setup the APR Levels plot:

		String path2;
		String path3;
		
		switch(simMode) {
			case 2:
				path2 = "/sdcard/high2.csv";
				path3 = "/sdcard/high3.csv";
				break;
			case 0:
				path2 = "/sdcard/low2.csv";
				path3 = "/sdcard/low3.csv";
				break;
			default:
				path2 = "/sdcard/2.csv";
				path3 = "/sdcard/3.csv";
				break;
		}
		
		
		
		try {
			File myFile = new File(path2);
			FileInputStream fIn = new FileInputStream(myFile);
			BufferedReader myReader = new BufferedReader(new InputStreamReader(
					fIn));
			String aDataRow = "";
			while ((aDataRow = myReader.readLine()) != null) {
				String[] a = aDataRow.split(",");
				list.add(Float.parseFloat(a[0]));
				//Log.d("MAIN__", a[0]);
				list1.add(Float.parseFloat(a[1]));
				//Log.e("MAIN__", a[1]);
			}
			myFile = new File(path3);
			fIn = new FileInputStream(myFile);
			myReader = new BufferedReader(new InputStreamReader(fIn));
			aDataRow = "";
			while ((aDataRow = myReader.readLine()) != null) {
				String[] a = aDataRow.split(",");
				list2.add(Float.parseFloat(a[0]));
				list3.add(Float.parseFloat(a[1]));
				list4.add(Float.parseFloat(a[2]));
			}

			myReader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		aprLevelsSeries = new SimpleXYSeries("APR Levels");
		aprLevelsSeries.useImplicitXVals();

		// any of the orientation sensors is -180 and 359 respectively so we
		// will fix our plot's
		// boundaries to those values. If we did not do this, the plot would
		// auto-range which

		// setup the APR History plot:
		aprHistoryPlot = (XYPlot) findViewById(R.id.aprHistoryPlot);

		azimuthHistorySeries = new SimpleXYSeries("ECG / HRV");
		azimuthHistorySeries.useImplicitXVals();
		pitchHistorySeries = new SimpleXYSeries("PPG / SBPV");
		pitchHistorySeries.useImplicitXVals();
		rollHistorySeries = new SimpleXYSeries("DBPV");
		rollHistorySeries.useImplicitXVals();

		aprHistoryPlot.setRangeBoundaries(LowerBound, UpperBound,
				BoundaryMode.FIXED);
		aprHistoryPlot.setDomainBoundaries(0, 30, BoundaryMode.FIXED);
		aprHistoryPlot
				.addSeries(azimuthHistorySeries, new LineAndPointFormatter(
						Color.rgb(100, 100, 200), null, null));
		aprHistoryPlot.addSeries(pitchHistorySeries, new LineAndPointFormatter(
				Color.rgb(100, 200, 100), null, null));
		aprHistoryPlot.addSeries(rollHistorySeries, new LineAndPointFormatter(
				Color.rgb(200, 100, 100), null, null));
		aprHistoryPlot.setDomainStepValue(5);
		aprHistoryPlot.setTicksPerRangeLabel(3);
		// aprHistoryPlot.setDomainLabel("Sample Index");
		// aprHistoryPlot.getDomainLabelWidget().pack();
		// aprHistoryPlot.setRangeLabel("Angle (Degs)");
		// aprHistoryPlot.getRangeLabelWidget().pack();

		// setup checkboxes:

		//
		// // register for orientation sensor events:
		// sensorMgr = (SensorManager)
		// getApplicationContext().getSystemService(Context.SENSOR_SERVICE);
		// for (Sensor sensor :
		// sensorMgr.getSensorList(Sensor.TYPE_ORIENTATION)) {
		// if (sensor.getType() == Sensor.TYPE_ORIENTATION) {
		// orSensor = sensor;
		// }
		// }
		//
		// // if we can't access the orientation sensor then exit:
		// if (orSensor == null) {
		// System.out.println("Failed to attach to orSensor.");
		// cleanup();
		// }

		// sensorMgr.registerListener(this, orkSensor,
		// SensorManager.SENSOR_DELAY_UI);

		// Called whenever a new orSensor reading is taken.

		TimerTask t = new TimerTask() {
			public void run() {
				// public synchronized void onSensorChanged(SensorEvent
				// sensorEvent) {

				// get rid the oldest sample in history:
				if (pitchHistorySeries.size() > HISTORY_SIZE) {

					azimuthHistorySeries.removeFirst();

					pitchHistorySeries.removeFirst();

					rollHistorySeries.removeFirst();

				}

				int j=0;
				// add the latest history sample:
				if (indicator == 2) {
					pitchHistorySeries.addLast(null, LowerBound - 10);
					azimuthHistorySeries.addLast(null, list.get(i));

					rollHistorySeries.addLast(null, list1.get(i));
				} else {
					int random = (int) (Math.random() * 2);
					pitchHistorySeries.addLast(null, list2.get(j) + random);
					random = (int) (Math.random() * 2);
					azimuthHistorySeries.addLast(null, list3.get(j) + random);
					random = (int) (Math.random() * 2);
					rollHistorySeries.addLast(null, list4.get(j) + random);
				}

				// rollHistorySeries.addLast(null, sensorEvent.values[2]);
				i++;
				j++;
				if (i >= 2999)
					i = 0;
				if (j >= 60)
					j = 0;
				// redraw the Plots:

				aprHistoryPlot.redraw();
			}

		};
		final Timer ts = new Timer();
		ts.scheduleAtFixedRate(t, 0, 25);
		Timer ts1 = new Timer();
		ts1.schedule(new TimerTask() {
			public void run() {
				ts.cancel();
			}
		}, 60 * 1000);

	}

	// =====================================================


	TimerTask t2 = new TimerTask() {
		public void run() {
			runOnUiThread(new Runnable() {

				public void run() {
					if (second == 30)
						timer.cancel();

					// display = (TextView) findViewById(R.id.textView1);
					display.setText("Recording... \n" + (30-second));
					second++;
				}

			});
		}
	};

	private void startCountdown() {
		second = 0;
		timer.scheduleAtFixedRate(t2, 0, 1000);
	}

}