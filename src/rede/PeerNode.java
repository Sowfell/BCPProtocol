package rede;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import blockchain.BlockChain;
import blockchain.ReplaceMessage;

public class PeerNode{

	//Constants
	private final static int MAX_PEERS = 5;
	private final static int MAX_KNOWN_PEERS = 50;
	private final static int CONNECT_TIMEOUT_MS = 3000;
	private final static int HANDSHAKE_TIMEOUT_MS = 5000;
	private final static String PROTOCOL_VERSION = "V1.0-";
	// Seed node estatico exigido pelo enunciado. Na apresentacao em varias
	// maquinas, troque pelo IP da maquina semente na rede local.
	private final static String DEFAULT_SEED = "127.0.0.1:8333";
	private final int door;

	//Atributes
	private final ConcurrentHashMap<String,PeerConnection> neighborhoodPeers = new ConcurrentHashMap<>();
	private final Set<String> knownPeers = ConcurrentHashMap.newKeySet();
	private final Set<String> connecting = ConcurrentHashMap.newKeySet();
	private final BlockChain blockChain = new BlockChain();
	private final List<String> seeds = new ArrayList<>();
	private final CountDownLatch serverStarted = new CountDownLatch(1);
	private volatile boolean serverReady = false;
	private String id;

	//get the door through console/main
	public PeerNode(int door) {
		this.door = door;
		this.id = "node-" + door;
	}

	public void init() {
		try(ServerSocket server = new ServerSocket(door)){
			System.out.println("Server started, listening on port " + door);
			serverReady = true;
			serverStarted.countDown();
			while(true) {
				Socket socket = server.accept();
				// O construtor do PeerConnection faz o handshake dos streams e pode
				// bloquear. Roda em uma thread separada para nao travar o accept.
				new Thread(() -> {
					try {
						socket.setSoTimeout(HANDSHAKE_TIMEOUT_MS);
						PeerConnection connection = new PeerConnection(socket, this, false);
						socket.setSoTimeout(0);
						connection.start();
					} catch (IOException e) {
						try { socket.close(); } catch (IOException ignored) {}
						System.out.println("Failed to initialize incoming connection: " + e.getMessage());
					}
				}, "accept-" + socket.getRemoteSocketAddress()).start();
			}
		}catch(IOException e) {
			System.out.println("Server error: " + e.getMessage());
		}finally {
			serverStarted.countDown();
		}
	}

	// Sobe o servidor primeiro e so depois conecta nos seeds. Assim um no
	// nunca tenta conectar antes do proprio ServerSocket estar ouvindo.
	public void start() {
		Thread server = new Thread(this::init, "server-" + door);
		server.start();

		try {
			serverStarted.await();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return;
		}

		if (!serverReady) return;

		for (String seed : seeds) {
			connect(seed);
		}

		try {
			server.join();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	public void connect(String address) {
		String key = canonical(address);
		if (key == null || itsMe(key)) return;

		String[] parts = split(key);
		if (parts == null) return;

		// evita duas threads tentando abrir a mesma conexao ao mesmo tempo
		if (!connecting.add(key)) return;

		Socket socket = null;
		try {
			synchronized (this) {
				PeerConnection existing = neighborhoodPeers.get(key);
				if (existing != null && !existing.isClosed()) return;
				if (existing != null) neighborhoodPeers.remove(key, existing);
				if (neighborhoodPeers.size() >= MAX_PEERS) return;
			}

			socket = new Socket();
			socket.connect(new InetSocketAddress(parts[0], Integer.parseInt(parts[1])), CONNECT_TIMEOUT_MS);
			socket.setSoTimeout(HANDSHAKE_TIMEOUT_MS);

			PeerConnection connection = new PeerConnection(socket,this, true);
			socket.setSoTimeout(0);
			connection.setAddress(key);

			synchronized (this) {
				if (neighborhoodPeers.size() >= MAX_PEERS && !neighborhoodPeers.containsKey(key)) {
					connection.close();
					return;
				}
				// Se ja existe outra conexao com esse peer, a duplicata sera
				// resolvida quando o HELLO chegar (ver resolveConnection).
				neighborhoodPeers.putIfAbsent(key, connection);
			}

			connection.start();
			connection.send(new NetworkMessage(MessageType.HELLO, helloPayload()));
			connection.send(new NetworkMessage(MessageType.GET_PEERS, null));
			connection.send(new NetworkMessage(MessageType.GET_BLOCKCHAIN, null));

		}catch(IOException e) {
			if (socket != null) {
				try { socket.close(); } catch (IOException ignored) {}
			}
			System.out.println("Cannot connect on address [" + address + "]");
		}finally {
			connecting.remove(key);
		}
	}

	public void recv(PeerConnection origin,NetworkMessage message) {
		if(message == null)
			return;

		MessageType type = message.getType();
		switch(type) {
		case HELLO -> helloHandler(origin, message); //payload = String

		case GET_PEERS -> getPeersHandler(origin);//payload = null

		case PEERS_LIST -> peerListHandler(origin,message);//payload = List<String>

		case GET_BLOCKCHAIN -> getBlockchainHandler(origin);//payload = null

		case BLOCKCHAIN_COPY -> blockchainCopyHandler(origin,message);//payload = ArrayList<BlockChain>

		case REJECT -> rejectHandler(origin,message);//payload = String

		default -> System.err.println("Unknown message type!");
		}
	}

	private void helloHandler(PeerConnection origin, NetworkMessage message) {
		if (!(message.getPayload() instanceof String helloPayload)) {
			origin.close();
			return;
		}

		String[] hello = parseHello(helloPayload);
		if (hello == null) {
			origin.close();
			return;
		}

		if(!origin.isSender()) {
			String remoteId = hello[0];
			String address = canonical(origin.getSocket().getInetAddress().getHostAddress() + ":" + hello[1]);
			if (address == null) {
				origin.close();
				return;
			}
			origin.setAddress(address);

			resolveConnection(origin, address, remoteId);
			if (origin.isClosed()) return;

			System.out.println("Peer " + remoteId + " connected from " + address);
			origin.send(new NetworkMessage(MessageType.HELLO, helloPayload()));

		}else {
			// resposta ao HELLO que este no enviou
			String remoteId = hello[0];
			String address = origin.getAddress();
			if (address == null) {
				origin.close();
				return;
			}

			resolveConnection(origin, address, remoteId);
			if (origin.isClosed()) return;

			System.out.println("HELLO accepted by " + remoteId + " at " + address);
		}
	}

	private void resolveConnection(PeerConnection connection, String address, String remoteId) {
		if (address == null || connection.isClosed()) {
			connection.close();
			return;
		}

		synchronized (this) {
			if (connection.isClosed()) return;

			knownPeers.add(address);

			PeerConnection existing = neighborhoodPeers.get(address);
			if (existing == connection) return;

			if (existing != null && existing.isClosed()) {
				neighborhoodPeers.remove(address, existing);
				existing = null;
			}

			if (existing == null) {
				if (neighborhoodPeers.size() >= MAX_PEERS) {
					connection.close();
					return;
				}
				neighborhoodPeers.put(address, connection);
				return;
			}

			// Quando os dois lados se conectam ao mesmo tempo, mantem a conexao
			// iniciada pelo no de menor id. Assim os dois lados escolhem a mesma.
			// Se os ids forem iguais, usa o endereco como desempate.
			int comparison = id.compareTo(remoteId);
			if (comparison == 0) {
				String localAddress = connection.getSocket().getLocalAddress().getHostAddress() + ":" + door;
				comparison = localAddress.compareTo(address);
			}
			boolean preferSender = comparison < 0;
			if (existing.isSender() == preferSender) {
				connection.close();
			} else {
				neighborhoodPeers.put(address, connection);
				existing.close();
			}
		}
	}

	private void getPeersHandler(PeerConnection origin) {
		ArrayList<String> list = new ArrayList<>(knownPeers);
		list.remove(origin.getAddress());
		list.removeIf(this::itsMe);
		origin.send(new NetworkMessage(MessageType.PEERS_LIST, list));
		System.out.println("Sending " + list.size() + " known peers to " + origin.getAddress());
	}

	private void peerListHandler(PeerConnection origin, NetworkMessage message) {
		if (!(message.getPayload() instanceof List<?> candidates)) return;

		List<String> toConnect = new ArrayList<>();
		synchronized (this) {
			for(Object candidateObj : candidates) {
				if (knownPeers.size() >= MAX_KNOWN_PEERS) break;
				if (!(candidateObj instanceof String candidate)) continue;

				String key = canonical(candidate);
				if (key == null || itsMe(key)) continue;

				// So tenta conectar em peers novos. Reutilizar os ja conhecidos
				// era o que gerava o loop de PEERS_LIST -> connect -> GET_PEERS.
				if (knownPeers.add(key) && neighborhoodPeers.size() < MAX_PEERS) {
					toConnect.add(key);
				}
			}
		}

		// connect() faz I/O bloqueante: chama fora do lock do no.
		for(String key : toConnect) {
			connect(key);
		}
	}

	private void getBlockchainHandler(PeerConnection origin) {
		synchronized (blockChain) {
			origin.send(new NetworkMessage(MessageType.BLOCKCHAIN_COPY, this.blockChain));
		}
		System.out.println("Sending local blockchain to " + origin.getAddress());
	}

	private void blockchainCopyHandler(PeerConnection origin, NetworkMessage message) {
		if (!(message.getPayload() instanceof BlockChain received)) return;
		// cadeia vazia nao substitui nada; evita REJECT desnecessario no startup
		if (received.getBlockChain() == null || received.getBlockChain().isEmpty()) return;

		ReplaceMessage output = this.blockChain.replaceChain(received.getBlockChain());

		if(output == ReplaceMessage.DONTCHANGE || output == ReplaceMessage.INVALIDCHAIN ) {
			origin.send(new NetworkMessage(MessageType.REJECT, BlockChain.rejectReason(output)));
		}else{
			synchronized (blockChain) {
				broadcast(new NetworkMessage(MessageType.BLOCKCHAIN_COPY, this.blockChain), origin);
			}
		}
	}

	private void rejectHandler(PeerConnection origin, NetworkMessage message) {
		System.out.println("Rejected by " + origin.getAddress() + ": " + message.getPayload());
	}

	public void disconnect(PeerConnection connection) {
		String address = connection.getAddress();
		if (address != null) {
			// remove so se ainda for esta conexao (uma nova pode ter substituido)
			neighborhoodPeers.remove(address, connection);
		}
	}

	// Envia uma mensagem para todos os vizinhos conectados.
	public void broadcast(NetworkMessage message) {
		broadcast(message, null);
	}

	// Igual ao anterior, mas ignora a conexao de origem (evita eco).
	public void broadcast(NetworkMessage message, PeerConnection except) {
		for (PeerConnection peer : neighborhoodPeers.values()) {
			if (peer != except) {
				peer.send(message);
			}
		}
	}

	// Fluxo C: chamar depois de minerar/inserir um bloco local.
	public void broadcastBlockchain() {
		synchronized (blockChain) {
			broadcast(new NetworkMessage(MessageType.BLOCKCHAIN_COPY, this.blockChain));
		}
	}

	private String helloPayload() {
		return PROTOCOL_VERSION + id + ":" + door;
	}

	private String[] parseHello(String hello) {
		if (hello == null || !hello.startsWith(PROTOCOL_VERSION)) return null;

		int colon = hello.lastIndexOf(':');
		if (colon <= PROTOCOL_VERSION.length() || colon == hello.length() - 1) return null;

		String remoteId = hello.substring(PROTOCOL_VERSION.length(), colon);
		if (remoteId.isEmpty()) return null;
		String port = hello.substring(colon + 1);

		try {
			int portNumber = Integer.parseInt(port);
			if (portNumber <= 0 || portNumber > 65535) return null;
		} catch (NumberFormatException e) {
			return null;
		}
		return new String[] { remoteId, port };
	}

	private String canonical(String address) {
		String[] parts = split(address);
		if (parts == null) return null;
		try {
			return InetAddress.getByName(parts[0]).getHostAddress() + ":" + parts[1];
		} catch (IOException e) {
			return null;
		}
	}

	private String[] split(String address) {
		if (address == null) return null;
		int i = address.lastIndexOf(':');
		if (i <= 0 || i == address.length() - 1) return null;
		String port = address.substring(i + 1);
		try {
			int portNumber = Integer.parseInt(port);
			if (portNumber <= 0 || portNumber > 65535) return null;
		} catch (NumberFormatException e) {
			return null;
		}
		return new String[] { address.substring(0, i), port };
	}

	private boolean itsMe(String address) {
		String[] p = split(address);
		if (p == null || Integer.parseInt(p[1]) != door) return false;
		try {
			InetAddress a = InetAddress.getByName(p[0]);
			return a.isAnyLocalAddress() || a.isLoopbackAddress()
					|| NetworkInterface.getByInetAddress(a) != null;
		} catch (IOException e) {
			return false;
		}
	}

	public static void main(String[] args) {
		if (args.length < 2) {
			System.out.println("Usage: java rede.PeerNode <port> <id> [host:port ...]");
			return;
		}

		int porta;
		try {
			porta = Integer.parseInt(args[0]);
		} catch (NumberFormatException e) {
			System.out.println("Invalid port: " + args[0]);
			return;
		}

		PeerNode node = new PeerNode(porta);
		node.id = args[1];

		// peers conhecidos vêm dos outros argumentos
		for (int i = 2; i < args.length; i++) {
			node.seeds.add(args[i]);
		}

		// Se nenhum seed foi passado, usa o seed estatico do enunciado.
		if (node.seeds.isEmpty()) {
			node.seeds.add(DEFAULT_SEED);
		}

		node.start(); // sobe o servidor e depois conecta nos seeds
	}

}
