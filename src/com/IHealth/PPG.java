package com.iHealth;

import com.iHealth.R;

public class PPG {

    int[] ppg, ppgPeak;
    double[] ppgd1;
    int samplesize = 500, stIndx = 0, buffersize;
    int lastPeak;
    DataQueue buffer;
    MathTools mathtool = new MathTools();
    int nGap = 3 * samplesize / 10, sigLen, nPart, nA, nB, nPos = 0;
    double fMeanMax, fMeanMin, thresholdMax, threasholdMin, fPrevA, fMean, nMax, fThresholdU;
    int FILTER_LEN = 10, m_nValidBufSize = 0;
    Boolean m_bPPeakExist, FootOK = false, RightOK = false;
    int[] zeromem = new int[samplesize];

    public PPG() {
        buffersize = samplesize * 4;
        ppg = new int[buffersize];
        ppgd1 = new double[buffersize];
        buffer = new DataQueue(samplesize);
        ppgPeak = new int[buffersize];
        lastPeak = buffersize - 1;
        m_bPPeakExist = false;
        m_nValidBufSize = 0;
    }

//	public void push(int i)
//	{
//		
//		buffer.push(i);
//		if(buffer.IsFull())
//		{
//			pushbulk(buffer.queue);
//			DetectPeaks();
//		}
//	}
    public void pushbulk(int[] buffer) {
        System.arraycopy(ppg, samplesize, ppg, 0, samplesize * 3);
        System.arraycopy(ppgd1, samplesize, ppgd1, 0, samplesize * 3);
        System.arraycopy(ppgPeak, samplesize, ppgPeak, 0, samplesize * 3);
        System.arraycopy(buffer, 0, ppg, samplesize * 3, samplesize);
        System.arraycopy(zeromem, 0, ppgPeak, samplesize * 3, samplesize);
//        FirstD();
        FirstDSlope();
        if (m_nValidBufSize < 4) {
            m_nValidBufSize++;
        }

        lastPeak -= samplesize;

        if (lastPeak < 0) {
            lastPeak = 0;
        }


    }

    public void FirstD() {

        for (int i = samplesize * 3; i < buffersize - 3; i++) {
            ppgd1[i] = (-ppg[i + 2] + 8 * ppg[i + 1] - 8 * ppg[i - 1] + ppg[i - 2]) / 12.0;
//            ecgd1[i] = mathtool.GetSlope(ecg, i, 20);
//            System.out.println(ppgd1[i] +" "+ppg[i]);

        }
    }

    public void FirstDSlope() {
        for (int i = samplesize * 3; i < buffersize - 11; i++) {
            ppgd1[i] = mathtool.GetSlope(ppg, i, 20);
//            ecgd1[i] = mathtool.GetSlope(ecg, i, 20);
//            System.out.println(ecgd1[i] +" "+ecg[i]);

        }
    }
    int nParts;
    
    public void DetectDPeaks() {
        try {
            stIndx = lastPeak >= 0 ? lastPeak + nGap : 0;
            sigLen = buffersize - stIndx;
            fMean = mathtool.GetMean(ppg, sigLen, stIndx);
            nMax = mathtool.GetMaxV(ppg, sigLen, stIndx);
            int nMin = mathtool.GetMin(ppg, sigLen, stIndx);
            nPos = mathtool.GetMax(ppg, sigLen, stIndx);
            int maxpos = nPos;
            fThresholdU = (int) (fMean + (nMax - fMean) * 0.65);
            m_bPPeakExist = false;
            nA = nB = -1;
            Boolean setpeak = false;
            double per = mathtool.GetGreatThanPercent(ppg, (buffersize - stIndx + 1), fThresholdU, stIndx);
            if (per < 15.0 && !((nMax-nMin)<=(1024/5)*0.25)) {
                for (int i = stIndx + 1; i < buffersize - 1; i++) {
                    if (mathtool.IsRisingEdgeCross(ppg, i, fThresholdU)) {

                        nA = i;
                    }
                    if (nA > 0 && mathtool.IsFallingEdgeCross(ppg, i, fThresholdU)) {

                        nB = i;
                    }

                    if (nA > 0 && nB > 0 && nA < nB && nB < buffersize) // if both A & B found
                    {
                        if (fPrevA * 0.5 < ppg[nA]) {
                            nPos = mathtool.GetMax(ppg, nB - nA + 1, nA);	// Get back to Raw ECG to locate the 
                            if (nA + nPos < buffersize) {
                                ppgPeak[nPos] = 2;					// mark down the peak in d1PPG
                                lastPeak = nPos;
                                fPrevA = ppg[nA];
                                m_bPPeakExist = true;
                                FootOK = DetectFoot_fromDPPG(nA);
                                RightOK = DetectRight_fromDPPG(nB);
                                nA = nB = -1;							// reset A & B to unfound
//                                if (!setpeak) {
//                                    ppgPeak[nPos] = 10;
//                                    setpeak = true;
//                                }
//                                if ((i + nGap) < buffersize) {
//                                    i += (nGap - 1);
//                                } else {
//                                    break;
//                                }
                            }
                        }
                    }
                }

            }
        } catch (Exception e) {
        }

    }

    public void DetectPeakFromRaw() {
        try {
            stIndx = lastPeak >= 0 ? lastPeak + nGap : 0;
            sigLen = buffersize - stIndx;
            fMean = mathtool.GetMean(ppgd1, sigLen, stIndx);
            nMax = mathtool.GetMaxV(ppgd1, sigLen, stIndx);
            nPos = mathtool.GetMax(ppgd1, sigLen, stIndx);
            int maxpos = nPos;
            Boolean setpeak = false;
            fThresholdU = nMax * 0.65;
            m_bPPeakExist = false;
            nA = nB = -1;
            double per = mathtool.GetGreatThanPercent(ppgd1, (buffersize - stIndx + 1), fThresholdU, stIndx);
            if (per < 20.0) {
                for (int i = stIndx + 1; i < buffersize - 1; i++) {
                    if (mathtool.IsRisingEdgeCross(ppgd1, i, fThresholdU)) {

                        nA = i;
                    }
                    if (nA > 0 && mathtool.IsFallingEdgeCross(ppgd1, i, fThresholdU)) {

                        nB = i;
                    }

                    if (nA > 0 && nB > 0 && nA < nB && nB < buffersize) // if both A & B found
                    {
                        if (fPrevA * 0.5 < ppg[nA]) {
                            nPos = mathtool.GetMax(ppg, nB - nA + 1, nA);	// Get back to Raw ECG to locate the 
                            if (nPos < buffersize) {
                                ppgPeak[nPos] = 2;					// mark down the peak in d1PPG
                                lastPeak = nPos;
                                fPrevA = ppgd1[nA];
                                m_bPPeakExist = true;
//                                System.out.println("Peak: "+lastPeak + " nB:"+ nB);
                                FootOK = DetectFootRawPPG(nA);
                                RightOK = DetectRightRawPPG(nB);


                                nA = nB = -1;							// reset A & B to unfound
                                if (!setpeak) {
                                    ppgPeak[nPos] = 10;
                                    setpeak = true;
                                }
                                //                                if ((i + nGap) < buffersize) {
//                                    i += (nGap - 1);
//                                } else {
//                                    break;
//                                }
                            }
                        }
                    }
                }
//                ppgPeak[maxpos] = 10;


            }
        } catch (Exception e) {
        }



    }

    Boolean DetectFoot_fromDPPG(int nA) {
        int n = -1;

        while (mathtool.GetSlope(ppg, nA, 13) > 0 && nA > 6) {
            n = --nA;
        }

        if (n > 0) {
            ppgPeak[n] = 4;
            return true;
        } else {
            return false;
        }
    }

    Boolean DetectFootRawPPG(int nA) {
        int n = -1;

        while (mathtool.GetSlope(ppgd1, nA, 13) > 0 && nA > 6) {
            n = --nA;
        }

        if (n > 0) {
            ppgPeak[n] = 4;
//            System.out.println("Ft: "+n);
            return true;
        } else {
            return false;
        }
    }

    Boolean DetectRight_fromDPPG(int nB) {
        int n = -1;

        while (mathtool.GetSlope(ppg, nB, 13) < 0 && nB < buffersize - 6) {
            n = ++nB;
        }

        if (n > 0) {
            ppgPeak[n] = 6;
            return true;
        }

        return false;
    }

    Boolean DetectRightRawPPG(int nB) {
        int n = -1;

        while (mathtool.GetSlope(ppgd1, nB, 13) < 0 && nB < buffersize - 6) {
            n = ++nB;
//            n++;
        }

        if (n > 0) {
            ppgPeak[n] = 6;
//            System.out.println("R: "+n);
            return true;
        }

        return false;
    }

    int GetPeakAt(int i) {
        if (i >= 0 && i < buffersize) {
            return ppgPeak[i];
        } else {
            return -1;
        }
    }
    double hrsMax = 150.0 / 60.0;
    double hrsMin = 40.0 / 60.0;

    Boolean PeaksOK() {
        int nPeakCount = 0;

        for (int i = 0; i < buffersize; i++) {
//            if (ppgPeak[i] == 1 || ppgPeak[i] == 2) {
//                nPeakCount++;
////                System.out.println(i);
//            }
            if (ppgPeak[i] == 1 || ppgPeak[i] == 2) {
                nPeakCount++;
            }
        }



        if (0 == nPeakCount) {
            return false;
        }
//        System.out.println(nPeakCount);
        return (nPeakCount > hrsMin * m_nValidBufSize) && (nPeakCount < hrsMax * m_nValidBufSize);
    }

    void ResetNewPeaks() {
        if (null == ppgPeak) {
            return;
        }

        for (int i = 0; i < buffersize; i++) {
            if (ppgPeak[i] == 2) {
                ppgPeak[i] = 1;
            } else if (ppgPeak[i] == 4) {
                ppgPeak[i] = 3;
            } else if (ppgPeak[i] == 6) {
                ppgPeak[i] = 5;
            } else if (ppgPeak[i] == 10) {
                ppgPeak[i] = 11;
            }
        }
    }
}