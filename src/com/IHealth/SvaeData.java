package com.iHealth;
import java.io.*;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.view.*;
import android.view.View.OnClickListener;
import android.widget.*;
import com.iHealth.R;
public class SvaeData {
	File myFile;
	FileOutputStream fOut;
	OutputStreamWriter myOutWriter;


	public void WriteTxt( String text)
	{try{

		
		myOutWriter.append(text);

		//		Toast.makeText(getBaseContext(),
		//				"Done writing SD 'mysdfile.txt'",
		//				SToast.LENGTH_SHORT).show();
		//	} catch (Exception e) {
		//		Toast.makeText(getBaseContext(), e.getMessage(),
		//				Toast.LENGTH_SHORT).show();
	}catch(Exception e){}

	}	public void CreatFile(String FileName) throws IOException
	{		try{
			new File("/sdcard/IHealthRecord").mkdir();
		}
		
		catch(Exception e){}
	String str = Environment.getExternalStorageDirectory().toString();
		myFile= new File(Environment.getExternalStorageDirectory(),"/IHealthRecord/"+FileName+".txt");
		
	
		fOut = new FileOutputStream(myFile);
		myOutWriter =new OutputStreamWriter(fOut);

	}
	public void EndSave() 
	{try{
				myOutWriter.close();
		fOut.close();}catch(Exception e){}
	}
	}
