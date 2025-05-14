abstract class BPlusTreeNode {
    protected int order;
    protected int size;
    protected int[] keys;
    
    public BPlusTreeNode(int order) {
        this.order = order;
        this.size = 0;
        this.keys = new int[order];
    }
    
    public abstract BPlusTreeNode insert(int key, TrashBin value);
    
    public abstract Object search(int key);
    
    protected int findKeyPosition(int key) {
        int pos = 0;
        while (pos < size && keys[pos] < key) {
            pos++;
        }
        return pos;
    }
}
