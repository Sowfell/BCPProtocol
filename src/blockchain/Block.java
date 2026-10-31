package blockchain;
import java.util.Date;
import java.io.Serializable;
public class Block implements Serializable {
	private static final long serialVersionUID = 1L;
	
	public int index; //block index
	public String hash;
	public String data;
	public String previousHash;
	public long timeStamp;
	public int nonce;
	
	public Block(String data, String previousBlockHash) {
		this.data=data;
		this.previousHash=previousBlockHash;
		this.timeStamp= new Date().getTime();
		this.hash = calculateHash();
	}
	
	public String calculateHash() {
		String calculatedHash = StringUtil.applySha256(previousHash + data + Long.toString(timeStamp) + Integer.toString(nonce));
		return calculatedHash;
	}
	
	public void mineBlock(int difficulty) {
		String target = new String(new char[difficulty]).replace('\0', '0'); //Create a string with difficulty zeros
		while(!hash.substring( 0, difficulty).equals(target)) { //While hash string[0 to difficulty - 1] is not equal to target
		nonce ++;
		hash = calculateHash();
		}
		System.out.println("Block Mined!!! : " + hash);
		}
	@Override
	public String toString() {
			String text = "Block Nº: " + this.index + "\n"
					+ "Block Hash: " + this.hash + "\n"
					+ "Block Data: " + this.data + "\n"
					+ "Block Previous Hash: " + this.previousHash + "\n";
			return text;
	}
}
