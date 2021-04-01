package io.virgo.virgoAPI.requestsResponses;

import java.util.ArrayList;

import io.virgo.virgoAPI.network.ResponseCode;

/**
 * Object representing the response to a GetTips request
 */
public class GetTipsResponse extends RequestResponse {

	private ArrayList<String> tips;
	
	public GetTipsResponse(ResponseCode responseCode, ArrayList<String> tips) {
		super(RequestType.GET_TIPS, responseCode);
		
		this.tips = tips;
	}
	
	/**
	 * @return A list of the tips found
	 */
	public ArrayList<String> getTips() {
		return new ArrayList<String>(tips);
	}
	
}
