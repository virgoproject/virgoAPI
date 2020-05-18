package io.virgo.virgoAPI.requestsResponses;

import java.util.ArrayList;

import io.virgo.geoWeb.ResponseCode;

public class GetTipsResponse extends RequestResponse {

	private ArrayList<String> tips;
	
	public GetTipsResponse(ResponseCode responseCode, ArrayList<String> tips) {
		super(RequestType.GET_TIPS, responseCode);
		
		this.tips = tips;
	}
	
	public ArrayList<String> getTips() {
		return tips;
	}
	
}
