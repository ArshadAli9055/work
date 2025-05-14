
class BPlusTree {
    private int order;
    private BPlusTreeNode root;
    
    public BPlusTree(int order) {
        this.order = order;
        this.root = new BPlusTreeLeafNode(order);
    }

    public void insert(int key, TrashBin value) {
        BPlusTreeNode newRoot = root.insert(key, value);
        if (newRoot != null) {
            root = newRoot;
        }
    }

    public Object search(int key) {
        return root.search(key);
    }

    public void update(int key, TrashBin value) {
        BPlusTreeLeafNode leaf = findLeafNode(key);
        if (leaf != null) {
            leaf.updateValue(key, value);
        }
    }

    private BPlusTreeLeafNode findLeafNode(int key) {
        BPlusTreeNode node = root;
        while (!(node instanceof BPlusTreeLeafNode)) {
            BPlusTreeInternalNode internalNode = (BPlusTreeInternalNode) node;
            node = internalNode.getChildContaining(key);
        }
        return (BPlusTreeLeafNode) node;
    }
}