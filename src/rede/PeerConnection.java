package rede;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;

public class PeerConnection extends Thread {

	private final Socket socket;
	private final PeerNode node;
	private final boolean sender;
	private String address;

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

		}catch(SocketException e) {
			//connection closed locally (duplicate resolution / shutdown)

		}catch(IOException | ClassNotFoundException e) {
			System.out.println("Connection lost: " + e.getMessage());

		}catch(ClassCastException e) {
			System.out.println("Invalid message received!");

		}finally {
			node.disconnect(this);
			close();
		}
	}

	public synchronized void send(NetworkMessage message) {
		if (socket.isClosed()) return;

		try {
			output.writeObject(message);
			output.reset();
			output.flush();
		}catch(IOException e){
			close();
		}
	}

	public void close() {
		try {
			socket.close();
		}catch(IOException ignored) {}
	}

	public boolean isClosed() {
		return socket.isClosed();
	}

	public boolean isSender() {
		return this.sender;
	}

	public Socket getSocket() {
		return this.socket;
	}

	public String getAddress() {
		return address;
	}
	public void setAddress(String address) {
		this.address = address;
	}

}
