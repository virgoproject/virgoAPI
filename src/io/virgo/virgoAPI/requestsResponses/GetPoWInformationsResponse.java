package io.virgo.virgoAPI.requestsResponses;

import java.math.BigInteger;
import java.util.ArrayList;

import io.virgo.virgoAPI.network.ResponseCode;
import io.virgo.virgoCryptoLib.Sha256Hash;

/**
 * Object representing the response to a GetPowInformations request
 */
public class GetPoWInformationsResponse extends RequestResponse {

	private Sha256Hash parentBeacon;
	private Sha256Hash key;
	private BigInteger difficulty;
	private ArrayList<Sha256Hash> parents;
	
	public GetPoWInformationsResponse(ResponseCode code, Sha256Hash parentBeacon, Sha256Hash key, BigInteger difficulty, ArrayList<Sha256Hash> parents) {
		super(RequestType.GET_POW_INFORMATIONS, code);
		
		this.parentBeacon = parentBeacon;
		this.key = key;
		this.difficulty = difficulty;
		this.parents = parents;
		
	}

	/**
	 * @return the recommended parent beacon's uid
	 */
	public Sha256Hash getParentBeaconUid() {
		return parentBeacon;
	}
	
	/**
	 * @return The current randomX key
	 */
	public Sha256Hash getRandomXKey() {
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
	public ArrayList<Sha256Hash> getParents(){
		return parents;
	}
}
