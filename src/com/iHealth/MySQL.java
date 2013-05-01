package com.iHealth;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.DriverManager;

import android.os.Environment;
import android.util.Log;

import com.mysql.jdbc.Connection;
import com.mysql.jdbc.PreparedStatement;

public class MySQL {
	public static void access(String filename, String uid, int sbpv, int dbpv, int hrv) {
		try {
			Class.forName("com.mysql.jdbc.Driver");
			Connection connection = null;
			connection = (Connection) DriverManager.getConnection(
					"jdbc:mysql://172.16.0.102:3306/iHealth", "root", "root");
			File file = new File(Environment.getExternalStorageDirectory(),
					"/IHealthRecord/" + filename + ".csv");

			InputStream is = null;

			is = new BufferedInputStream(new FileInputStream(file));

			BufferedReader reader = new BufferedReader(
					new InputStreamReader(is));

			String line = reader.readLine();
			int i = 0;
			String sql = "insert delayed into raws (uid, sequence, ecg, ppg) values (?,?,?,?); ";
			String[] RowData = new String[2];
			PreparedStatement ps = (PreparedStatement) connection
					.prepareStatement(sql);

			while ((line = reader.readLine()) != null) {
				// do something with "line"

				RowData = line.split(",");
				ps.setString(1, uid);
				ps.setInt(2, i++);
				ps.setInt(3, Integer.parseInt(RowData[0]));
				ps.setInt(4, Integer.parseInt(RowData[1]));
				ps.addBatch();
				
				if (((i % 1000) == 0)) {
					ps.executeBatch();
					ps.clearBatch();
					Log.d("MySQL", "Added Batch " + i);
				}
				
				
			}
			reader.close();
			ps.executeBatch();

			((PreparedStatement) connection
					.prepareStatement("insert into results (uid, sbpv, dbpv, hrv) values ('"
							+ uid
							+ "',"
							+ sbpv
							+ "," + dbpv + "," + hrv + ");")).execute();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	//====================
	public static void access(String filename, String uid, int s, int sbpv, int dbpv, int hrv, int simMode) {
		try {
			Class.forName("com.mysql.jdbc.Driver");
			Connection connection = null;
			connection = (Connection) DriverManager.getConnection(
					"jdbc:mysql://172.16.0.102:3306/iHealth", "root", "root");
		
			((PreparedStatement) connection
					.prepareStatement("insert into results (uid, calc, time, sbpv, dbpv, hrv, mode) values ('"
							+ uid
							+ "',"
							+ 0
							+ "," 
							+ s 
							+ "," 
							+ sbpv
							+ "," 
							+ dbpv 
							+ "," 
							+ hrv 
							+ "," 
							+ simMode
							+ ");")).execute();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
