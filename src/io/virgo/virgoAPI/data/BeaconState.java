package io.virgo.virgoAPI.data;

import org.json.JSONObject;

import io.virgo.virgoCryptoLib.Sha256Hash;

public class BeaconState {

	private Sha256Hash hash;
	private long diff;
	private boolean isMainChainMember;
	private long weight;
	private Sha256Hash parentBeacon;
	private long confirmations;
	private long height;
	private Sha256Hash randomXKey;
	
	public BeaconState(Sha256Hash hash, long diff, boolean isMainChainMember, long weight, Sha256Hash parentBeacon, long confirmations, long height, Sha256Hash randomXKey) {
		this.hash = hash;
		this.diff = diff;
		this.isMainChainMember = isMainChainMember;
		this.weight = weight;
		this.parentBeacon = parentBeacon;
		this.confirmations = confirmations;
		this.height = height;
		this.randomXKey = randomXKey;
	}
	
	public static BeaconState fromJSON(Sha256Hash hash, JSONObject json) {
		try {
			
			return new BeaconState(
					hash,
					json.getLong("difficulty"),
					json.getBoolean("isMainChainMember"),
					json.getLong("weight"),
					new Sha256Hash(json.getString("parentBeacon")),
					json.getLong("confirmations"),
					json.getLong("height"),
					new Sha256Hash(json.getString("randomXKey"))
					);
					
		}catch(Exception e) {
			return null;
		}
	}
	
	/**
	 * @return The Id of the transaction this object is about
	 */
	public Sha256Hash getHash() {
		return hash;
	}
	
	public long getDifficulty() {
		return diff;
	}
	
	public boolean isMainChainMember() {
		return isMainChainMember;
	}
	
	public long getWeight() {
		return weight;
	}
	
	public Sha256Hash getParentBeacon() {
		return parentBeacon;
	}
	
	public long getConfirmations() {
		return confirmations;
	}
	
	public long getHeight() {
		return height;
	}
	
	public Sha256Hash getRandomXKey() {
		return randomXKey;
	}
	
}
