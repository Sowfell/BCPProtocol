package rede;

import java.io.Serializable;

public class NetworkMessage implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private MessageType type;
	private Serializable payload;
	
	public NetworkMessage(MessageType type, Serializable payload) {
		this.type = type;
		this.payload = payload;
	}
	
	public MessageType getType() {
		return type;
	}
	public void setType(MessageType type) {
		this.type = type;
	}
	public Object getPayload() {
		return payload;
	}
	public void setPayload(Serializable payload) {
		this.payload = payload;
	}
	
}
