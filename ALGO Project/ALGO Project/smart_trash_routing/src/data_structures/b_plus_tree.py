class BPlusNode:
    """Base node class for B+ Tree."""
    def __init__(self, order):
        self.order = order
        self.keys = []
        self.parent = None
        self.is_leaf = True

class BPlusLeafNode(BPlusNode):
    """Leaf node for B+ Tree that stores actual data."""
    def __init__(self, order):
        super().__init__(order)
        self.next_leaf = None
        self.values = []  # Data values stored at this leaf

class BPlusInternalNode(BPlusNode):
    """Internal node for B+ Tree that stores keys and child pointers."""
    def __init__(self, order):
        super().__init__(order)
        self.is_leaf = False
        self.children = []  # Pointers to child nodes

class BPlusTree:
    """B+ Tree implementation for indexing bins by attributes."""
    
    def __init__(self, order=5):
        """
        Initialize an empty B+ Tree.
        
        Args:
            order (int): The order of the B+ tree (max number of children per node)
        """
        self.root = BPlusLeafNode(order)  # Start with a leaf node as root
        self.order = order
    
    def insert(self, key, value):
        """
        Insert a key-value pair into the B+ Tree.
        
        Args:
            key: The key to insert (e.g., bin_id, location)
            value: The value to associate with the key (e.g., Bin object)
        """
        # Find the leaf node where the key should be inserted
        leaf_node = self._find_leaf(key)
        
        # Insert the key-value pair into the leaf node
        self._insert_in_leaf(leaf_node, key, value)
        
        # If the leaf node is full, split it
        if len(leaf_node.keys) > self.order - 1:
            self._split_leaf(leaf_node)
    
    def _find_leaf(self, key):
        """Find the leaf node where the key should be inserted or found."""
        current = self.root
        
        # Traverse the tree until we reach a leaf node
        while not current.is_leaf:
            i = 0
            while i < len(current.keys) and key >= current.keys[i]:
                i += 1
            current = current.children[i]
        
        return current
    
    def _insert_in_leaf(self, leaf_node, key, value):
        """Insert key-value pair in the correct position within a leaf node."""
        i = 0
        while i < len(leaf_node.keys) and key > leaf_node.keys[i]:
            i += 1
            
        # Insert key and value at position i
        leaf_node.keys.insert(i, key)
        leaf_node.values.insert(i, value)
    
    def _split_leaf(self, leaf_node):
        """Split a leaf node that has become too full."""
        # Create a new leaf node
        new_leaf = BPlusLeafNode(self.order)
        
        # Find the midpoint
        mid = (self.order) // 2
        
        # Move half the keys and values to the new leaf
        new_leaf.keys = leaf_node.keys[mid:]
        new_leaf.values = leaf_node.values[mid:]
        
        # Update the original leaf
        leaf_node.keys = leaf_node.keys[:mid]
        leaf_node.values = leaf_node.values[:mid]
        
        # Set up the leaf chain
        new_leaf.next_leaf = leaf_node.next_leaf
        leaf_node.next_leaf = new_leaf
        
        # Update parent nodes
        self._insert_in_parent(leaf_node, new_leaf.keys[0], new_leaf)
    
    def _insert_in_parent(self, left_node, key, right_node):
        """Insert a key and pointer to right_node in parent after splitting."""
        parent = left_node.parent
        
        # If there's no parent (the node was the root), create a new root
        if parent is None:
            new_root = BPlusInternalNode(self.order)
            new_root.keys = [key]
            new_root.children = [left_node, right_node]
            self.root = new_root
            
            # Update parent pointers
            left_node.parent = new_root
            right_node.parent = new_root
            return
            
        # Insert in the parent
        i = 0
        while i < len(parent.keys) and key > parent.keys[i]:
            i += 1
            
        parent.keys.insert(i, key)
        parent.children.insert(i + 1, right_node)
        right_node.parent = parent
        
        # If the parent is too full, split it
        if len(parent.keys) > self.order - 1:
            self._split_internal(parent)
    
    def _split_internal(self, internal_node):
        """Split an internal node that has become too full."""
        # Create a new internal node
        new_internal = BPlusInternalNode(self.order)
        
        # Find the midpoint
        mid = self.order // 2
        
        # Extract the middle key
        mid_key = internal_node.keys[mid]
        
        # Move half the keys and children to the new internal node
        new_internal.keys = internal_node.keys[mid+1:]
        new_internal.children = internal_node.children[mid+1:]
        
        # Update parent pointers for moved children
        for child in new_internal.children:
            child.parent = new_internal
            
        # Update the original internal node
        internal_node.keys = internal_node.keys[:mid]
        internal_node.children = internal_node.children[:mid+1]
        
        # Insert the middle key in the parent
        self._insert_in_parent(internal_node, mid_key, new_internal)
    
    def search(self, key):
        """
        Search for a key in the B+ Tree.
        
        Args:
            key: The key to search for
            
        Returns:
            The value associated with the key, or None if not found
        """
        leaf_node = self._find_leaf(key)
        
        try:
            index = leaf_node.keys.index(key)
            return leaf_node.values[index]
        except ValueError:
            return None
    
    def search_range(self, start_key, end_key):
        """
        Search for all values with keys in the given range.
        
        Args:
            start_key: The lower bound of the range (inclusive)
            end_key: The upper bound of the range (inclusive)
            
        Returns:
            A list of values with keys in the range
        """
        result = []
        
        # Find the leaf node containing the start key
        current = self._find_leaf(start_key)
        
        # Traverse the leaf nodes and collect values within the range
        while current is not None:
            for i, key in enumerate(current.keys):
                if start_key <= key <= end_key:
                    result.append(current.values[i])
                elif key > end_key:
                    return result
            
            current = current.next_leaf
        
        return result
    
    def update(self, key, value):
        """
        Update the value associated with a key.
        
        Args:
            key: The key to update
            value: The new value
            
        Returns:
            True if the key was found and updated, False otherwise
        """
        leaf_node = self._find_leaf(key)
        
        try:
            index = leaf_node.keys.index(key)
            leaf_node.values[index] = value
            return True
        except ValueError:
            return False
    
    def delete(self, key):
        """
        Delete a key-value pair from the B+ Tree.
        
        Args:
            key: The key to delete
            
        Returns:
            True if the key was found and deleted, False otherwise
            
        Note: This is a simplified deletion that doesn't handle merging of nodes.
        """
        leaf_node = self._find_leaf(key)
        
        try:
            index = leaf_node.keys.index(key)
            leaf_node.keys.pop(index)
            leaf_node.values.pop(index)
            return True
        except ValueError:
            return False

    def __str__(self):
        """Return a string representation of the B+ Tree."""
        return self._print_tree(self.root, 0)
    
    def _print_tree(self, node, level):
        """Helper method to print the tree structure."""
        result = ""
        indent = "  " * level
        
        if node.is_leaf:
            result += f"{indent}Leaf Node: {node.keys}\n"
        else:
            result += f"{indent}Internal Node: {node.keys}\n"
            for child in node.children:
                result += self._print_tree(child, level + 1)
                
        return result