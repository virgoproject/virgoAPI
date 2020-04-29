package io.virgo.virgoAPI;

import io.virgo.virgoAPI.crypto.Address;

public class AddrAndBal {

	Address addr;
	long balance;
	
	public AddrAndBal(Address addr, long balance) {
		this.addr = addr;
		this.balance = balance;
	}
	
}
