class BPlusTreeLeafNode extends BPlusTreeNode {
    private TrashBin[] values;
    private BPlusTreeLeafNode nextLeaf; // For linked list of leaf nodes
    
    public BPlusTreeLeafNode(int order) {
        super(order);
        this.values = new TrashBin[order];
        this.nextLeaf = null;
    }
    
    public void setNextLeaf(BPlusTreeLeafNode nextLeaf) {
        this.nextLeaf = nextLeaf;
    }
    
    public BPlusTreeLeafNode getNextLeaf() {
        return nextLeaf;
    }
    
    @Override
    public BPlusTreeNode insert(int key, TrashBin value) {
        int pos = findKeyPosition(key);
        if (pos < size && keys[pos] == key) {
            values[pos] = value;
            return null;
        }
        if (size < order) {
            insertKeyValue(pos, key, value);
            return null;
        }
        BPlusTreeLeafNode newLeaf = new BPlusTreeLeafNode(order);
        int midPoint = (order + 1) / 2;
        
        if (pos < midPoint) {
            for (int i = midPoint - 1, j = 0; i < order; i++, j++) {
                newLeaf.keys[j] = keys[i];
                newLeaf.values[j] = values[i];
                newLeaf.size++;
            }
            
            size = midPoint - 1;
            insertKeyValue(pos, key, value);
        } else {
            for (int i = midPoint, j = 0; i < order; i++, j++) {
                newLeaf.keys[j] = keys[i];
                newLeaf.values[j] = values[i];
                newLeaf.size++;
            }
            
            size = midPoint;
            
            pos -= midPoint;
            newLeaf.insertKeyValue(pos, key, value);
        }
        newLeaf.nextLeaf = this.nextLeaf;
        this.nextLeaf = newLeaf;
        
        return newLeaf;
    }
    
    private void insertKeyValue(int pos, int key, TrashBin value) {
        for (int i = size; i > pos; i--) {
            keys[i] = keys[i - 1];
            values[i] = values[i - 1];
        }
        
        keys[pos] = key;
        values[pos] = value;
        size++;
    }
    
    @Override
    public Object search(int key) {
        int pos = findKeyPosition(key);
        
        if (pos < size && keys[pos] == key) {
            return values[pos];
        }
        
        return null;
    }
    
    public void updateValue(int key, TrashBin value) {
        int pos = findKeyPosition(key);
        
        if (pos < size && keys[pos] == key) {
            values[pos] = value;
        }
    }
}