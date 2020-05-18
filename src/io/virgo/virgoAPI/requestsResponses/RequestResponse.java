package io.virgo.virgoAPI.requestsResponses;

import io.virgo.geoWeb.ResponseCode;

public abstract class RequestResponse {

	private RequestType type;
	private ResponseCode code;
	
	public RequestResponse(RequestType type, ResponseCode code) {
		this.type = type;
		this.code = code;
	}
	
	public RequestType getType() {
		return type;
	}
	
	public ResponseCode getResponseCode() {
		return code;
	}
	
}
