import numpy as np
from src.algorithms.dynamic_programming import optimal_bin_sequence

class RouteOptimizer:
    """Class for optimizing existing routes."""
    
    def __init__(self, city):
        """
        Initialize the route optimizer.
        
        Args:
            city: City object with graph and bin information
        """
        self.city = city
    
    def optimize_truck_routes(self, routes):
        """
        Optimize routes for multiple trucks.
        
        Args:
            routes: List of routes, where each route is a dict with truck, bins, etc.
            
        Returns:
            list: Optimized routes
        """
        optimized_routes = []
        
        for route in routes:
            # Clone the route
            optimized_route = route.copy()
            
            # Optimize the sequence
            if len(route['bins']) > 1:
                sequence, distance = optimal_bin_sequence(self.city, route['bins'])
                optimized_route['bins'] = sequence
                optimized_route['distance'] = distance
            
            optimized_routes.append(optimized_route)
        
        return optimized_routes
    
    def balance_loads(self, routes, trucks):
        """
        Balance the loads among trucks to improve overall efficiency.
        
        Args:
            routes: List of routes, where each route is a dict with truck, bins, etc.
            trucks: List of Truck objects
            
        Returns:
            list: Updated routes with balanced loads
        """
        # First, reset truck loads and routes
        for truck in trucks:
            truck.empty_truck()
            truck.reset_route()
        
        # Collect all bins from all routes
        all_bins = []
        for route in routes:
            all_bins.extend([(bin_id, self.city.bins[bin_id]) for bin_id in route['bins']])
        
        # Sort bins by fill level (descending)
        all_bins.sort(key=lambda x: x[1].current_fill_level, reverse=True)
        
        # Initialize new routes
        new_routes = []
        bins_assigned = set()
        
        # Assign bins to trucks
        while all_bins and any(truck.remaining_capacity > 0 for truck in trucks):
            # Find the truck with the most remaining capacity
            best_truck = max(trucks, key=lambda t: t.remaining_capacity)
            
            # Find bins that can be collected by this truck
            truck_bins = []
            
            for bin_id, bin_obj in all_bins:
                if bin_id not in bins_assigned and best_truck.can_collect_bin(bin_obj):
                    truck_bins.append(bin_id)
                    bins_assigned.add(bin_id)
                    best_truck.collect_bin(bin_obj)
            
            # Remove assigned bins from all_bins
            all_bins = [(bin_id, bin_obj) for bin_id, bin_obj in all_bins if bin_id not in bins_assigned]
            
            # If bins were assigned to this truck, create a route
            if truck_bins:
                # Optimize the sequence
                sequence, distance = optimal_bin_sequence(self.city, truck_bins)
                
                new_routes.append({
                    'truck': best_truck,
                    'bins': sequence,
                    'distance': distance,
                    'priority_bins': sum(1 for bin_id in truck_bins if self.city.bins[bin_id].is_high_priority)
                })
        
        return new_routes
    
    def calculate_fuel_consumption(self, routes, fuel_rate=0.1):
        """
        Calculate fuel consumption for each route.
        
        Args:
            routes: List of routes, where each route is a dict with truck, bins, etc.
            fuel_rate: Fuel consumption rate (liters per km)
            
        Returns:
            dict: Dictionary mapping route index to fuel consumption
        """
        fuel_consumption = {}
        
        for i, route in enumerate(routes):
            # Calculate fuel consumption based on distance and truck load
            distance = route['distance']
            avg_load = route['truck'].load_percentage / 2  # Approximate average load during the route
            
            # Adjust fuel rate based on load (higher load means higher consumption)
            adjusted_fuel_rate = fuel_rate * (1 + (avg_load / 100) * 0.3)
            
            fuel_consumption[i] = distance * adjusted_fuel_rate
        
        return fuel_consumption
    
    def optimize_for_priority(self, routes):
        """
        Optimize routes to prioritize high-priority bins.
        
        Args:
            routes: List of routes, where each route is a dict with truck, bins, etc.
            
        Returns:
            list: Optimized routes with high-priority bins visited earlier
        """
        optimized_routes = []
        
        for route in routes:
            # Clone the route
            optimized_route = route.copy()
            bins = route['bins']
            
            if len(bins) <= 2:
                optimized_routes.append(optimized_route)
                continue
            
            # Separate high-priority and normal bins
            high_priority = [bin_id for bin_id in bins if self.city.bins[bin_id].is_high_priority]
            normal = [bin_id for bin_id in bins if not self.city.bins[bin_id].is_high_priority]
            
            if not high_priority or not normal:
                # If all bins are of the same priority, optimize normally
                optimized_routes.append(optimized_route)
                continue
            
            # Optimize the sequence of high-priority bins
            hp_sequence, hp_distance = optimal_bin_sequence(self.city, high_priority)
            
            # Optimize the sequence of normal bins
            normal_sequence, normal_distance = optimal_bin_sequence(self.city, normal)
            
            # Try two approaches:
            # 1. Visit all high-priority bins first, then normal bins
            # 2. Visit some high-priority bins, then some normal bins, then remaining high-priority bins
            
            # Approach 1
            sequence1 = hp_sequence + normal_sequence
            distance1 = hp_distance + normal_distance
            
            # Add the distance between the last high-priority bin and the first normal bin
            if hp_sequence and normal_sequence:
                try:
                    distance1 += self.city.graph[hp_sequence[-1]][normal_sequence[0]]['weight']
                except KeyError:
                    _, dist = self.city.get_shortest_path(hp_sequence[-1], normal_sequence[0])
                    distance1 += dist
            
            # Approach 2: Split high-priority bins into two groups
            if len(high_priority) >= 4:
                mid = len(high_priority) // 2
                hp_first = hp_sequence[:mid]
                hp_second = hp_sequence[mid:]
                
                sequence2 = hp_first + normal_sequence + hp_second
                
                # Calculate distance for this sequence
                distance2 = self._calculate_sequence_distance(sequence2)
                
                # Choose the better approach
                if distance2 < distance1:
                    optimized_route['bins'] = sequence2
                    optimized_route['distance'] = distance2
                else:
                    optimized_route['bins'] = sequence1
                    optimized_route['distance'] = distance1
            else:
                optimized_route['bins'] = sequence1
                optimized_route['distance'] = distance1
            
            optimized_routes.append(optimized_route)
        
        return optimized_routes
    
    def _calculate_sequence_distance(self, sequence):
        """
        Calculate the total distance of a sequence.
        
        Args:
            sequence: List of bin IDs in sequence
            
        Returns:
            float: Total distance
        """
        total_distance = 0
        
        for i in range(len(sequence) - 1):
            try:
                total_distance += self.city.graph[sequence[i]][sequence[i+1]]['weight']
            except KeyError:
                _, dist = self.city.get_shortest_path(sequence[i], sequence[i+1])
                total_distance += dist
        
        return total_distance