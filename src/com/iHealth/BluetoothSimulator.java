package com.iHealth;

import java.io.*;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

public class BluetoothSimulator implements Runnable{

	// Member fields
	private final Handler mHandler;
	private int mState;
	private DataOutputStream doutECG, doutPPG, doutDecode, doutCecg, doutCppg;
	private InputStreamReader ir;
	
	public static Boolean Records = false;
	public static String Filename = "";
	public int samplesize = 8;
	public static SaveData saveData = null;
	
	Filter filtecg = new Filter(), filtppg= new Filter();
	
	Context mcontext = null; 

	public BluetoothSimulator(Context context, Handler handler, PipedOutputStream outecg, PipedOutputStream outppg,PipedOutputStream outCecg, PipedOutputStream outCppg)
	{
		mcontext = context;
		mHandler = handler;
		doutECG = new DataOutputStream(outecg);
		doutPPG = new DataOutputStream(outppg);
		doutCecg = new DataOutputStream(outCecg);
		doutCppg = new DataOutputStream(outCppg);
		saveData = new SaveData();
	}
	
	public void Changesamplesize(int size)
	{
		samplesize = size;
	}

	public void run()
	{
		Log.d("BLUSIM", "bluetooth simulator started!!!!!");
		int[] decodedData = new int[10];
		int count = 0;
		decodedData[0] = 300;
		decodedData[1] = 450;
		
		try{
			  // Open the file that is the first 
			  // command line parameter
			  InputStream fstream = mcontext.getAssets().open("ecg_ppg_signal.csv");
			  // Get the object of DataInputStream
			  DataInputStream in = new DataInputStream(fstream);
			  BufferedReader br = new BufferedReader(new InputStreamReader(in));
			  String strLine;
			  //Read File Line By Line
			  Log.d("BLUSIM", "decode data sim!!!!!");
			  while ((strLine = br.readLine()) != null)   {
			  // Print the content on the console
				  //piped decoded data
				  String[] strArray = strLine.split(",");
				  for(int i = 0; i < strArray.length; i++) {
					  decodedData[i] = Integer.parseInt(strArray[i]);
				  }
				  
//					try {
//						doutCecg.writeInt(filtecg.Filt(decodedData[0]));
//						doutCppg.writeInt(filtppg.Filt(decodedData[1]));
//					} catch (IOException e1) {
//						// TODO Auto-generated catch block
//						e1.printStackTrace();
//					}
					
//					//if(count% samplesize==0){
//						//try {
//							//doutECG.writeInt(decodedData[0]);
//							//doutPPG.writeInt(decodedData[1]);
//						} catch (IOException e) {
//							// TODO Auto-generated catch block
//							e.printStackTrace();
//						}
//					}
					
					if(Records)
					{
						Log.d("Blt Sim__", "Recoeds == true  " + count);
						saveData.WriteTxt(decodedData[0]+","+decodedData[1]+"\r\n" );
					}
					count++;  
				 
					//System.out.println (decodedData[0]);
					//System.out.println (decodedData[1]);
			  	}
			  	//Close the input stream
			  	in.close();
			  } catch (Exception e){//Catch exception if any
				  Log.e("BLUSIM", "can't read csv file!!!!!");
				  Log.e("BLUSIM", "Error: " + e.getMessage());
				  System.out.println ("can't read csv file");
				  System.err.println("Error: " + e.getMessage());
			  }//end of while loop

	}
}