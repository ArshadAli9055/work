import networkx as nx
import math

class City:
    """Represents a city with bin locations and road network."""
    
    def __init__(self):
        """Initialize an empty city graph."""
        self.graph = nx.Graph()
        self.bins = {}  # Dictionary of bin_id -> Bin object
        self.disposal_facilities = []  # List of disposal facility locations
        
    def add_bin(self, bin_obj):
        """
        Add a bin to the city.
        
        Args:
            bin_obj: Bin object to add
        """
        self.bins[bin_obj.bin_id] = bin_obj
        # Add the bin as a node in the graph with its location as attribute
        self.graph.add_node(bin_obj.bin_id, pos=bin_obj.location, 
                           fill_level=bin_obj.current_fill_level,
                           is_high_priority=bin_obj.is_high_priority)
    
    def add_road(self, bin_id1, bin_id2, distance=None):
        """
        Add a road between two bins.
        
        Args:
            bin_id1 (str): ID of the first bin
            bin_id2 (str): ID of the second bin
            distance (float, optional): Road distance. If None, calculate Euclidean distance.
        """
        if bin_id1 not in self.bins or bin_id2 not in self.bins:
            raise ValueError("Bin IDs must be valid")
            
        # Calculate Euclidean distance if not provided
        if distance is None:
            loc1 = self.bins[bin_id1].location
            loc2 = self.bins[bin_id2].location
            distance = math.sqrt((loc1[0] - loc2[0])**2 + (loc1[1] - loc2[1])**2)
            
        # Add edge with distance attribute
        self.graph.add_edge(bin_id1, bin_id2, weight=distance)
    
    def add_disposal_facility(self, location):
        """
        Add a disposal facility to the city.
        
        Args:
            location (tuple): (x, y) coordinates of the facility
        """
        facility_id = f"facility_{len(self.disposal_facilities)}"
        self.disposal_facilities.append((facility_id, location))
        
        # Add the facility as a node in the graph
        self.graph.add_node(facility_id, pos=location, is_facility=True)
        
        # Connect the facility to all bins with appropriate distances
        for bin_id, bin_obj in self.bins.items():
            distance = math.sqrt((location[0] - bin_obj.location[0])**2 + 
                               (location[1] - bin_obj.location[1])**2)
            self.graph.add_edge(facility_id, bin_id, weight=distance)
    
    def get_closest_facility(self, location):
        """
        Get the closest disposal facility to a given location.
        
        Args:
            location (tuple): (x, y) coordinates
            
        Returns:
            tuple: (facility_id, facility_location, distance)
        """
        if not self.disposal_facilities:
            raise ValueError("No disposal facilities in the city")
            
        closest = None
        min_distance = float('inf')
        
        for facility_id, facility_loc in self.disposal_facilities:
            distance = math.sqrt((location[0] - facility_loc[0])**2 + 
                               (location[1] - facility_loc[1])**2)
            if distance < min_distance:
                min_distance = distance
                closest = (facility_id, facility_loc, min_distance)
                
        return closest
    
    def get_shortest_path(self, bin_id1, bin_id2):
        """
        Get the shortest path between two bins.
        
        Args:
            bin_id1 (str): ID of the first bin
            bin_id2 (str): ID of the second bin
            
        Returns:
            list: List of bin IDs in the shortest path
            float: Total distance of the path
        """
        if bin_id1 not in self.graph or bin_id2 not in self.graph:
            raise ValueError("Bin IDs must be valid nodes in the graph")
            
        try:
            path = nx.shortest_path(self.graph, bin_id1, bin_id2, weight='weight')
            distance = nx.shortest_path_length(self.graph, bin_id1, bin_id2, weight='weight')
            return path, distance
        except nx.NetworkXNoPath:
            return None, float('inf')
    
    def get_high_priority_bins(self):
        """Return a list of high priority bin IDs (bins that are nearly full)."""
        return [bin_id for bin_id, bin_obj in self.bins.items() if bin_obj.is_high_priority]
    
    def __str__(self):
        return f"City with {len(self.bins)} bins and {len(self.disposal_facilities)} disposal facilities"