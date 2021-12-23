package io.virgo.virgoAPI.requestsResponses;

import java.util.ArrayList;
import java.util.HashMap;

import io.virgo.virgoAPI.data.BeaconState;
import io.virgo.virgoAPI.network.ResponseCode;
import io.virgo.virgoCryptoLib.Sha256Hash;

public class GetBeaconsStateResponse extends RequestResponse {

	private HashMap<Sha256Hash, BeaconState> states;
	
	public GetBeaconsStateResponse(ResponseCode code, HashMap<Sha256Hash, BeaconState> states) {
		super(RequestType.GET_BEACONS_STATE, code);
		this.states = states;
	}

	/**
	 * @return An ArrayList of the {@link TransactionState} for the target transactions
	 */
	public ArrayList<BeaconState> getStates() {
		return new ArrayList<BeaconState>(states.values());
	}
	
	/**
	 * Get the transaction state of the given transaction
	 * @param txHash The hash of the transaction to get the state of
	 * @return The {@link TransactionState} of the given transaction or null if not found
	 */
	public BeaconState getState(Sha256Hash txHash) {
		return states.get(txHash);
	}
	
}