package com.interordi.iosync.utilities;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;

public class Http {
	
	//Read the content of a remote page
	public static String readUrl(String url) {
		URL realUrl;
		
		try {
			realUrl = new URL(url);
		} catch (MalformedURLException e) {
			System.out.println("MALFORMED URL: " + url);
			e.printStackTrace();
			return "";
		}
		
		return readUrl(realUrl);
	}
	
	
	//Read the content of a remote page
	public static String readUrl(URL url) {
		String content = "";
		BufferedReader in = null;
		try {
			HttpURLConnection connection = (HttpURLConnection)url.openConnection();
			connection.setRequestMethod("GET");
			connection.setConnectTimeout(5000);	//5 seconds
			connection.setReadTimeout(2000);	//2 seconds
			connection.connect();
			
			in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			String inputLine;
			while ((inputLine = in.readLine()) != null) {
				content += inputLine;
			}
			
			//int code = connection.getResponseCode();
		} catch (SocketTimeoutException e) {
			System.out.println("TIMEOUT reading remote page " + url.toString());
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("UNEXPECTED ERROR reading remote page " + url.toString());
			e.printStackTrace();
		} finally {
			try {
				if (in != null)
					in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		return content;
	}
}
