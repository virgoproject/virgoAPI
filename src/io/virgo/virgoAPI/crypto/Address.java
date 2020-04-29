package io.virgo.virgoAPI.crypto;

import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import io.virgo.virgoAPI.BoltAPI;
import io.virgo.virgoAPI.data.AddressTxs;
import net.boltLabs.boltCryptoLib.Converter;
import net.boltLabs.boltCryptoLib.ECDSA;
import net.boltLabs.boltCryptoLib.ECDSASignature;
import net.boltLabs.boltCryptoLib.Sha256;
import net.boltLabs.boltCryptoLib.Sha256Hash;

public class Address {

	private String address;
	private byte[] pubKey;
	private byte[] privKey;
	
	public Address(byte[] privKey) {
		this(privKey, 0);
	}
	
	public Address(byte[] privKey, long unlockTime) {
		this.privKey = privKey;
		pubKey = ECDSA.getPublicKey(privKey);
		address = Converter.Addressify(pubKey, BoltAPI.ADDR_IDENTIFIER);
		
		if(unlockTime > 0) {
			
			new Timer().schedule(new TimerTask() {

				@Override
				public void run() {
					lock();
				}
				
			}, unlockTime);
			
		}
		
	}
	
	public void lock() {
		privKey = null;
	}
	
	public void unlock(byte[] privKey) {
		unlock(privKey, 0);
	}
	
	public void unlock(byte[] privKey, long unlockTime) {
		if(ECDSA.getPublicKey(privKey) != pubKey)
			throw new IllegalArgumentException("Wrong private key given");
		
		this.privKey = privKey;
		
		if(unlockTime > 0) {
			
			new Timer().schedule(new TimerTask() {

				@Override
				public void run() {
					lock();
				}
				
			}, unlockTime);			
			
		}
	}

	public String getAddress() {
		return address;
	}
	
	public byte[] getPublicKey() {
		return pubKey;
	}
	
	public ECDSASignature sign(String message) {
		return sign(Sha256.getDoubleHash(message.getBytes()));
	}
	
	public ECDSASignature sign(Sha256Hash hash) {
		if(privKey == null)
			throw new IllegalStateException("Private key not loaded, please unlock your wallet");
		
		ECDSA signer = new ECDSA();
		return signer.Sign(hash, privKey);
	}
	
}
