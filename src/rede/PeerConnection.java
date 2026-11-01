package rede;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class PeerConnection extends Thread {

	private final Socket socket;
	private final PeerNode node;
	private final boolean sender;
	
	private final ObjectInputStream input;
	private final ObjectOutputStream output;
	
	public PeerConnection(Socket socket, PeerNode node, boolean sender) throws IOException{
		this.socket = socket;
		this.node = node;
		this.output = new ObjectOutputStream(socket.getOutputStream());
		this.output.flush();
		this.input = new ObjectInputStream(socket.getInputStream());
		this.sender = sender;
		
	}
	
	@Override
	public void run() {
		try {
			while(true) {
				NetworkMessage message = (NetworkMessage)input.readObject();
				node.recv(this, message);
			}
		}catch(EOFException e) {
			//other side just close de connection
			
		}catch(IOException | ClassNotFoundException e) {
			System.out.println("Connection lost!");
			
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
	
	public boolean isSender() {
		return this.sender;
	}
	
	public Socket getSocket() {
		return this.socket;
	}
	
	
}
