package io.virgo.virgoAPI.crypto;

import io.virgo.virgoAPI.VirgoAPI;
import io.virgo.virgoCryptoLib.Converter;
import io.virgo.virgoCryptoLib.ECDSA;
import io.virgo.virgoCryptoLib.ECDSASignature;
import io.virgo.virgoCryptoLib.Sha256;
import io.virgo.virgoCryptoLib.Sha256Hash;
import io.virgo.virgoCryptoLib.Utils;

/**
 * Object representing an address, with utils to sign messages and check a privateKey against it
 */
//TODO: Complete it to be more like the VirgoWallet one ?
public class Address {

	private String address;
	
	public Address(String address) {
		if(!Utils.validateAddress(address, VirgoAPI.ADDR_IDENTIFIER))
			throw new IllegalArgumentException("Address invalid");
			
		this.address = address;
	}

	public boolean checkAgainstPrivateKey(byte[] privateKey) {
		return address.equals(Converter.Addressify(getPublicKey(privateKey), VirgoAPI.ADDR_IDENTIFIER));
	}
	
	public String getAddress() {
		return address;
	}
	
	public byte[] getPublicKey(byte[] privateKey) {
		return ECDSA.getPublicKey(privateKey);
	}
	
	public ECDSASignature sign(String message, byte[] privateKey) {
		return sign(Sha256.getDoubleHash(message.getBytes()), privateKey);
	}
	
	public ECDSASignature sign(Sha256Hash hash, byte[] privateKey) {
		
		ECDSA signer = new ECDSA();
		return signer.Sign(hash, privateKey);
	}
	
}
