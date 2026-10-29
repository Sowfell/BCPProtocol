package rede;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

public class PeerNode{
	
	//Constants
	private final static int MAX_PEERS = 3;
	private final static int INITIAL_LISTENING_DOOR = 6000;
	private static int newPeersCount = 0;
	private final int door;
	
	//Atributes
	private ConcurrentHashMap<String,PeerConnection> neighborhoodPeers = new ConcurrentHashMap<>();
	private ConcurrentHashMap<String,Integer> knownPeers = new ConcurrentHashMap<>();	
	
	//get the door through console/main
	public PeerNode(int door) {
		this.door = door;
	}
	
	public void start() {
		try(ServerSocket server = new ServerSocket(door)){
			System.out.println("Server starded, listening for new connection requests");
			while(true) {
				Socket socket = server.accept();
				new PeerConnection(socket, this).start();;
			}
		}catch(IOException e) {
			System.out.println("Server error: " + e.getMessage());
		}
	}
	
	public void connect(String address) {
		try {
			String[] ipDoor = address.split(":");
			Socket socket = new Socket(ipDoor[0],Integer.parseInt(ipDoor[1]));
			PeerConnection connection = new PeerConnection(socket,this);
			connection.start();
			connection.send("Hello" + door);
		}catch(IOException e) {
			System.out.println("Cannot connet on addres [" + address + "]");
		}
	}
	
	public void receive(PeerConnection origin, String message) {
		System.out.println("Door[" + door + "] received: " + message);
	}
	
	
	
	public static void main(String[] args) {
        int porta = Integer.parseInt(args[0]);
        PeerNode node = new PeerNode(porta);

        // peers conhecidos vêm dos outros argumentos
        for (int i = 1; i < args.length; i++) {
            node.connect(args[i]);
        }

        node.start(); // por último, porque fica em loop
    }
	
}
