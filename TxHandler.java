import java.util.ArrayList;
import java.util.HashSet;

public class TxHandler {

	/* Creates a public ledger whose current UTXOPool (collection of unspent 
	 * transaction outputs) is utxoPool. This should make a defensive copy of 
	 * utxoPool by using the UTXOPool(UTXOPool uPool) constructor.
	 */
	private UTXOPool my_ledger;
	public TxHandler(UTXOPool utxoPool) {
		// IMPLEMENT THIS
		my_ledger = new UTXOPool(utxoPool); 
	}

	/* Returns true if 
	 * (1) all outputs claimed by tx are in the current UTXO pool, 
	 * (2) the signatures on each input of tx are valid, 
	 * (3) no UTXO is claimed multiple times by tx, 
	 * (4) all of txâ€™s output values are non-negative, and
	 * (5) the sum of txâ€™s input values is greater than or equal to the sum of   
	        its output values;
	   and false otherwise.
	 */
	
	private boolean areCoinsExistedInPool(Transaction tx) {
		ArrayList<Transaction.Input> inputs = tx.getInputs();
		ArrayList<UTXO> temp = new ArrayList<UTXO>();
		for (int i = 0; i < tx.numInputs(); i++) {
			temp.add(new UTXO(inputs.get(i).prevTxHash, inputs.get(i).outputIndex));
		}
		for (UTXO curr: temp) {
			if(!my_ledger.contains(curr
					)) {
				return false;
			}
		}
		return true;
	}
	
	private boolean areSignaturesValid(Transaction tx) {
		byte[] msg;
		byte[] sig;
		boolean isValidSig;
		for (int i=0; i < tx.numInputs(); i ++) {
			msg = tx.getRawDataToSign(i);
			Transaction.Input in = tx.getInput(i);
			sig = in.signature;
			UTXO prevUTXO = new UTXO(in.prevTxHash, in.outputIndex);
			Transaction.Output prevTxOp= my_ledger.getTxOutput(prevUTXO);
			RSAKey pKey = prevTxOp.address; 
			isValidSig = pKey.verifySignature(msg, sig);
			if (!isValidSig) {
				return false;
			}
		}
		return true;
	}
	
	private boolean isUTXOAlreadyClaimed(Transaction tx) {
		HashSet<UTXO> claimedUTXO= new HashSet<UTXO>();
		
		for (int i=0; i< tx.numInputs(); i++) {
			Transaction.Input in = tx.getInput(i);
			UTXO prevUTXO = new UTXO(in.prevTxHash, in.outputIndex);
			if (claimedUTXO.contains(prevUTXO)) {
				return false;
			}
			claimedUTXO.add(prevUTXO);
		}
		return true;
	}

	public boolean isValidTx(Transaction tx) {
		// IMPLEMENT THIS
		return areCoinsExistedInPool(tx) && 
				areSignaturesValid(tx) &&
				isUTXOAlreadyClaimed(tx);
		
//		return true;
	}

	/* Handles each epoch by receiving an unordered array of proposed 
	 * transactions, checking each transaction for correctness, 
	 * returning a mutually valid array of accepted transactions, 
	 * and updating the current UTXO pool as appropriate.
	 */
	public Transaction[] handleTxs(Transaction[] possibleTxs) {
		// IMPLEMENT THIS
		for (Transaction tx: possibleTxs) {
			if(!isValidTx(tx)) {
				return null;
			}
		}
		return possibleTxs;
	}

} 
