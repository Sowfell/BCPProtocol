package blockchain;
import java.util.ArrayList;

public class BlockChain {
	private ArrayList<Block> blockChain = new ArrayList<>();
	private int difficulty = 5;
	public BlockChain(){
		System.out.println("------- BlockChain created -------\n"
				+ "BlockChain current size: " + blockChain.size() + "\n");
	}
	
	public void addToChain(Block block) {
		if(block==null) {
			return;
		}
		if(blockChain.size() == 0) {
			if(!block.previousHash.equals("0")) {
				System.err.println("Invalid Block: Previous Hash is different to expected");
				return;
			}
		}
		block.mineBlock(difficulty);
		blockChain.add(block);
		block.index = blockChain.size();
		System.out.println("\nBlock: " + block.index + " correctly added to BlockChain");
		System.out.println("Currently blockchain size: " + blockChain.size() + "\n");
		
	}
	
	public static boolean isChainValid(ArrayList<Block> chainToValidate) {
		if (chainToValidate == null || 
                chainToValidate.isEmpty()) {
            return false;
        }
		Block currentBlock;
		Block previousBlock;
		
		for(int i = 1; i<chainToValidate.size();i++) {
			currentBlock = chainToValidate.get(i);
			previousBlock = chainToValidate.get(i-1);
			
			if(!currentBlock.hash.equals(currentBlock.calculateHash())) {
				System.out.println("Current Hashes is not equals");
				return false;
			}
			if(!currentBlock.previousHash.equals(previousBlock.hash)) {
				System.out.println("Previous Hashes is not equals");
				return false;
			}
		}
		System.out.println("INTEGRITY TEST SUCCESFULLY COMPLETED\n");
		return true;
	}
	
	public boolean replaceChain(ArrayList<Block> receivedChain) {
		if(!BlockChain.isChainValid(receivedChain)) {
			System.err.println("INVALID BLOCKCHAIN RECEIVED");
			return false;
		}
		
		if(receivedChain.size()>this.blockChain.size()) {
			this.blockChain = receivedChain;
			System.out.println("BlockChain Updated by network. New size: " + blockChain.size());
			return true;
		}
		else {
			System.out.println("Blockchain received size is less than or equal to actual Blockchain");
			return false;
		}
		
	}
	
	public ArrayList<Block> getBlockChain(){
		return this.blockChain;
	}
	
	public Block getLastBlock() {    //AQUI Ó, RESGATA A BLOCKCHAIN
		if(this.blockChain.isEmpty())
			return null;
		return this.blockChain.getLast();
	}

	@Override
	public String toString(){
		String text = "";
		for(Block block : this.blockChain) {
			text = text.concat(block.toString()) + "\n";
		}
		return text;
	}
}
