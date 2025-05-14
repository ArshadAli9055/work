
class BPlusTreeInternalNode extends BPlusTreeNode {
    private BPlusTreeNode[] children;
    
    public BPlusTreeInternalNode(int order) {
        super(order);
        this.children = new BPlusTreeNode[order + 1];
    }
    
    public BPlusTreeNode getChildContaining(int key) {
        int pos = 0;
        while (pos < size && key >= keys[pos]) {
            pos++;
        }
        return children[pos];
    }
    
    @Override
    public BPlusTreeNode insert(int key, TrashBin value) {
        int pos = findKeyPosition(key);
        
        if (pos < size && keys[pos] == key) {
            pos++; // If key exists, insert into the right child
        }
        
        BPlusTreeNode child = children[pos];
        BPlusTreeNode newChild = child.insert(key, value);
        
        if (newChild == null) {
            return null; // No split happened
        }
        if (newChild instanceof BPlusTreeInternalNode) {
            return insertInternalNode(pos, (BPlusTreeInternalNode) newChild);
        } else {
            return insertLeafNode(pos, (BPlusTreeLeafNode) newChild);
        }
    }
    
    private BPlusTreeNode insertInternalNode(int pos, BPlusTreeInternalNode newChild) {
        int newKey = newChild.keys[0];
        if (size < order) {
            insertKeyAndChild(pos, newKey, newChild);
            return null;
        }
        BPlusTreeInternalNode newNode = new BPlusTreeInternalNode(order);
        int midPoint = order / 2;
        if (pos < midPoint) {
            for (int i = midPoint - 1, j = 0; i < order; i++, j++) {
                newNode.keys[j] = keys[i];
                newNode.children[j] = children[i];
                newNode.size++;
            }
            newNode.children[newNode.size] = children[order];
            
            size = midPoint;
            insertKeyAndChild(pos, newKey, newChild);
        } else {
            for (int i = midPoint, j = 0; i < order; i++, j++) {
                newNode.keys[j] = keys[i];
                newNode.children[j] = children[i];
                newNode.size++;
            }
            newNode.children[newNode.size] = children[order];
            
            size = midPoint;
            
            if (pos == midPoint) {
                newNode.insertKeyAndChild(0, newKey, newChild);
            } else {
                pos -= midPoint;
                newNode.insertKeyAndChild(pos, newKey, newChild);
            }
        }
        BPlusTreeInternalNode newRoot = new BPlusTreeInternalNode(order);
        newRoot.keys[0] = newNode.keys[0];
        newRoot.children[0] = this;
        newRoot.children[1] = newNode;
        newRoot.size = 1;
        
        return newRoot;
    }
    
    private BPlusTreeNode insertLeafNode(int pos, BPlusTreeLeafNode newLeaf) {
        int newKey = newLeaf.keys[0];
        if (size < order) {
            insertKeyAndChild(pos, newKey, newLeaf);
            return null;
        }
        BPlusTreeInternalNode newNode = new BPlusTreeInternalNode(order);
        int midPoint = order / 2;
        if (pos < midPoint) {
            for (int i = midPoint - 1, j = 0; i < order; i++, j++) {
                newNode.keys[j] = keys[i];
                newNode.children[j] = children[i];
                newNode.size++;
            }
            newNode.children[newNode.size] = children[order];
            
            size = midPoint;
            insertKeyAndChild(pos, newKey, newLeaf);
        } else {
            for (int i = midPoint, j = 0; i < order; i++, j++) {
                newNode.keys[j] = keys[i];
                newNode.children[j] = children[i];
                newNode.size++;
            }
            newNode.children[newNode.size] = children[order];
            
            size = midPoint;
            
            if (pos == midPoint) {
                newNode.insertKeyAndChild(0, newKey, newLeaf);
            } else {
                pos -= midPoint;
                newNode.insertKeyAndChild(pos, newKey, newLeaf);
            }
        }
        BPlusTreeInternalNode newRoot = new BPlusTreeInternalNode(order);
        newRoot.keys[0] = newNode.keys[0];
        newRoot.children[0] = this;
        newRoot.children[1] = newNode;
        newRoot.size = 1;
        
        return newRoot;
    }
    
    private void insertKeyAndChild(int pos, int key, BPlusTreeNode child) {
        // Shift keys and children to make room
        for (int i = size; i > pos; i--) {
            keys[i] = keys[i - 1];
            children[i + 1] = children[i];
        }
        
        keys[pos] = key;
        children[pos + 1] = child;
        size++;
    }
    
    @Override
    public Object search(int key) {
        return getChildContaining(key).search(key);
    }
}