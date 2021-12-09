package io.virgo.virgoAPI.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;

public class Provider {

	String hostname;
	
	public Provider(String hostname) {
		this.hostname = hostname;
	}
	
	/**
	 * Call a REST GET method and return it's result
	 * @param method The method to call
	 * @return the result
	 */
	public Response get(String method) {
		try {
			URL url = new URL(hostname + method);
			URLConnection con = url.openConnection();
			
			HttpURLConnection httpConnection = (HttpURLConnection)con;
			con.connect();
			
			if (httpConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
				BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));

	            String line;

	            StringBuilder sb = new StringBuilder();

	            while ((line = br.readLine()) != null) {
	                sb.append(line);
	                sb.append(System.lineSeparator());
	            }
	            
	            return new Response(ResponseCode.OK, sb.toString());
	            
			} else if(httpConnection.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
	            return new Response(ResponseCode.NOT_FOUND, null);
			}
			
		} catch (MalformedURLException e) {
			return new Response(ResponseCode.BAD_REQUEST, null);
		} catch (IOException e) {
			return new Response(ResponseCode.REQUEST_TIMEOUT, null);
		}
		
		return new Response(ResponseCode.BAD_REQUEST, null);
	}
	
	/**
	 * Call a REST POST method and return it's result
	 * @param method The method to call
	 * @param data The data to post
	 * @return the result
	 */
	public Response post(String method, String data) {
		try {
			URL url = new URL(hostname+method);
			URLConnection con = url.openConnection();
			HttpURLConnection httpConnection = (HttpURLConnection) con;
			httpConnection.setFixedLengthStreamingMode(data.length());
			con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
			con.setDoOutput(true);
			con.connect();
			try(OutputStream os = con.getOutputStream()) {
			    os.write(data.getBytes(StandardCharsets.UTF_8));
			}
			
			if(httpConnection.getResponseCode() == HttpURLConnection.HTTP_OK)
	            return new Response(ResponseCode.OK, null);
			else if(httpConnection.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND)
				return new Response(ResponseCode.NOT_FOUND, null);
			
			
		} catch (IOException e) {}
		
		return new Response(ResponseCode.BAD_REQUEST, null);
	}
	
}
