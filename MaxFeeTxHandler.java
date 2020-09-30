import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MaxFeeTxHandler {

	/*
	 * Creates a public ledger whose current UTXOPool (collection of unspent
	 * transaction outputs) is utxoPool. This should make a defensive copy of
	 * utxoPool by using the UTXOPool(UTXOPool uPool) constructor.
	 */
	private UTXOPool my_ledger;

//	private double currMax;
//	private ArrayList<Transaction> setTxWithMaxFee = new ArrayList<Transaction>();
	public MaxFeeTxHandler(UTXOPool utxoPool) {
		// IMPLEMENT THIS
		my_ledger = new UTXOPool(utxoPool);
	}

	/*
	 * Returns true if (1) all outputs claimed by tx are in the current UTXO pool,
	 * (2) the signatures on each input of tx are valid, (3) no UTXO is claimed
	 * multiple times by tx, (4) all of tx’s output values are non-negative, and
	 * (5) the sum of tx’s input values is greater than or equal to the sum of its
	 * output values; and false otherwise.
	 */

	private boolean areCoinsExistedInPool(Transaction tx) {
		ArrayList<Transaction.Input> inputs = tx.getInputs();
		ArrayList<UTXO> temp = new ArrayList<UTXO>();
		for (int i = 0; i < tx.numInputs(); i++) {
			temp.add(new UTXO(inputs.get(i).prevTxHash, inputs.get(i).outputIndex));
		}
		for (UTXO curr : temp) {
			if (!my_ledger.contains(curr)) {
				return false;
			}
		}
		return true;
	}

	private boolean areSignaturesValid(Transaction tx) {
		byte[] msg;
		byte[] sig;
		boolean isValidSig;
		for (int i = 0; i < tx.numInputs(); i++) {
			msg = tx.getRawDataToSign(i);
			Transaction.Input in = tx.getInput(i);
			sig = in.signature;
			UTXO prevUTXO = new UTXO(in.prevTxHash, in.outputIndex);
			Transaction.Output prevTxOp = my_ledger.getTxOutput(prevUTXO);
			RSAKey pKey = prevTxOp.address;
			isValidSig = pKey.verifySignature(msg, sig);
			if (!isValidSig) {
				return false;
			}
		}
		return true;
	}

	private boolean isUTXOAlreadyClaimed(Transaction tx) {
		HashSet<UTXO> claimedUTXO = new HashSet<UTXO>();

		for (int i = 0; i < tx.numInputs(); i++) {
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
		for (Transaction.Output op : outputs) {
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
		for (Transaction.Output op : outputs) {
			totalOp += op.value;
		}
		for (int i = 0; i < tx.numInputs(); i++) {
			Transaction.Input in = tx.getInput(i);
			UTXO prevUTXO = new UTXO(in.prevTxHash, in.outputIndex);
			Transaction.Output prevTxOp = my_ledger.getTxOutput(prevUTXO);
			totalIn += prevTxOp.value;
		}
		if (totalOp > totalIn) {
			return false;
		}
		return true;
	}

	public boolean isValidTx(Transaction tx) {
		// IMPLEMENT THIS
		return areCoinsExistedInPool(tx) && areSignaturesValid(tx) && isUTXOAlreadyClaimed(tx)
				&& areAllOutputPositive(tx) && checkOutputExceedsInput(tx);

	}

	public double calcSumOutput(Transaction tx) {
		double re = 0.0;
		for (int i = 0; i < tx.numOutputs(); i++) {
			Transaction.Output op = tx.getOutput(i);
			re += op.value;
		}
		return re;
	}

	public double calcSumInput(Transaction tx) {
		double re = 0.0;
		ArrayList<Transaction.Input> inputs = tx.getInputs();
		for (int i = 0; i < tx.numInputs(); i++) {
			UTXO utxo = new UTXO(inputs.get(i).prevTxHash, inputs.get(i).outputIndex);
			if (!isValidTx(tx)) { continue; } 
			Transaction.Output curr = my_ledger.getTxOutput(utxo);
			re += curr.value;
		}
		return re;
	}

	public double calcFee(double sumInput, double sumOutput) {
		return sumInput - sumOutput;
	}

	// inspired from
	// https://www.programcreek.com/2013/11/arrays-sort-comparator/
	// Custom sort on Transaction objects
	class TransactionComparator implements Comparator<Transaction> {
		@Override
		public int compare(Transaction t1, Transaction t2) {
			double sumInputT1 = calcSumInput(t1);
			double sumOutputT1 = calcSumOutput(t1);
			double feeT1 = calcFee(sumInputT1, sumOutputT1);
			
			double sumInputT2 = calcSumInput(t2);
			double sumOutputT2 = calcSumOutput(t2);
			double feeT2 = calcFee(sumInputT2, sumOutputT2);
			
			double diff = feeT2 - feeT1;
			if (diff > 0) {
				return 1; // transaction 2 has larger fee than transaction 1
			} else if (diff == 0) {
				return 0; // transaction 2 has the same fee than transaction 1
			} else {
				return -1; // transaction 2 has lesser fee than transaction 1
			}
		}
	}

	public Transaction[] handleTxs(Transaction[] possibleTxs) {
		// IMPLEMENT THIS
		// Sort to find the set of transactions that have max fee
		Arrays.sort(possibleTxs, new TransactionComparator());
		Set<Transaction> validTx = new HashSet<Transaction>(); // make sure txns are unique
		for (Transaction tx : possibleTxs) {
			if (isValidTx(tx)) {
				validTx.add(tx);

				// avoid double-spend coins by removing the unspent coin from the ledger
				ArrayList<Transaction.Input> inputs = tx.getInputs();
				for (int i = 0; i < tx.numInputs(); i++) {
					UTXO utxo = new UTXO(inputs.get(i).prevTxHash, inputs.get(i).outputIndex);
					my_ledger.removeUTXO(utxo);
				}

				// for newly created coins, we need to add them back to update the current
				// UTXOpool
				// outputs are the newly created coins in a txn
				for (int i = 0; i < tx.numOutputs(); i++) {
					Transaction.Output op = tx.getOutput(i);
					UTXO utxo = new UTXO(tx.getHash(), i);
					my_ledger.addUTXO(utxo, op);
				}
			}
		}

		// inspired from
		// https://stackoverflow.com/questions/5374311/convert-arrayliststring-to-string-array
		Transaction[] reTxns = new Transaction[validTx.size()];
		return validTx.toArray(reTxns);
	}

}
