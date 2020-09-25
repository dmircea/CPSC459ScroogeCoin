import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
	 * (4) all of tx’s output values are non-negative, and
	 * (5) the sum of tx’s input values is greater than or equal to the sum of   
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
			if(!my_ledger.contains(curr)) {
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
		
		for (int i=0; i < tx.numInputs(); i++) {
			Transaction.Input in = tx.getInput(i);
			UTXO prevUTXO = new UTXO(in.prevTxHash, in.outputIndex);
			if (claimedUTXO.contains(prevUTXO)) {
				return false;
			}
			claimedUTXO.add(prevUTXO);
		}
		return true;
	}
	
	private boolean areAllOutputPositive(Transaction tx) {
		ArrayList<Transaction.Output> outputs = tx.getOutputs();
		for (Transaction.Output op: outputs) {
			if (op.value < 0) {
				return false;
			}
		}
		return true;
	}
	
	private boolean checkOutputExceedsInput(Transaction tx) {
		ArrayList<Transaction.Output> outputs = tx.getOutputs();
		double totalOp = 0.0;
		double totalIn = 0.0;
		for (Transaction.Output op: outputs) {
			totalOp += op.value;
		}
		for (int i=0; i < tx.numInputs(); i++) {
			Transaction.Input in = tx.getInput(i);
			UTXO prevUTXO = new UTXO(in.prevTxHash, in.outputIndex);
			Transaction.Output prevTxOp= my_ledger.getTxOutput(prevUTXO);
			totalIn += prevTxOp.value;
		}
		if (totalOp > totalIn) {
			return false;
		}
		return true;
	}

	public boolean isValidTx(Transaction tx) {
		// IMPLEMENT THIS
		return areCoinsExistedInPool(tx) && 
				areSignaturesValid(tx) &&
				isUTXOAlreadyClaimed(tx) &&
				areAllOutputPositive(tx) &&
				checkOutputExceedsInput(tx);

	}

	/* Handles each epoch by receiving an unordered array of proposed 
	 * transactions, checking each transaction for correctness, 
	 * returning a mutually valid array of accepted transactions, 
	 * and updating the current UTXO pool as appropriate.
	 */
	public Transaction[] handleTxs(Transaction[] possibleTxs) {
		// IMPLEMENT THIS
		Set<Transaction> validTx = new HashSet<Transaction>(); // make sure txns are unique
		for (Transaction tx: possibleTxs) {
			if(isValidTx(tx)) {
				validTx.add(tx);
				// avoid double-spend coins by removing the unspent coin from the ledger
				ArrayList<Transaction.Input> inputs = tx.getInputs();
				for (int i=0; i < tx.numInputs(); i++) {
					UTXO utxo = new UTXO(inputs.get(i).prevTxHash, inputs.get(i).outputIndex);
					my_ledger.removeUTXO(utxo);
				}

				
				// for newly created coins, we need to add them back to update the current UTXOpool
				// outputs are the newly created coins in a txn
				for(int i=0; i < tx.numOutputs(); i++) {
					Transaction.Output op = tx.getOutput(i);
					UTXO utxo = new UTXO(tx.getHash(), i);
					my_ledger.addUTXO(utxo, op);
				}
			}
		}
		// inspired from https://stackoverflow.com/questions/5374311/convert-arrayliststring-to-string-array
		Transaction[] reTxns = new Transaction[validTx.size()];
		return validTx.toArray(reTxns);
	}

} 
