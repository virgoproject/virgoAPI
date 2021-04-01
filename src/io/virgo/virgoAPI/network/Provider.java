package io.virgo.virgoAPI.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class Provider {

	String hostname;
	
	public Provider(String hostname) {
		this.hostname = hostname;
	}
	
	public Response get(String method) {
		try {
			URL url = new URL(hostname + method);
			HttpURLConnection connection = (HttpURLConnection)url.openConnection();
			connection.connect();
			
			if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
				BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));

	            String line;

	            StringBuilder sb = new StringBuilder();

	            while ((line = br.readLine()) != null) {
	                sb.append(line);
	                sb.append(System.lineSeparator());
	            }
	            
	            return new Response(ResponseCode.OK, sb.toString());
	            
			} else if(connection.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
	            return new Response(ResponseCode.NOT_FOUND, null);
			}
			
		} catch (MalformedURLException e) {
			return new Response(ResponseCode.BAD_REQUEST, null);
		} catch (IOException e) {
			return new Response(ResponseCode.REQUEST_TIMEOUT, null);
		}
		
		return new Response(ResponseCode.BAD_REQUEST, null);
	}
	
	public Response post(String method, String data) {
		try {
			URL url = new URL(hostname+method);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setFixedLengthStreamingMode(data.length());
			conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
			conn.setDoOutput(true);
			conn.connect();
			try(OutputStream os = conn.getOutputStream()) {
			    os.write(data.getBytes(StandardCharsets.UTF_8));
			}
			
			if(conn.getResponseCode() == HttpURLConnection.HTTP_OK)
	            return new Response(ResponseCode.OK, null);
			else if(conn.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND)
				return new Response(ResponseCode.NOT_FOUND, null);
			
			
		} catch (IOException e) {}
		
		return new Response(ResponseCode.BAD_REQUEST, null);
	}
	
}
