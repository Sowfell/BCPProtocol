package rede;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class PeerConnection extends Thread {

	private final Socket socket;
	private final PeerNode node;
	private final BufferedReader input;
	private final PrintWriter output;
	
	public PeerConnection(Socket socket, PeerNode node) throws IOException{
		this.socket = socket;
		this.node = node;
		this.input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		this.output = new PrintWriter(socket.getOutputStream(), true);
	}
	
	@Override
	public void run() {
		try {
			String line;
			while ((line = input.readLine()) != null) {
				node.receive(this,line);
			}
		}catch(IOException e) {
			System.out.println("Connection error!");
		}finally {
			try{socket.close();}catch(Exception ignored) {};
		}
	}
	
	public synchronized void send(String message) {
		output.println(message);
	}
	
}
