package com.iHealth;

import com.iHealth.R;

public class Filter {
	
	DataQueue filter = new DataQueue(10);
	
	public int Filt(int n)
	{
		filter.push(n);
		return filter.IsFull()? filter.GetMean():n;
	} 

}
