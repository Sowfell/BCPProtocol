package rede;

import java.util.Random;

import blockchain.Block;

/**
 * Thread de mineracao de um peer.
 * Fica tentando criar um novo bloco em intervalos aleatorios; quando consegue,
 * adiciona na blockchain local (via PeerNode.mineBlock) e faz o broadcast.
 */
public class Miner extends Thread {

	private final PeerNode node;
	private final long intervalMs;
	private final long jitterMs;
	private final Random random = new Random();
	private volatile boolean running = true;
	
	public Miner(PeerNode node, String name, long intervalMs, long jitterMs) {
		this.node = node;
		this.intervalMs = intervalMs;
		this.jitterMs = jitterMs;
		setName("miner-" + name);
		setDaemon(true);
	}
	
	@Override
	public void run() {
		System.out.println("Miner started (interval " + intervalMs + "ms + jitter " + jitterMs + "ms)");
		
		while (running) {
			try {
				long wait = intervalMs + (jitterMs > 0 ? random.nextLong(jitterMs) : 0);
				Thread.sleep(wait);
				if (!running) break;
				
				Block block = node.mineBlock();
				if (block != null) {
					System.out.println("Novo bloco minerado: #" + block.index + " (" + block.hash + ")");
					node.broadcastBlockchain();
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return;
			} catch (RuntimeException e) {
				System.err.println("Mining error: " + e.getMessage());
			}
		}
	}
	
	public void shutdown() {
		running = false;
		interrupt();
	}
	
}
