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
	public static void access(String filename) {
		try {
			Class.forName("com.mysql.jdbc.Driver");
			Connection connection = null;
			connection = (Connection) DriverManager.getConnection(
					"jdbc:mysql://172.16.0.102:3306/iHealth", "root", "root");
			// Statement st = (Statement) connection.createStatement();
			String path = Environment.getExternalStorageDirectory()
					.getAbsolutePath();
			File file = new File(Environment.getExternalStorageDirectory(),
					"/IHealthRecord/" + filename + ".csv");

			InputStream is = null;

			is = new BufferedInputStream(new FileInputStream(file));

			BufferedReader reader = new BufferedReader(
					new InputStreamReader(is));

			String line = reader.readLine();
			int i = 0;
			while ((line = reader.readLine()) != null) {
				// do something with "line"

				String[] RowData = line.split(",");
				String sql = "insert into raws (uid, sequence, ecg, ppg) values ('" + filename + "',"
						+ i++ + "," + RowData[0] + "," + RowData[1] + "); ";

				Log.d("MAIN__", sql);
				PreparedStatement ps = (PreparedStatement) connection
						.prepareStatement(sql);
				ps.executeUpdate();

			}

			((PreparedStatement) connection
					.prepareStatement("insert into results (uid, sbpv, dbpv, hrv) values ('" + filename
							+ "'," + (int) (Math.random()*100) + "," + 110 + "," + 91 + ");"))
					.execute();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}
