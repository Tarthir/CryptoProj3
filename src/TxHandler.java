import java.security.PublicKey;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class TxHandler {
    private UTXOPool utxoPool;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        this.utxoPool = utxoPool;
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool,
     * (2) the signatures on each input of {@code tx} are valid,
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     * values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        ArrayList<UTXO> utx = this.utxoPool.getAllUTXO(); //TODO need a utxo pool that is stored somewhere/static
        // 1. all outputs claimed by {@code tx} are in the current UTXO pool
        for (Transaction.Output output : tx.getOutputs()) {
            if (utx.contains(output)) {
                //3. no UTXO is claimed multiple times by {@code tx}
                Set<Transaction.Output> set_list = new LinkedHashSet<>(tx.getOutputs());
                if (set_list.size() < tx.getOutputs().size()) {
                    return false;
                }
            } else {
                return false;
            }

        }
        //2 the signatures on each input of {@code tx} are valid
        for (int i = 0; i < tx.getInputs().size(); i++) {
            PublicKey publicKey = tx.getOutput(tx.getInput(i).outputIndex).address;
            byte[] message = tx.getRawDataToSign(i);
            byte[] signature = tx.getInput(i).signature;
            if (!Crypto.verifySignature(publicKey, message, signature)) {
                return false;
            }
        }
        // 4 all of {@code tx}s output values are non-negative
        for (Transaction.Output output : tx.getOutputs()) {
            if (output.value < 0) {
                return false;
            }
        }

        // 5. the sum of {@code tx}s input values is greater than or equal to the sum of its output
        //  values; and false otherwise.
        double inputSum = 0;
        double outputSum = 0;
        for (Transaction.Input input : tx.getInputs()) {
            Transaction.Output prevOutput = tx.getOutput(input.outputIndex);
            inputSum += prevOutput.value;
        }
        for (Transaction.Output output : tx.getOutputs()) {
            outputSum += output.value;
        }
        if (outputSum < inputSum) {
            return false;
        }

        return true;
    }


    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        List<Transaction> transactions = new ArrayList<>();
        for(int i = 0; i < possibleTxs.length; i++) {
            if(isValidTx(possibleTxs[i])) {
                transactions.add(possibleTxs[i]);
                for(Transaction.Output output : possibleTxs[i].getOutputs()) {
                    utxoPool.addUTXO(new UTXO(possibleTxs[i].getHash(), i), output);
                }
            }
        }
        return (Transaction[])transactions.toArray();
    }

    public UTXOPool getUTXOPOOL() {
        return null;
    }
}