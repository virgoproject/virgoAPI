package io.virgo.virgoAPI.requestsResponses;

import io.virgo.geoWeb.ResponseCode;

/**
 * Base request response object, basically an ENUM telling the request type and a response codes
 */
public abstract class RequestResponse {

	private RequestType type;
	private ResponseCode code;
	
	public RequestResponse(RequestType type, ResponseCode code) {
		this.type = type;
		this.code = code;
	}
	
	/**
	 * @return The request type
	 */
	public RequestType getType() {
		return type;
	}
	
	/**
	 * @return The request response code
	 */
	public ResponseCode getResponseCode() {
		return code;
	}
	
}
