package io.virgo.virgoAPI.requestsResponses;

import java.util.ArrayList;
import java.util.HashMap;

import io.virgo.virgoAPI.network.ResponseCode;

public class GetUnspentInputsResponse extends RequestResponse {

	public HashMap<String, ArrayList<String>> inputs;
	
	public GetUnspentInputsResponse(ResponseCode code, HashMap<String, ArrayList<String>> addressesTxsMap) {
		super(RequestType.GET_UNSPENT_INPUTS, code);
		this.inputs = addressesTxsMap;
	}

	public ArrayList<String> getUnspentInputs(String address) {
		return inputs.get(address);
	}
	
}
