package io.virgo.virgoAPI.requestsResponses;

import java.util.ArrayList;
import java.util.HashMap;

import io.virgo.virgoAPI.data.AddressBalance;
import io.virgo.geoWeb.ResponseCode;

public class GetBalancesResponse extends RequestResponse {

	private HashMap<String, AddressBalance> balances;
	
	public GetBalancesResponse(ResponseCode code, HashMap<String, AddressBalance> balances) {
		super(RequestType.GET_BALANCES, code);
		
		this.balances = balances;
	}
	
	public ArrayList<AddressBalance> getBalances() {
		return new ArrayList<AddressBalance>(balances.values());
	}
	
	public AddressBalance getBalance(String uid) {
		return balances.get(uid);
	}
	
}
