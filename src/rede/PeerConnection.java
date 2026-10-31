package rede;
import java.awt.Window.Type;
import java.io.BufferedReader;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.Socket;

public class PeerConnection extends Thread {

	private final Socket socket;
	private final PeerNode node;
	
	private final ObjectInputStream input;
	private final ObjectOutputStream output;
	
	public PeerConnection(Socket socket, PeerNode node) throws IOException{
		this.socket = socket;
		this.node = node;
		this.output = new ObjectOutputStream(socket.getOutputStream());
		this.output.flush();
		this.input = new ObjectInputStream(socket.getInputStream());
		
	}
	
	@Override
	public void run() {
		try {
			NetworkMessage message = (NetworkMessage)input.readObject();
			node.receive(this, message);
			
		}catch(EOFException e) {
			
			
		}catch(IOException | ClassNotFoundException e) {
			System.out.println("Peer Lost!");
			
		}finally {
			try{socket.close();}catch(Exception ignored) {};
		}
	}
	
	public synchronized void send(NetworkMessage message) {
		
		try {
			output.writeObject(message);
			output.reset();
			output.flush();
		}catch(IOException e){
			try{socket.close();}catch(Exception ignored) {}
		}
		
	}
	
	public synchronized void recv(NetworkMessage message) {
		if(message == null)
			return;
		
		FileInputStream fis = new FileInputStream("data.message");
		ObjectInputStream ois = new ObjectInputStream(fis);
		NetworkMessage clienteRecuperado = (NetworkMessage) ois.readObject();
		ois.close();
		
		MessageType type = message.getType();
		switch(type) {
		case HELLO -> helloHandler(); //payload = String
			
		case GET_PEERS -> getPeersHandler();//payload = null
			
		case PEERS_LIST -> peerListHandler();//payload = List<String>
			
		case GET_BLOCKCHAIN -> getBlockchainHandler();//payload = null
			
		case BLOCKCHAIN_COPY -> blockchainCopyHandler();//payload = ArrayList<BlockChain>
			
		case REJECT -> rejectHandler();//payload = String
			
		default -> System.err.println("Unknown message type!");
		}
		
	}
	
	private void helloHandler() {
		
	}
	private void getPeersHandler() {
		
	}
	private void peerListHandler() {
		
	}
	private void getBlockchainHandler() {
		
	}
	private void blockchainCopyHandler() {
		
	}
	private void rejectHandler() {
		
	}
	
}
