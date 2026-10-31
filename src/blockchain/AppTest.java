package blockchain;
import java.util.ArrayList;

import blockchain.Block;
public class AppTest {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
//		int difficulty = 1;
//		Block genesis = new Block("Hello", "0");
//		genesis.mineBlock(difficulty);
//
//		Block block1 = new Block("Hello im the second block", genesis.hash);
//		block1.mineBlock(difficulty);
//
//		Block block2 = new Block("Hello im the third block", block1.hash);
//		block2.mineBlock(difficulty);
//		
//		System.out.println("LOG --------------------------");
//		System.out.println("Total Blocks: " + Block.total);
//		
//		System.out.println("Index Block Genesis: " + genesis.index);
//		System.out.println("Index Block 1: " + block1.index);
//		System.out.println("Index Block 2: " + block2.index);

		
		BlockChain chain = new BlockChain();
		ArrayList<Block> blockChain = chain.getBlockChain();
		
		Block blockTest1 = new Block("Hello baby boi", "0");
		chain.addToChain(blockTest1);
		
		Block blockTest2 = new Block("Hello baby boi1", blockTest1.hash);
		chain.addToChain(blockTest2);
		
		Block blockTest3 = new Block("Hello baby boi2", blockTest2.hash);
		chain.addToChain(blockTest3);
		
		Block blockTest4 = new Block("Hello baby boi3", blockTest3.hash);
		chain.addToChain(blockTest4);
		
		
		System.out.println("\nBlock 1 (genesis) hash: "+blockTest1.hash);
		System.out.println("Block 1 prev.hash: "+blockTest1.previousHash);
	
		System.out.println("Block 2 hash: " + blockTest2.hash);
		System.out.println("Block 2 prev.hash: " + blockTest2.previousHash);
	
		System.out.println("\n------- INTEGRITY TEST -------");
		BlockChain.isChainValid(blockChain);
		
		System.out.println(chain);
	}

}
