package io.virgo.virgoAPI.requestsResponses;

import java.util.ArrayList;

import io.virgo.virgoAPI.network.ResponseCode;
import io.virgo.virgoCryptoLib.Sha256Hash;

public class GetLatestBeaconsResp extends RequestResponse {

	private ArrayList<Sha256Hash> beacons;
	
	
	public GetLatestBeaconsResp(ResponseCode responseCode, ArrayList<Sha256Hash> beacons) {
		super(RequestType.GET_LATEST_BEACONS, responseCode);
		
		this.beacons = beacons;
	}
	
	/**
	 * @return A list of the tips found
	 */
	public ArrayList<Sha256Hash> getBeacons() {
		return new ArrayList<Sha256Hash>(beacons);
	}
	
	
}
