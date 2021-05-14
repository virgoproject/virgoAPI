package io.virgo.virgoAPI.requestsResponses;

import java.math.BigInteger;
import java.util.ArrayList;

import io.virgo.virgoAPI.network.ResponseCode;

/**
 * Object representing the response to a GetPowInformations request
 */
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

	/**
	 * @return the recommended parent beacon's uid
	 */
	public String getParentBeaconUid() {
		return parentBeacon;
	}
	
	/**
	 * @return The current randomX key
	 */
	public String getRandomXKey() {
		return key;
	}
	
	/**
	 * @return The current difficulty
	 */
	public BigInteger getDifficulty() {
		return difficulty;
	}
	
	/**
	 * @return The recommended parents for a beacon 
	 */
	public ArrayList<String> getParents(){
		return parents;
	}
}
