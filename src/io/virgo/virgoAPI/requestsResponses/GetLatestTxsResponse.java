package io.virgo.virgoAPI.requestsResponses;

import java.util.ArrayList;

import io.virgo.virgoAPI.network.ResponseCode;
import io.virgo.virgoCryptoLib.Sha256Hash;

public class GetLatestTxsResponse extends RequestResponse {

	private ArrayList<Sha256Hash> txs;
	
	
	public GetLatestTxsResponse(ResponseCode responseCode, ArrayList<Sha256Hash> txs) {
		super(RequestType.GET_LATEST_TXS, responseCode);
		
		this.txs = txs;
	}
	
	/**
	 * @return A list of the tips found
	 */
	public ArrayList<Sha256Hash> getTxs() {
		return new ArrayList<Sha256Hash>(txs);
	}
	
	
}
