package io.virgo.virgoAPI.requestsResponses;

import java.util.ArrayList;
import java.util.HashMap;

import io.virgo.virgoAPI.data.AddressTxs;
import io.virgo.virgoAPI.network.ResponseCode;

/**
 * Object representing the response to a GetAddressesTxs request
 */
public class GetAddressesTxsResponse extends RequestResponse {

	private HashMap<String, AddressTxs> addressesTxs;
	
	public GetAddressesTxsResponse(ResponseCode code, HashMap<String, AddressTxs> addressesTxsMap) {
		super(RequestType.GET_ADDR_TXS, code);
		
		addressesTxs = addressesTxsMap;
	}

	/**
	 * @return An arrayList of {@link AddressTxs} for the target addresses 
	 */
	public ArrayList<AddressTxs> getAddressesTxs() {
		return new ArrayList<AddressTxs>(addressesTxs.values());
	}
	
	/**
	 * Get the transactions about a specific address
	 * @param uid the target address
	 * @return a {@link AddressTxs} object for the target address or null if not found
	 */
	public AddressTxs getAddressTxs(String uid) {
		return addressesTxs.get(uid);
	}	
	
}
