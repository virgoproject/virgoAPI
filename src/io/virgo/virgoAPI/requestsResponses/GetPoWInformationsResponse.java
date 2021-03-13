package io.virgo.virgoAPI.requestsResponses;

import java.util.ArrayList;

import io.virgo.geoWeb.ResponseCode;

public class GetPoWInformationsResponse extends RequestResponse {

	private String parentBeacon;
	private long difficulty;
	private ArrayList<String> parents;
	
	public GetPoWInformationsResponse(ResponseCode code, String parentBeacon, long difficulty, ArrayList<String> parents) {
		super(RequestType.GET_POW_INFORMATIONS, code);
		
		this.parentBeacon = parentBeacon;
		this.difficulty = difficulty;
		this.parents = parents;
		
	}

	public String getParentBeaconUid() {
		return parentBeacon;
	}
	
	public long getDifficulty() {
		return difficulty;
	}
	
	public ArrayList<String> getParents(){
		return parents;
	}
}
