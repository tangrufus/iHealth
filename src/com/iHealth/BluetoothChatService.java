package com.iHealth;

import java.io.*;
import java.sql.Date;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import com.iHealth.R;

/**
 * This class does all the work for setting up and managing Bluetooth
 * connections with other devices. It has a thread that listens for
 * incoming connections, a thread for connecting with a device, and a
 * thread for performing data transmissions when connected.
 */
public class BluetoothChatService {
	// Debugging
	private static final String TAG = "BluetoothChatService";
	private static final boolean D = true;

	// Name for the SDP record when creating server socket
	private static final String NAME_SECURE = "BluetoothChatSecure";
	private static final String NAME_INSECURE = "BluetoothChatInsecure";

	// Unique UUID for this application
	private static final UUID MY_UUID_SECURE =
			UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	//    private static final UUID MY_UUIDf_INSECURE =
	//        UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");

	// Member fields
	private final BluetoothAdapter mAdapter;
	private final Handler mHandler;
	private ConnectThread mConnectThread;
	private ConnectedThread mConnectedThread;
	private int mState;
	private DataOutputStream doutECG, doutPPG, doutDecode, doutCecg, doutCppg;
	private InputStreamReader ir;

	// Constants that indicate the current connection state
	public static final int STATE_NONE = 0;       // we're doing nothing
	public static final int STATE_LISTEN = 1;     // now listening for incoming connections
	public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
	public static final int STATE_CONNECTED = 3;  // now connected to a remote device
	public static Boolean Records = false;
	public static String Filename = "";
	public int samplesize = 8;
	public static SvaeData saveData = null;
	/**
	 * Constructor. Prepares a new BluetoothChat session.
	 * @param context  The UI Activity Context
	 * @param handler  A Handler to send messages back to the UI Activity
	 */
	public BluetoothChatService(Context context, Handler handler, PipedOutputStream outecg, PipedOutputStream outppg,PipedOutputStream outCecg, PipedOutputStream outCppg ) {
		mAdapter = BluetoothAdapter.getDefaultAdapter();
		mState = STATE_NONE;
		mHandler = handler;
		doutECG = new DataOutputStream(outecg);
		doutPPG = new DataOutputStream(outppg);
		doutCecg = new DataOutputStream(outCecg);
		doutCppg = new DataOutputStream(outCppg);
		saveData = new SvaeData();
		
//		doutDecode = new DataOutputStream(outDecode);
	}

	/**
	 * Set the current state of the chat connection
	 * @param state  An integer defining the current connection state
	 */
	private synchronized void setState(int state) {
		if (D) Log.d(TAG, "setState() " + mState + " -> " + state);
		mState = state;

		// Give the new state to the Handler so the UI Activity can update
		//        mHandler.obtainMessage(BLTActivity.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
	}

	/**
	 * Return the current connection state. */
	public synchronized int getState() {
		return mState;
	}

	/**
	 * Start the chat service. Specifically start AcceptThread to begin a
	 * session in listening (server) mode. Called by the Activity onResume() */
	public synchronized void start() {
		if (D) Log.d(TAG, "start");

		// Cancel any thread attempting to make a connection
		if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

		// Cancel any thread currently running a connection
		if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

		setState(STATE_LISTEN);


	}

	/**
	 * Start the ConnectThread to initiate a connection to a remote device.
	 * @param device  The BluetoothDevice to connect
	 * @param secure Socket Security type - Secure (true) , Insecure (false)
	 */
	public synchronized void connect(BluetoothDevice device) {
		if (D) Log.d(TAG, "connect to: " + device);

		// Cancel any thread attempting to make a connection
		if (mState == STATE_CONNECTING) {
			if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
		}

		// Cancel any thread currently running a connection
		if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

		// Start the thread to connect with the given device
		mConnectThread = new ConnectThread(device);
		mConnectThread.start();
		setState(STATE_CONNECTING);
	}

	/**
	 * Start the ConnectedThread to begin managing a Bluetooth connection
	 * @param socket  The BluetoothSocket on which the connection was made
	 * @param device  The BluetoothDevice that has been connected
	 */
	public synchronized void connected(BluetoothSocket socket, BluetoothDevice
			device, final String socketType) {
		if (D) Log.d(TAG, "connected, Socket Type:" + socketType);

		// Cancel the thread that completed the connection
		if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

		// Cancel any thread currently running a connection
		if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}



		// Start the thread to manage the connection and perform transmissions
		mConnectedThread = new ConnectedThread(socket, socketType);
		mConnectedThread.start();

		// Send the name of the connected device back to the UI Activity
		//        Message msg = mHandler.obtainMessage(BluetoothChat.MESSAGE_DEVICE_NAME);
		//        Bundle bundle = new Bundle();
		//        bundle.putString(BluetoothChat.DEVICE_NAME, device.getName());
		//        msg.setData(bundle);
		//        mHandler.sendMessage(msg);

		setState(STATE_CONNECTED);
	}

	/**
	 * Stop all threads
	 */
	public synchronized void stop() {
		if (D) Log.d(TAG, "stop");

		if (mConnectThread != null) {
			mConnectThread.cancel();
			mConnectThread = null;
		}

		if (mConnectedThread != null) {
			mConnectedThread.cancel();
			mConnectedThread = null;
		}


		setState(STATE_NONE);
	}



	/**
	 * Indicate that the connection attempt failed and notify the UI Activity.
	 */
	private void connectionFailed() {
		// Send a failure message back to the Activity
		//        Message msg = mHandler.obtainMessage(BluetoothChat.MESSAGE_TOAST);
		//        Bundle bundle = new Bundle();
		//        bundle.putString(BluetoothChat.TOAST, "Unable to connect device");
		//        msg.setData(bundle);
		//        mHandler.sendMessage(msg);

		// Start the service over to restart listening mode
		BluetoothChatService.this.start();
	}

	/**
	 * Indicate that the connection was lost and notify the UI Activity.
	 */
	private void connectionLost() {
		// Send a failure message back to the Activity
		//        Message msg = mHandler.obtainMessage(BluetoothChat.MESSAGE_TOAST);
		//        Bundle bundle = new Bundle();
		//        bundle.putString(BluetoothChat.TOAST, "Device connection was lost");
		//        msg.setData(bundle);
		//        mHandler.sendMessage(msg);

		// Start the service over to restart listening mode
		BluetoothChatService.this.start();
	}




	/**
	 * This thread runs while attempting to make an outgoing connection
	 * with a device. It runs straight through; the connection either
	 * succeeds or fails.
	 */
	private class ConnectThread extends Thread {
		private final BluetoothSocket mmSocket;
		private final BluetoothDevice mmDevice;
		private String mSocketType;

		public ConnectThread(BluetoothDevice device) {
			mmDevice = device;
			BluetoothSocket tmp = null;
			//            mSocketType = secure ? "Secure" : "Insecure";

			// Get a BluetoothSocket for a connection with the
			// given BluetoothDevice
			try {

				tmp = device.createRfcommSocketToServiceRecord(
						MY_UUID_SECURE);

			} catch (IOException e) {
				Log.e(TAG, "Socket Type: " + mSocketType + "create() failed", e);
			}
			mmSocket = tmp;
		}

		public void run() {
			Log.i(TAG, "BEGIN mConnectThread SocketType:" + mSocketType);
			setName("ConnectThread" + mSocketType);

			// Always cancel discovery because it will slow down a connection
			mAdapter.cancelDiscovery();

			// Make a connection to the BluetoothSocket
			try {
				// This is a blocking call and will only return on a
				// successful connection or an exception
				mmSocket.connect();
				int j = 6;
			} catch (IOException e) {
				// Close the socket
				try {
					mmSocket.close();
				} catch (IOException e2) {
					Log.e(TAG, "unable to close() " + mSocketType +
							" socket during connection failure", e2);
				}
				connectionFailed();
				return;
			}

			// Reset the ConnectThread because we're done
			synchronized (BluetoothChatService.this) {
				mConnectThread = null;
			}

			// Start the connected thread
			connected(mmSocket, mmDevice, mSocketType);
		}

		public void cancel() {
			try {
				mmSocket.close();
			} catch (IOException e) {
				Log.e(TAG, "close() of connect " + mSocketType + " socket failed", e);
			}
		}
	}

	/**
	 * This thread runs during a connection with a remote device.
	 * It handles all incoming and outgoing transmissions.
	 */
	private class ConnectedThread extends Thread {
		private final BluetoothSocket mmSocket;
		private final InputStream mmInStream;
		//        private final OutputStream mmOutStream;

		public ConnectedThread(BluetoothSocket socket, String socketType) {
			Log.d(TAG, "create ConnectedThread: " + socketType);
			mmSocket = socket;
			InputStream tmpIn = null;


			// Get the BluetoothSocket input and output streams
			try {
				tmpIn = socket.getInputStream();

			} catch (IOException e) {
				Log.e(TAG, "temp sockets not created", e);
			}

			mmInStream = tmpIn;
			//            mmOutStream = tmpOut;
			
		}
		byte[] buffer = new byte[300];
		int bytes;
		
		Filter filtecg = new Filter(), filtppg= new Filter();
		public void run() {
			Log.i(TAG, "BEGIN mConnectedThread");
//			byte[] buffer = new byte[10];
//			char[] buffer = new char[1024];
//			int bytes;
			
			// Keep listening to the InputStream while connected
			while (true) {
				try {
					// Read from the InputStream

					
					CheckPackage(mmInStream.read());
//					doutDecode.writeInt(mmInStream.read());
//					bytes =  mmInStream.read(buffer);
//						for(int i =0; i<bytes;i++){
//						int n =0;	
//						n = buffer[i] & 0xff;
//						CheckPackage(n);
//						}
//			
// 					 mmInStream.read(buffer);
// 					 for(byte b:buffer)
// 					 {
// 						 if((int)b!=-1)
// 						 {
// 							 CheckPackage((int)b);
// 						 }
// 						 else
// 						 {
// 							 int a;
// 							 a= b;
// 						 }
// 					 }
 					 
//					CheckPackage(buffer);
					
					
					// 					send the bytes out and verify

					// Send the obtained bytes to the UI Activity
//					mHandler.obtainMessage(IHealthActivity.MESSAGE_READ,bytes,-1, buffer)
//					.sendToTarget();
				} catch (IOException e) {
					Log.e(TAG, "disconnected", e);
					connectionLost();
					// Start the service over to restart listening mode
					BluetoothChatService.this.start();
					break;
				}
			}
		}

		/**
		 * Write to the connected OutStream.
		 * @param buffer  The bytes to write
		 */

		public void cancel() {
			try {
				mmSocket.close();
			} catch (IOException e) {
				Log.e(TAG, "close() of connect socket failed", e);
			}
		}
		public DataQueue DQ = new DataQueue(11);
		public int[] decodedData = new int[2];
		int header;
		public synchronized void CheckPackage(int b)
		{

			
			DQ.push(b);
//			c++;
//			record.push(b);
//			if(c==200)
//			{
//				c=0;
//			}
			header = DQ.getHead();
			if( (header & 0x80) == 0x80)
			{
				decode();
				
				
			}
		}
		int c=0;
		int count = 0;
		int checksum;
		DataQueue record = new DataQueue(200);
		
		public void decode()
		{
//			short[] d = new short[5];	
			for(int i = 1;i< DQ.queue.length-1;i++)
			{
				checksum+=DQ.queue[i];
			}
			checksum = (0xff & checksum);
			checksum =  (0xff - checksum);
			if( checksum == DQ.queue[DQ.queue.length-1]){
			decodedData[0] = ((DQ.queue[1] & 0x7F) << 3) +((DQ.queue[2] & 0x70) >> 4);   
			decodedData[1] = ((DQ.queue[2] & 0x07) << 7) + (DQ.queue[3] & 0x7F);
			decodedData[1]= 1024-decodedData[1];
			DQ.Clear();
			count++;
			
			try {
				doutCecg.writeInt(filtecg.Filt(decodedData[0]));
				doutCppg.writeInt(filtppg.Filt(decodedData[1]));
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			if(count% samplesize==0){
			try {
				doutECG.writeInt(decodedData[0]);
				doutPPG.writeInt(decodedData[1]);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}}
			checksum = 0;}
			
			if(Records)
			{
				saveData.WriteTxt(decodedData[0]+","+decodedData[1]+"\r\n" );
			}

		}
		
		
	}
	public void Changesamplesize(int size)
	{
		samplesize = size;
	}
	
}
