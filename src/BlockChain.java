// Block Chain should maintain only limited block nodes to satisfy the functions
// You should not have all the blocks added to the block chain in memory
// as it would cause a memory overflow.

import java.util.ArrayList;
import java.util.HashMap;

public class BlockChain {
    public static final int CUT_OFF_AGE = 10;
    TransactionPool TxPool = null;
    // Our blockchain, which is the hash of the block pointing to the node it belongs to
    // The Node holds the info on its children/parent/height
    HashMap<ByteArrayWrapper, Node> blockChain;
    // The node that is at the current "max height"
    Node maxH;

    /**
     * create an empty block chain with just a genesis block. Assume {@code genesisBlock} is a valid
     * block
     */
    public BlockChain(Block genesisBlock) {
        // IMPLEMENT THIS
        this.TxPool = new TransactionPool();
        blockChain = new HashMap<>();
        UTXOPool pool = new UTXOPool();
        putInUTXOPOOL(genesisBlock, pool);
        Node g = new Node(genesisBlock,null, pool);
        blockChain.put(new ByteArrayWrapper(genesisBlock.getHash()), g);
        maxH = g;
    }

    /** Get the maximum height block */
    public Block getMaxHeightBlock() {
        return maxH.block;
    }

    /** Get the UTXOPool for mining a new block on top of max height block */
    public UTXOPool getMaxHeightUTXOPool() {
        return maxH.getUPool();
    }

    /** Get the transaction pool to mine a new block */
    public TransactionPool getTransactionPool() {
        return TxPool;
    }

    /**
     * Add {@code block} to the block chain if it is valid. For validity, all transactions should be
     * valid and block should be at {@code height > (maxHeight - CUT_OFF_AGE)}.
     *
     * <p>
     * For example, you can try creating a new block over the genesis block (block height 2) if the
     * block chain height is {@code <=
     * CUT_OFF_AGE + 1}. As soon as {@code height > CUT_OFF_AGE + 1}, you cannot create a new block
     * at height 2.
     *
     * @return true if block is successfully added
     */
    public boolean addBlock(Block block) {
        if (block == null){
            return false;
        }
        // if someone if trying to overwrite the genesis block
        if(block.getPrevBlockHash() == null) {
            return false;
        }

        Node parent = blockChain.get(new ByteArrayWrapper(block.getPrevBlockHash()));

        //if this block does not have a parent
        if (parent == null){
            return false;
        }
        TxHandler handler = new TxHandler(parent.getUPool());
        // check for valid transactions
        Transaction[] valid = handler.handleTxs(block.getTransactions().toArray(new Transaction[0]));
        if (valid.length != block.getTransactions().size()){
            return false;
        }
        // check height
        if (parent.block_height + 1 <= maxH.block_height - CUT_OFF_AGE){
            return false;
        }

        UTXOPool pool = handler.getUTXOPool();
        putInUTXOPOOL(block, pool);
        Node new_node = new Node(block,parent,pool);
        blockChain.put(new ByteArrayWrapper(block.getHash()),new_node);
        // new max height node? if the new height it greater than curr max height
        if (parent.block_height + 1 > maxH.block_height){
            maxH = new_node;
        }
        return true;
    }

    private void putInUTXOPOOL(Block block, UTXOPool pool) {
        for (int i = 0; i < block.getCoinbase().numOutputs(); i++) {
            UTXO xo = new UTXO(block.getCoinbase().getHash(), i);
            pool.addUTXO(xo, block.getCoinbase().getOutput(i));
        }
    }


    /** Add a transaction to the transaction pool */
    public void addTransaction(Transaction tx) {
        TxPool.addTransaction(tx);
    }



    // Holds meta data for each block
    class Node{
        Block block;
        Node parent; // the block above it
        ArrayList<Node> children; // its kids
        int block_height; // the height of the block
        private UTXOPool u_pool; // the unspent transaction pool for this node

        public Node(Block block, Node parent, UTXOPool u_pool ) {
            this.u_pool = u_pool;
            this.block = block;
            this.parent = parent;
            if(parent != null) {
                block_height = parent.block_height + 1;
                parent.children.add(this);
            }
            else{
                block_height = 1;
            }
            this.children = new ArrayList<>();
        }

        UTXOPool getUPool(){
            return new UTXOPool(u_pool);
        }
    }
}