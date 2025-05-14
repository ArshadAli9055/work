class Bin:
    """Represents a trash bin in the city."""
    
    def __init__(self, bin_id, location, capacity, current_fill_level=0):
        """
        Initialize a trash bin.
        
        Args:
            bin_id (str): Unique identifier for the bin
            location (tuple): (x, y) coordinates of the bin
            capacity (float): Maximum capacity of the bin
            current_fill_level (float): Current fill level (0-1 representing percentage)
        """
        self.bin_id = bin_id
        self.location = location
        self.capacity = capacity
        self.current_fill_level = current_fill_level
        
    @property
    def is_high_priority(self):
        """Return True if bin is nearly full (>75% capacity)."""
        return self.current_fill_level > 0.75
    
    @property
    def fill_percentage(self):
        """Return the fill percentage of the bin."""
        return self.current_fill_level * 100
    
    @property
    def remaining_capacity(self):
        """Return the remaining capacity of the bin."""
        return self.capacity * (1 - self.current_fill_level)
    
    def update_fill_level(self, new_fill_level):
        """Update the fill level of the bin."""
        if 0 <= new_fill_level <= 1:
            self.current_fill_level = new_fill_level
        else:
            raise ValueError("Fill level must be between 0 and 1")
            
    def __str__(self):
        return f"Bin {self.bin_id} at {self.location} - {self.fill_percentage:.1f}% full"
    
    def __repr__(self):
        return f"Bin(id={self.bin_id}, loc={self.location}, fill={self.fill_percentage:.1f}%)"