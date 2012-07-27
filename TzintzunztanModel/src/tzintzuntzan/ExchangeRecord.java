package tzintzuntzan;

import tzintzuntzan.Settings.ExchangeType;

/**
 * 
 * @author shah
 *
 */

public class ExchangeRecord {
	private int senderID = -1;
	private int receiverID = -1;
	private int timestep = -1;
	private ExchangeType exchangeType = ExchangeType.NONE;
	private int exchangeValue = -1;
	
	public ExchangeRecord() {}
	
	/**
	 * Constructor for the ExchangeRecord Structure
	 * @param _timestep
	 * @param _exchangeType
	 * @param _exchangeValue
	 */
	public ExchangeRecord(int _timestep, ExchangeType _exchangeType, int _exchangeValue) {
		this.timestep = _timestep;
		this.exchangeType = _exchangeType;
		this.exchangeValue = _exchangeValue;
	}

	public int getTimestep() {
		return timestep;
	}

	public void setTimestep(int timestep) {
		this.timestep = timestep;
	}

	public int getExchangeValue() {
		return exchangeValue;
	}

	public void setExchangeValue(int exchangeValue) {
		this.exchangeValue = exchangeValue;
	}

	public int getSenderID() {
		return senderID;
	}

	public void setSenderID(int senderID) {
		this.senderID = senderID;
	}

	public int getReceiverID() {
		return receiverID;
	}

	public void setReceiverID(int receiverID) {
		this.receiverID = receiverID;
	}

	public ExchangeType getExchangeType() {
		return exchangeType;
	}

	public void setExchangeType(ExchangeType exchangeType) {
		this.exchangeType = exchangeType;
	}		
}