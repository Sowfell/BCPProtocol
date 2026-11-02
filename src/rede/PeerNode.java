package rede;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import blockchain.BlockChain;

public class PeerNode{
	
	//Constants
	private final static int MAX_PEERS = 5;
	private final static int MIN_PEERS = 2;
	private final static int MAX_SEEDS = 1;
	private final int door;
	
	//Atributes
	private ConcurrentHashMap<String,PeerConnection> neighborhoodPeers = new ConcurrentHashMap<>();
	private ArrayList<String> knownPeers = new ArrayList<>();	
	private BlockChain blockChain = new BlockChain();
	private final List<String> seeds = new ArrayList<>(); 
	private String id;
	
	//get the door through console/main
	public PeerNode(int door) {
		this.door = door;
	}
	
	public void init() {
		try(ServerSocket server = new ServerSocket(door)){
			System.out.println("Server starded, listening for new connection requests");
			while(true) {
				Socket socket = server.accept();
				new PeerConnection(socket, this, false).start();
						}
		}catch(IOException e) {
			System.out.println("Server error: " + e.getMessage());
		}
	}
	
	public void connect(String address) {
		try {
			String[] ipDoor = address.split(":");
			Socket socket = new Socket(ipDoor[0],Integer.parseInt(ipDoor[1]));
			PeerConnection connection = new PeerConnection(socket,this, true);
			connection.start();
			connection.send(new NetworkMessage(MessageType.HELLO, id));
			connection.send(new NetworkMessage(MessageType.GET_PEERS, null));
			connection.send(new NetworkMessage(MessageType.GET_BLOCKCHAIN, null));

		}catch(IOException e) {
			System.out.println("Cannot connet on addres [" + address + "]");
		}
	}
	
	public void receive(PeerConnection origin, NetworkMessage message) {
		System.out.println("Door[" + door + "] received: " + message);
	}
	
	public synchronized void recv(PeerConnection origin,NetworkMessage message) {
		if(message == null)
			return;
		
		MessageType type = message.getType();
		switch(type) {
		case HELLO -> helloHandler(origin, message); //payload = String

		case GET_PEERS -> getPeersHandler(origin);//payload = null
			
		case PEERS_LIST -> peerListHandler(origin,message);//payload = List<String>
			
		case GET_BLOCKCHAIN -> getBlockchainHandler(origin);//payload = null
			
		case BLOCKCHAIN_COPY -> blockchainCopyHandler(origin,message);//payload = ArrayList<BlockChain>
			
		case REJECT -> rejectHandler(origin);//payload = String
			
		default -> System.err.println("Unknown message type!");
		}
		
	}
	
	private synchronized void helloHandler(PeerConnection origin, NetworkMessage message) {
		if(!origin.isSender()) {
			System.out.println(message.getPayload());
			origin.send(new NetworkMessage(MessageType.HELLO, "Connected in: " + id));
		}else {
			System.out.println(message.getPayload());
		}
	}
	
	private synchronized void getPeersHandler(PeerConnection origin) {
		origin.send(new NetworkMessage(MessageType.PEERS_LIST, new ArrayList<>(this.knownPeers)));
		System.out.println("enviando peers conhecidos para: " + origin.getSocket().getLocalAddress().getHostAddress());
	}
	
	@SuppressWarnings("unchecked")
	private synchronized void peerListHandler(PeerConnection origin, NetworkMessage message) {
		this.knownPeers = (ArrayList<String>)message.getPayload();
		System.out.println("Peers novos recebidos");
	}
	
	private synchronized void getBlockchainHandler(PeerConnection origin) {
		origin.send(new NetworkMessage(MessageType.BLOCKCHAIN_COPY, this.blockChain));
		System.out.println("enviando blockchain local para: " + origin.getSocket().getLocalAddress().getHostAddress());
	}
	
	private synchronized void blockchainCopyHandler(PeerConnection origin, NetworkMessage message) {
		this.blockChain = (BlockChain)message.getPayload();
		System.out.println("blockchain recebida");
	}
	
	private synchronized void rejectHandler(PeerConnection origin) {
		System.out.println("Fudeu");
		
	}
	
	
	public static void main(String[] args) {
        int porta = Integer.parseInt(args[0]);
        PeerNode node = new PeerNode(porta);
		node.id = args[1];

        // peers conhecidos vêm dos outros argumentos
        for (int i = 2; i < args.length; i++) {
        	node.seeds.add(args[i]);
            node.connect(args[i]);
        }

        node.init(); // por último, porque fica em loop
    }
	
}
