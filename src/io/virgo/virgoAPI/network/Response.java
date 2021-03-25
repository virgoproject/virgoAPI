package io.virgo.virgoAPI.network;

import org.json.JSONObject;

public class Response {

	private ResponseCode respCode;
	private String response = null;
	
	public Response(ResponseCode badRespCode) {
		this.respCode = badRespCode;
	}

	public Response(ResponseCode respCode, String response) {
		this.respCode = respCode;
		this.response = response;
	}
	
	/**
	 * @return Message response code
	 */
	public ResponseCode getResponseCode() {
		return respCode;
	}
	
	/**
	 * @return Message response as a {@link JSONObject}
	 */
	public String getResponse() {
		return response;
	}
	
}