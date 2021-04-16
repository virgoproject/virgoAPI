package io.virgo.virgoAPI.requestsResponses;

import java.math.BigInteger;
import java.util.ArrayList;

import io.virgo.virgoAPI.network.ResponseCode;

public class GetPoWInformationsResponse extends RequestResponse {

	private String parentBeacon;
	private String key;
	private BigInteger difficulty;
	private ArrayList<String> parents;
	
	public GetPoWInformationsResponse(ResponseCode code, String parentBeacon, String key, BigInteger difficulty, ArrayList<String> parents) {
		super(RequestType.GET_POW_INFORMATIONS, code);
		
		this.parentBeacon = parentBeacon;
		this.key = key;
		this.difficulty = difficulty;
		this.parents = parents;
		
	}

	public String getParentBeaconUid() {
		return parentBeacon;
	}
	
	public String getRandomXKey() {
		return key;
	}
	
	public BigInteger getDifficulty() {
		return difficulty;
	}
	
	public ArrayList<String> getParents(){
		return parents;
	}
}
