package com.iHealth;

import com.iHealth.R;

class HR {
    Boolean ECGOK = false, PPGOK=false;
	DoubleQ m_ECGHRQueue = new DoubleQ(10), m_PPGHRQueue= new DoubleQ(10);
	public int HRV;
	public void CalcHR_ECG(ECG ecg)
	{
		ECGOK = false;
		if(ecg.PeaksOK())
		{
			int		hr, nLastPeak = -1;
			int	peak;

			for (int i = 0; i < ecg.buffersize ; i++)
			{
				peak =ecg.GetPeakAt(i);
				if (1 == peak)
					nLastPeak = i;
				else if (2 == peak)
				{
					if (nLastPeak >= 0)
					{
						hr = (int)(((double)(ecg.samplesize * 60) / (i - nLastPeak)) + 0.5); 
//					 System.out.println("HR:" + hr);	
												if (hr>=40 && hr<=160)
													m_ECGHRQueue.push(hr);
					}
					nLastPeak = i;
				}
			}

			ECGOK = true;
			if(m_ECGHRQueue.IsFull()){
			HRV = (int)m_ECGHRQueue.GetMean();}
		}
                /*
                if (ECGOK)
                    System.out.println("ECGOK");
                else
                    System.out.println("ECG_NOT_OK"); */
	}

	public void CalcHR_PPG(PPG ppg)
	{
		PPGOK = false;


		if (ppg.PeaksOK())
		{
			int		hr, nLastPeak = -1;
			int	peak;

			for (int i = 0; i < ppg.buffersize; i++)
			{
				peak = ppg.GetPeakAt(i);
				if (1 == peak)
					nLastPeak = i;
				else if (2 == peak)
				{
					if (nLastPeak >= 0)
					{
						hr = (int)(((double)(ppg.samplesize * 60) / (i - nLastPeak)) + 0.5); 
						
						if (hr>=40 && hr<=160)
							m_PPGHRQueue.push(hr);
					}
					nLastPeak = i;
				}
			}

			PPGOK = true;
			if(m_PPGHRQueue.IsFull()){
			HRV = (int)m_PPGHRQueue.GetMean(); 
		}}
	}
	public void CalcHR_RawPPG(PPG ppg)
	{
		PPGOK = false;


		if (ppg.PeaksOK())
		{
			int		hr, nLastPeak = -1;
			int	peak;

			for (int i = 0; i < ppg.buffersize; i++)
			{
				peak = ppg.GetPeakAt(i);
				if (11 == peak)
					nLastPeak = i;
				else if (10 == peak)
				{
					if (nLastPeak >= 0)
					{
						hr = (int)(((double)(ppg.samplesize * 60) / (i - nLastPeak)) + 0.5); 
						
						if (hr>=40 && hr<=160)
							m_PPGHRQueue.push(hr);
					}
					nLastPeak = i;
				}
			}

			PPGOK = true;
			HRV = (int)m_PPGHRQueue.GetMean();
		}
	}
    
}

