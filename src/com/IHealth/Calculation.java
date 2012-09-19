package com.iHealth;

import java.io.*;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import com.iHealth.R;

public class Calculation implements Runnable{


	int ppt=0 , SBP = 0, DBP=0, HR=0;
	//	DataQueue ecg , ppg;
	public PipedInputStream pinECG, pinPPG;
	public DataInputStream dinECG, dinPPG;
	Handler mhandler;
	ECG ecg;
	PPG ppg; 
	HR hr;
	BP bp;
	int SBP0,DBP0;
	int[] results = new int[4];


	public Calculation(Context context, Handler mhandler, int sbp0, int dbp0,int buffsize,PipedInputStream pinECG, PipedInputStream pinPPG )
	{

		dinECG = new DataInputStream(pinECG);
		dinPPG = new DataInputStream(pinPPG);
		this.mhandler = mhandler;
		SBP0=sbp0;
		DBP0 = dbp0;
		ecg= new ECG();  
		ppg = new PPG();

	}


	int ec=0,pp=0;
	int count=0, count2 =0;
	public void run()
	{
		hr = new HR();
		bp = new BP(SBP0,DBP0);
		bp.Calibrate();
		DataQueue ecgbuffer = new DataQueue(500), ppgbuffer = new DataQueue(500);
		while(true){
			try{
				ecgbuffer.push(dinECG.readInt());
				ppgbuffer.push(dinPPG.readInt());
			}catch(Exception e){};


			if(ecgbuffer.IsFull()&&ppgbuffer.IsFull())
			{ecg.pushbulk(ecgbuffer.queue);
			ppg.pushbulk(ppgbuffer.queue);
			ecg.DetectPeak();
			ppg.DetectDPeaks();
			//                ppg.DetectPeakFromRaw();
			ecgbuffer.Clear();
			ppgbuffer.Clear();


			try
			{
				hr.CalcHR_ECG(ecg);
				results[0] = hr.HRV;
			}
			catch(Exception e){}
			if(!hr.ECGOK)
			{
				
				hr.CalcHR_PPG(ppg);
				results[0] = hr.HRV;
				
				if(!hr.PPGOK)
				{
					results[0]=-1;
				}
			}
			
			if(hr.ECGOK)
			{
				try
				{
					if(bp.CalcTimes(ecg, ppg))					
					{bp.CalcBP();
					if(bp.m_SBPQueue.IsFull()&&bp.m_DBPQueue.IsFull())
					results[2]= (int)bp.m_SBPQueue.GetMean();
					results[3]= (int)bp.m_DBPQueue.GetMean();
					}else 
					{
						count++;
						if(count>5){
						results[2]= -1;
						results[3]=-1;
						count =0;
						}
					}


				}catch (Exception e) {
					// TODO: handle exception
				}
			}
			else
			{
				count2++;
				if(count2>4){
				results[2]= -1;
				results[3]=-1;count2=0;
				bp.Reset();
				bp.Calibrate();
				
				}
			}

			
			results[1]=(int)bp.ptt;

			ecg.ResetNewPeaks();
			ppg.ResetNewPeaks();
			mhandler.obtainMessage(IHealthActivity.resultHR, results).sendToTarget();
			}


		}
	}
}