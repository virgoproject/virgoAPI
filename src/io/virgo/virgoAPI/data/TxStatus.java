package io.virgo.virgoAPI.data;

/**
 * ENUM for transaction status
 */
public enum TxStatus {
	PENDING(0),
	CONFIRMED(1),
	REFUSED(2);
	
	private int code;
	
	TxStatus(int code) {
		this.code = code;
	}
	
	public int getCode() {
		return code;
	}
	
	/**
	 * @return true if this transaction is pending
	 */
	public boolean isPending() {
		return this == PENDING;
	}
	
	/**
	 * @return true if this transaction has been confirmed
	 */
	public boolean isConfirmed() {
		return this == CONFIRMED;
	}
	
	/**
	 * @return true if this transaction has been refused
	 */
	public boolean isRefused() {
		return this == REFUSED;
	}
	
	/**
	 * convert a status code to it's {@link TxStatus} equivalent
	 * @param code the status code to convert
	 * @return the corresponding {@link TxStatus}
	 */
	public static TxStatus fromCode(int code) {
		switch(code) {
		case 0:
			return PENDING;
		case 1:
			return CONFIRMED;
		case 2:
			return REFUSED;
			default:
				return PENDING;
		}
	}
}
