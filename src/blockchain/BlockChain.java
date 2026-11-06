package blockchain;
import java.io.Serializable;
import java.util.ArrayList;

public class BlockChain implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private ArrayList<Block> blockChain = new ArrayList<>();
	private int difficulty = 5;
	public BlockChain(){
		System.out.println("------- BlockChain created -------\n"
				+ "BlockChain current size: " + blockChain.size() + "\n");
	}
	
	public synchronized void addToChain(Block block) {
	    if (block == null) {
	        return;
	    }

	    if (blockChain.isEmpty()) {
	        if (!block.previousHash.equals("0")) {
	            System.err.println("Invalid Block: genesis previousHash must be \"0\"");
	            return;
	        }
	    } else {
	        Block lastBlock = blockChain.get(blockChain.size() - 1);
	        if (!block.previousHash.equals(lastBlock.hash)) {
	            System.err.println("Invalid Block: previousHash does not match the last block's hash");
	            return;
	        }
	    }

	    if (!block.hash.equals(block.calculateHash())) {
	        System.err.println("Invalid Block: hash does not match its content");
	        return;
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
		Block genesisBlock = chainToValidate.get(0);
		if(!genesisBlock.hash.equals(genesisBlock.calculateHash()))
			return false;
		if(!genesisBlock.previousHash.equals("0"))
			return false;
		
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
	
	public synchronized ReplaceMessage replaceChain(ArrayList<Block> receivedChain) {
        if (!BlockChain.isChainValid(receivedChain)) {
            System.err.println("INVALID BLOCKCHAIN RECEIVED");
            return ReplaceMessage.INVALIDCHAIN;
        }

        if (receivedChain.size() > this.blockChain.size()) {
            this.blockChain = receivedChain;
            System.out.println("BlockChain Updated by network. New size: " + blockChain.size()
                    + ", last: " + getLastBlock().hash);
            return ReplaceMessage.CHAINSWITCH;
        } else {
            System.out.println("Blockchain received size is less than or equal to actual Blockchain");
            return ReplaceMessage.DONTCHANGE;
        }
    }

	public static String rejectReason(ReplaceMessage result) {
        switch (result) {
            case INVALIDCHAIN:
                return "Invalid Chain";
            case DONTCHANGE:
                return "Received size is less than or equal to actual Blockchain";
            case CHAINSWITCH:
            default:
                return null;
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
