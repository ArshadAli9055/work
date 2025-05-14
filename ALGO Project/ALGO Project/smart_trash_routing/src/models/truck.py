class Truck:
    """Represents a trash collection truck."""
    
    def __init__(self, truck_id, capacity, current_load=0, starting_location=(0, 0)):
        """
        Initialize a trash collection truck.
        
        Args:
            truck_id (str): Unique identifier for the truck
            capacity (float): Maximum capacity of the truck
            current_load (float): Current load of the truck
            starting_location (tuple): Starting location coordinates (x, y)
        """
        self.truck_id = truck_id
        self.capacity = capacity
        self.current_load = current_load
        self.starting_location = starting_location
        self.current_location = starting_location
        self.route = []  # List of bin IDs in order of collection
        self.bins_to_collect = {}  # Dictionary mapping bin IDs to their objects
        
    @property
    def remaining_capacity(self):
        """Return the remaining capacity of the truck."""
        return self.capacity - self.current_load
    
    @property
    def load_percentage(self):
        """Return the load percentage of the truck."""
        return (self.current_load / self.capacity) * 100 if self.capacity > 0 else 0
    
    def can_collect_bin(self, bin_obj):
        """Check if the truck can collect the given bin."""
        load_to_add = bin_obj.capacity * bin_obj.current_fill_level
        return load_to_add <= self.remaining_capacity
    
    def assign_bin(self, bin_obj):
        """
        Assign a bin to this truck's collection route without emptying it yet.
        
        Args:
            bin_obj: Bin object to assign
            
        Returns:
            bool: True if bin was assigned successfully, False otherwise
        """
        if self.can_collect_bin(bin_obj):
            # Calculate load to add
            load_to_add = bin_obj.capacity * bin_obj.current_fill_level
            
            # Update truck load
            self.current_load += load_to_add
            
            # Add bin to route and collection list
            self.route.append(bin_obj.bin_id)
            self.bins_to_collect[bin_obj.bin_id] = bin_obj
            
            return True
        return False
    
    def remove_bin(self, bin_id):
        """
        Remove a bin from this truck's route.
        
        Args:
            bin_id: ID of the bin to remove
            
        Returns:
            bool: True if bin was removed successfully, False otherwise
        """
        if bin_id in self.bins_to_collect:
            bin_obj = self.bins_to_collect[bin_id]
            load_to_subtract = bin_obj.capacity * bin_obj.current_fill_level
            
            # Update truck load
            self.current_load -= load_to_subtract
            
            # Remove bin from route and collection list
            if bin_id in self.route:
                self.route.remove(bin_id)
            del self.bins_to_collect[bin_id]
            
            return True
        return False
    
    def collect_bin(self, bin_id):
        """
        Collect trash from a bin and update truck load.
        Note: This method should only be called during actual collection,
        not during route planning.
        
        Args:
            bin_id: ID of the bin to collect
            
        Returns:
            bool: True if bin was collected successfully, False otherwise
        """
        if bin_id in self.bins_to_collect:
            bin_obj = self.bins_to_collect[bin_id]
            self.current_location = bin_obj.location
            
            # Empty the bin (would happen during actual collection)
            bin_obj.update_fill_level(0)
            
            return True
        return False
    
    def simulate_collection(self):
        """
        Simulate collection of all assigned bins.
        This empties all bins in the route without changing the truck's load.
        """
        for bin_id, bin_obj in self.bins_to_collect.items():
            bin_obj.update_fill_level(0)  # Empty the bin during actual collection
        
    def empty_truck(self):
        """Empty the truck at disposal facility."""
        self.current_load = 0
        self.bins_to_collect.clear()
        
    def reset_route(self):
        """Reset the truck's route."""
        self.route = []
        self.bins_to_collect.clear()
        self.current_load = 0
        self.current_location = self.starting_location
        
    def update_bin_fill_level(self, bin_id, new_fill_level):
        """
        Update the fill level of a bin in the route and adjust truck load.
        
        Args:
            bin_id: ID of the bin to update
            new_fill_level: New fill level (0-1) for the bin
            
        Returns:
            bool: True if bin was updated successfully, False otherwise
        """
        if bin_id in self.bins_to_collect:
            bin_obj = self.bins_to_collect[bin_id]
            old_load = bin_obj.capacity * bin_obj.current_fill_level
            
            # Update bin fill level
            bin_obj.update_fill_level(new_fill_level)
            
            # Calculate new load
            new_load = bin_obj.capacity * bin_obj.current_fill_level
            
            # Update truck load
            self.current_load = self.current_load - old_load + new_load
            
            return True
        return False
    
    def __str__(self):
        return f"Truck {self.truck_id} - {self.load_percentage:.1f}% full"
    
    def __repr__(self):
        return f"Truck(id={self.truck_id}, capacity={self.capacity}, load={self.load_percentage:.1f}%)"