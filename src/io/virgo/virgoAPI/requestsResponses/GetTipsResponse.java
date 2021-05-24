package io.virgo.virgoAPI.requestsResponses;

import java.util.ArrayList;

import io.virgo.virgoAPI.network.ResponseCode;
import io.virgo.virgoCryptoLib.Sha256Hash;

/**
 * Object representing the response to a GetTips request
 */
public class GetTipsResponse extends RequestResponse {

	private ArrayList<Sha256Hash> tips;
	
	public GetTipsResponse(ResponseCode responseCode, ArrayList<Sha256Hash> tips) {
		super(RequestType.GET_TIPS, responseCode);
		
		this.tips = tips;
	}
	
	/**
	 * @return A list of the tips found
	 */
	public ArrayList<Sha256Hash> getTips() {
		return new ArrayList<Sha256Hash>(tips);
	}
	
}
