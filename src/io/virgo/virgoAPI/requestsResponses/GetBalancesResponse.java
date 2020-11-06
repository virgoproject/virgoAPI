package io.virgo.virgoAPI.requestsResponses;

import java.util.ArrayList;
import java.util.HashMap;

import io.virgo.virgoAPI.data.AddressBalance;
import io.virgo.geoWeb.ResponseCode;

/**
 * Object representing the response to a GetBalances request
 */
public class GetBalancesResponse extends RequestResponse {

	private HashMap<String, AddressBalance> balances;
	
	public GetBalancesResponse(ResponseCode code, HashMap<String, AddressBalance> balances) {
		super(RequestType.GET_BALANCES, code);
		
		this.balances = balances;
	}
	
	/**
	 * @return An arrayList of {@link AddressBalance} for the target addresses 
	 */
	public ArrayList<AddressBalance> getBalances() {
		return new ArrayList<AddressBalance>(balances.values());
	}
	
	/**
	 * Get the balance of a specific address
	 * @param uid the target address
	 * @return a {@link AddressBalance} object for the target address or null if not found
	 */
	public AddressBalance getBalance(String uid) {
		return balances.get(uid);
	}
	
}
