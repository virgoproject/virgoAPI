package io.virgo.virgoAPI.data;

import java.util.ArrayList;

import io.virgo.virgoCryptoLib.Sha256Hash;

/**
 * An object resuming all input and output transactions of an address
 */
public class AddressTxs {

	private String address;
	private ArrayList<Sha256Hash> transactions;
	private int size;
	
	public AddressTxs(String address, ArrayList<Sha256Hash> transactions, int size) {
		this.address = address;
		this.transactions = transactions;
		this.size = size;
	}
	
	/**
	 * @return The address this object is about
	 */
	public String getAddress() {
		return address;
	}
	
	/**
	 * @return The address's transactions ids
	 */
	public Sha256Hash[] getTransactions() {
		return transactions.toArray(new Sha256Hash[transactions.size()]);
	}
	
	public int getTotalSize() {
		return size;
	}
	
}
