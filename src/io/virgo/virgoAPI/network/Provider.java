package io.virgo.virgoAPI.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class Provider {

	String hostname;
	
	Provider(String hostname) {
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
		return null;
	}
	
}
