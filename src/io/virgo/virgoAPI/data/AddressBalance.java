package io.virgo.virgoAPI.data;

public class AddressBalance {

	private String address;
	
	private long received;
	private long sent;
	
	public AddressBalance(String address, long received, long sent) {
		this.address = address;
		this.received = received;
		this.sent = sent;
	}
	
	public String getAddress() {
		return address;
	}
	
	public long getTotalReceived() {
		return received;
	}
	
	public long getTotalSent() {
		return sent;
	}
	
	public long getFinalBalance() {
		return received-sent;
	}
	
}
