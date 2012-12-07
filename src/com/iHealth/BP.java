package com.iHealth;

import com.iHealth.R;
import java.math.*;
class BP {
    
	Boolean	bGoodPeak;
	double	RT0, FT0, PTT0;
	double	k1, k2, k3;
	double	As, Bs, Cs, Ad, Bd, Cd, D;
	public double	rt, ft, ptt, m_nFT,m_nRT,m_nPTT;
	public int		nSBP, nDBP, m_nSBP0,m_nDBP0 ;
	DoubleQ m_SBPQueue	= new DoubleQ(10);
	DoubleQ m_DBPQueue	= new DoubleQ(10);
	DoubleQ m_RT0Queue	= new DoubleQ(10);
	DoubleQ m_FT0Queue	= new DoubleQ(10);
	DoubleQ m_PTT0Queue	= new DoubleQ(10);
	DoubleQ m_PTTAQueue	= new DoubleQ(10);

	public BP(int SBP0,int DBP0)
	{
		m_nSBP0 = SBP0;
		m_nDBP0 = DBP0;

	}

	Boolean CalcTimes(ECG ecg, PPG ppg)
	{


		if(ecg.PeaksOK() && ppg.PeaksOK()){
			int[]	PeakPPG = ppg.ppgPeak;
			int[]	PeakECG = ecg.ecgPeak;
			int		nFoot, nRight, nR, nPrevRight;
			Boolean	bFound = false;
			int nDelay = 0;
			nR = nFoot = nRight = nPrevRight = -1;
			for (int i = 0; i < ecg.buffersize; i++)
			{
				if (PeakPPG[i] == 4)
					nFoot = i;
				else if (nFoot > 0 && PeakPPG[i] == 6)
					nRight = i;

				if (nFoot >= 0 && nRight > 0 && nRight > nFoot)	// an unprocessed PPG pulse found
				{
					PeakPPG[nFoot]	 = 3;
					PeakPPG[nRight] = 5;

					for (int n = nFoot; n >= 0; n--)	// Looking for the previous nRight to detemine m_nFT
					{
						if (PeakPPG[n] == 5 || PeakPPG[n] == 6)
						{
							nPrevRight = n;
							break;
						}
					}

					if (nPrevRight >= 0)
					{
						m_nFT = (nFoot - nPrevRight) * 1000 / ecg.samplesize;
					}

					for (int n = nFoot; n >= 0; n--)	// Looking for the corresponding R-peak
					{
						if (PeakECG[n] == 1 || PeakECG[n] == 2)
						{
							nR = n;
							break;
						}
					}

					if (nR >= 0)
					{
						m_nRT	= (nRight - nFoot) * 1000 / ecg.samplesize;
						m_nPTT	= (nFoot - nR - nDelay) * 1000 /ecg.samplesize;
					}

					bFound = true;
					nFoot  = nRight = nR = nPrevRight = -1;
				}
			}

			return bFound;
		}
		else{return false;}
	}
	public void CalcBP()
	{
		// because m_nRT, m_nFT, m_nPTT are in millisecond
		rt	= m_nRT / 1000.0;
		ft	= m_nFT / 1000.0;
		ptt	= m_nPTT / 1000.0;

		bGoodPeak = 
				(rt > 0.1 && rt < 0.8) &&
				(ft > 0.1 && ft < 2.0) &&
				(ptt > 0.05 && ptt < 0.5);

		if (bGoodPeak)
		{
			if (!m_RT0Queue.IsFull())
				m_RT0Queue.push(m_nRT);

			if (!m_FT0Queue.IsFull())
				m_FT0Queue.push(m_nFT);

			if (!m_PTT0Queue.IsFull())
				m_PTT0Queue.push(m_nPTT);

			if (m_nPTT > 100 && m_nPTT < 650)
				m_PTTAQueue.push(m_nPTT);

			RT0		= m_RT0Queue.GetMean() / 1000.0;
			FT0		= m_FT0Queue.GetMean() / 1000.0;
			PTT0	= m_PTT0Queue.GetMean() / 1000.0;

			k1	= 0.5;
			k2	= -1;
			k3	= 0.5;
			D	= 0;

			As	= k1 * m_nSBP0 * (PTT0 + D) / Math.sqrt(RT0);
			Bs	= k2 * FT0;
			Cs	= k3 * m_nSBP0;

			Ad	= k1 * m_nDBP0 * (PTT0 + D) / Math.sqrt(RT0);
			Bd	= k2 * FT0;
			Cd	= k3 * m_nDBP0;

			nSBP = (int)(As * Math.sqrt(rt) / (ptt + D) + Bs / ft + Cs);
			nDBP = (int)(Ad * Math.sqrt(rt) / (ptt + D) + Bd / ft + Cd);

			if ((nSBP >= 60) && (nSBP <= 160))
			{
				m_SBPQueue.push(nSBP);

				if (nDBP >= 50 && nDBP <= 130)
				{
					m_DBPQueue.push(nDBP);
				}
			}

		}
	}
	
	void Reset()
	{
		m_RT0Queue.Clear();
		m_FT0Queue.Clear();
		m_PTT0Queue.Clear();
		m_PTTAQueue.Clear();

		m_SBPQueue.Clear();
		m_DBPQueue.Clear();
	}
	void Calibrate()
	{
		Reset();
		m_SBPQueue.push(m_nSBP0);
		m_DBPQueue.push(m_nDBP0);
	}

}

