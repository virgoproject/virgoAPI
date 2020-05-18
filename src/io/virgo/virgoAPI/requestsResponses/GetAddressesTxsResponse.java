package io.virgo.virgoAPI.requestsResponses;

import java.util.ArrayList;
import java.util.HashMap;

import io.virgo.virgoAPI.data.AddressTxs;
import io.virgo.geoWeb.ResponseCode;

public class GetAddressesTxsResponse extends RequestResponse {

	private HashMap<String, AddressTxs> addressesTxs;
	
	public GetAddressesTxsResponse(ResponseCode code, HashMap<String, AddressTxs> addressesTxsMap) {
		super(RequestType.GET_ADDR_TXS, code);
		
		addressesTxs = addressesTxsMap;
	}

	public ArrayList<AddressTxs> getAddressesTxs() {
		return new ArrayList<AddressTxs>(addressesTxs.values());
	}
	
	public AddressTxs getAddressTxs(String uid) {
		return addressesTxs.get(uid);
	}	
	
}
