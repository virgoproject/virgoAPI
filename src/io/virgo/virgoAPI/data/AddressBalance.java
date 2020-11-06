package io.virgo.virgoAPI.data;

/**
 * An object representing an address balance: How much have been received and spent
 */
public class AddressBalance {

	private String address;
	
	private long received;
	private long sent;
	
	/**
	 * Create a new address balance representation
	 * @param address The target address
	 * @param received How much it received
	 * @param sent How much it spent
	 */
	public AddressBalance(String address, long received, long sent) {
		this.address = address;
		this.received = received;
		this.sent = sent;
	}
	
	/**
	 * @return The address this balance is about
	 */
	public String getAddress() {
		return address;
	}
	
	/**
	 * @return How much the address received
	 */
	public long getTotalReceived() {
		return received;
	}
	
	/**
	 * @return How much the address spent
	 */
	public long getTotalSent() {
		return sent;
	}
	
	/**
	 * @return The address's final balance
	 */
	public long getFinalBalance() {
		return received-sent;
	}
	
}
