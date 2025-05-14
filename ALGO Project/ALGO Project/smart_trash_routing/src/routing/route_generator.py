from src.algorithms.kruskal import kruskal_mst
from src.algorithms.kadane import adapted_kadane_for_bin_clusters
from src.algorithms.dynamic_programming import optimize_cluster_routes
import math

class RouteGenerator:
    """Main class for generating optimized trash collection routes."""
    
    def __init__(self, city, trucks, use_clustering=True, distance_threshold=None):
        """
        Initialize the route generator.
        
        Args:
            city: City object with graph and bin information
            trucks: List of Truck objects
            use_clustering: Whether to use clustering for route generation
            distance_threshold: Distance threshold for DBSCAN clustering
        """
        self.city = city
        self.trucks = trucks
        self.use_clustering = use_clustering
        self.distance_threshold = distance_threshold
        self.routes = []  # List of finalized routes
        
    def generate_routes(self):
        """
        Generate optimized routes for all trucks.
        
        Returns:
            list: List of routes, where each route is a dict with truck, bins, and metrics
        """
        # Reset routes
        self.routes = []
        
        # Reset trucks
        for truck in self.trucks:
            truck.empty_truck()
            truck.reset_route()
        
        # First, identify high priority bins
        high_priority_bins = self.city.get_high_priority_bins()
        normal_bins = [bin_id for bin_id in self.city.bins if bin_id not in high_priority_bins]
        
        if self.use_clustering:
            # Use clustering to group bins
            all_clusters = adapted_kadane_for_bin_clusters(
                self.city, 
                distance_threshold=self.distance_threshold
            )
            
            # Optimize the sequence within each cluster
            optimized_clusters = optimize_cluster_routes(self.city, all_clusters)
            
            # Sort clusters by priority (clusters with high priority bins first)
            prioritized_clusters = self._prioritize_clusters(all_clusters, high_priority_bins)
            
            # Assign clusters to trucks
            routes = self._assign_clusters_to_trucks_improved(prioritized_clusters, optimized_clusters)
        else:
            # Use Kruskal's MST for route generation
            routes = self._mst_based_routing(high_priority_bins, normal_bins)
        
        # Store the generated routes
        self.routes = routes
        
        return routes
    
    def _prioritize_clusters(self, clusters, high_priority_bins):
        """
        Sort clusters by priority.
        
        Args:
            clusters: List of clusters (each a list of bin IDs)
            high_priority_bins: List of high priority bin IDs
            
        Returns:
            list: List of cluster indices sorted by priority
        """
        # Calculate priority score for each cluster
        cluster_scores = []
        
        for i, cluster in enumerate(clusters):
            # Count high priority bins in the cluster
            high_priority_count = sum(1 for bin_id in cluster if bin_id in high_priority_bins)
            
            # Calculate percentage of high priority bins
            high_priority_percentage = (high_priority_count / len(cluster)) if cluster else 0
            
            # Score based on high priority percentage and count
            score = (high_priority_percentage * 0.7)
            if len(high_priority_bins) > 0:
                score += (high_priority_count / len(high_priority_bins) * 0.3)
            
            cluster_scores.append((i, score))
        
        # Sort clusters by score in descending order
        return [idx for idx, _ in sorted(cluster_scores, key=lambda x: x[1], reverse=True)]
    
    def _assign_clusters_to_trucks_improved(self, prioritized_clusters, optimized_clusters):
        """
        Assign clusters to trucks based on capacity and priority,
        minimizing the number of routes and maximizing truck utilization.
        
        Args:
            prioritized_clusters: List of cluster indices sorted by priority
            optimized_clusters: Dict mapping cluster index to (sequence, distance)
            
        Returns:
            list: List of routes, where each route is a dict with truck, bins, and metrics
        """
        routes = []
        bins_assigned = set()
        
        # Step 1: Combine all bins into a prioritized list
        all_bins = []
        
        # First, add high priority bins
        high_priority_bins = self.city.get_high_priority_bins()
        for bin_id in high_priority_bins:
            all_bins.append(bin_id)
        
        # Then, add remaining bins from clusters
        for cluster_idx in prioritized_clusters:
            if cluster_idx in optimized_clusters:
                sequence, _ = optimized_clusters[cluster_idx]
                for bin_id in sequence:
                    if bin_id not in high_priority_bins and bin_id not in all_bins:
                        all_bins.append(bin_id)
        
        # Step 2: Sort trucks by capacity (largest first)
        sorted_trucks = sorted(self.trucks, key=lambda t: t.capacity, reverse=True)
        
        # Step 3: Assign bins to trucks, maximizing utilization
        for truck in sorted_trucks:
            route_bins = []
            
            # Try to add as many bins as possible to this truck
            for bin_id in all_bins:
                if bin_id not in bins_assigned:
                    bin_obj = self.city.bins[bin_id]
                    
                    if truck.can_collect_bin(bin_obj):
                        # Add bin to this truck's route
                        route_bins.append(bin_id)
                        bins_assigned.add(bin_id)
                        truck.assign_bin(bin_obj)
            
            # If we have bins for this truck, optimize the route and create route object
            if route_bins:
                # Optimize the sequence of bins
                optimized_sequence, distance = self._optimize_route_sequence(route_bins)
                
                # Reset truck and reassign bins in optimized order
                truck.reset_route()
                
                # Assign bins to the truck in optimized order
                for bin_id in optimized_sequence:
                    bin_obj = self.city.bins[bin_id]
                    truck.assign_bin(bin_obj)
                
                # Create route object
                routes.append({
                    'truck': truck,
                    'bins': optimized_sequence,
                    'distance': distance,
                    'priority_bins': sum(1 for bin_id in optimized_sequence if bin_id in high_priority_bins)
                })
        
        return routes
    
    def _mst_based_routing(self, high_priority_bins, normal_bins):
        """
        Generate routes based on Minimum Spanning Tree approach.
        
        Args:
            high_priority_bins: List of high priority bin IDs
            normal_bins: List of normal priority bin IDs
            
        Returns:
            list: List of routes, where each route is a dict with truck, bins, and metrics
        """
        # Create a combined list with high priority bins first
        all_bins = list(high_priority_bins) + list(normal_bins)
        
        # Generate MST
        mst = kruskal_mst(self.city)
        
        # Sort trucks by capacity (largest first)
        sorted_trucks = sorted(self.trucks, key=lambda t: t.capacity, reverse=True)
        
        # Initialize routes
        routes = []
        bins_assigned = set()
        
        # For each truck, create a route by adding as many bins as possible
        for truck in sorted_trucks:
            # Skip if truck is already at capacity (can happen during real-time updates)
            if truck.current_load >= truck.capacity * 0.99:
                continue
                
            route_bins = []
            
            # Try to add as many bins as possible to this truck
            for bin_id in all_bins:
                if bin_id not in bins_assigned:
                    bin_obj = self.city.bins[bin_id]
                    
                    if truck.can_collect_bin(bin_obj):
                        route_bins.append(bin_id)
                        bins_assigned.add(bin_id)
                        truck.assign_bin(bin_obj)
            
            # If we have bins for this truck, optimize the route sequence
            if route_bins:
                # Try to optimize the sequence using MST for proximity
                optimized_sequence = self._optimize_using_mst(route_bins, mst)
                
                # Calculate distance for the route
                distance = self._calculate_route_distance(optimized_sequence)
                
                # Reset truck and reassign bins in optimized order
                truck.reset_route()
                
                # Assign bins to the truck in optimized order
                for bin_id in optimized_sequence:
                    bin_obj = self.city.bins[bin_id]
                    truck.assign_bin(bin_obj)
                
                # Create route object
                routes.append({
                    'truck': truck,
                    'bins': optimized_sequence,
                    'distance': distance,
                    'priority_bins': sum(1 for bin_id in optimized_sequence if bin_id in high_priority_bins)
                })
        
        return routes
    
    def _optimize_using_mst(self, bin_ids, mst):
        """
        Optimize the sequence of bin visits using the MST.
        
        Args:
            bin_ids: List of bin IDs to sequence
            mst: Minimum Spanning Tree
            
        Returns:
            list: Optimized sequence of bin IDs
        """
        if not bin_ids:
            return []
        
        if len(bin_ids) <= 2:
            return bin_ids
        
        # Start with the bin closest to the depot
        closest_bin = min(bin_ids, key=lambda bin_id: 
                         math.sqrt(self.city.bins[bin_id].location[0]**2 + 
                                 self.city.bins[bin_id].location[1]**2))
        
        sequence = [closest_bin]
        remaining_bins = set(bin_ids) - {closest_bin}
        
        # Add bins one by one, always choosing the closest one to the last added bin
        while remaining_bins:
            last_bin = sequence[-1]
            
            # Find the closest bin
            next_bin = None
            min_distance = float('inf')
            
            for bin_id in remaining_bins:
                try:
                    # Try to get distance from MST
                    edge_data = mst.get_edge_data(last_bin, bin_id)
                    if edge_data:
                        distance = edge_data['weight']
                    else:
                        # Calculate Euclidean distance if not in MST
                        loc1 = self.city.bins[last_bin].location
                        loc2 = self.city.bins[bin_id].location
                        distance = math.sqrt((loc1[0] - loc2[0])**2 + (loc1[1] - loc2[1])**2)
                except (KeyError, TypeError, AttributeError):
                    # Calculate Euclidean distance if there's any error
                    loc1 = self.city.bins[last_bin].location
                    loc2 = self.city.bins[bin_id].location
                    distance = math.sqrt((loc1[0] - loc2[0])**2 + (loc1[1] - loc2[1])**2)
                
                if distance < min_distance:
                    min_distance = distance
                    next_bin = bin_id
            
            if next_bin:
                sequence.append(next_bin)
                remaining_bins.remove(next_bin)
            else:
                # If no bin found, add remaining bins in any order
                sequence.extend(remaining_bins)
                break
        
        return sequence
    
    def _optimize_route_sequence(self, bin_ids):
        """
        Optimize the sequence of bin visits in a route.
        
        Args:
            bin_ids: List of bin IDs in the route
            
        Returns:
            tuple: (optimized_sequence, total_distance)
        """
        if not bin_ids:
            return [], 0
            
        from src.algorithms.dynamic_programming import optimal_bin_sequence
        
        # Special handling for single bin routes to calculate proper distance
        if len(bin_ids) == 1:
            bin_id = bin_ids[0]
            bin_obj = self.city.bins[bin_id]
            # Calculate distance from depot to bin and back
            depot_to_bin_distance = math.sqrt(bin_obj.location[0]**2 + bin_obj.location[1]**2)
            # Double the distance for round trip
            return bin_ids, depot_to_bin_distance * 2
            
        return optimal_bin_sequence(self.city, bin_ids)
    
    def _calculate_route_distance(self, bin_sequence):
        """
        Calculate the total distance of a route.
        
        Args:
            bin_sequence: List of bin IDs in sequence
            
        Returns:
            float: Total distance of the route
        """
        if not bin_sequence:
            return 0
            
        # Handle single bin routes
        if len(bin_sequence) == 1:
            bin_obj = self.city.bins[bin_sequence[0]]
            # Calculate distance from depot to bin and back
            depot_to_bin_distance = math.sqrt(bin_obj.location[0]**2 + bin_obj.location[1]**2)
            return depot_to_bin_distance * 2
        
        total_distance = 0
        
        # Add distance from depot to first bin
        first_bin = self.city.bins[bin_sequence[0]]
        total_distance += math.sqrt(first_bin.location[0]**2 + first_bin.location[1]**2)
        
        # Add distances between consecutive bins
        for i in range(len(bin_sequence) - 1):
            try:
                total_distance += self.city.graph[bin_sequence[i]][bin_sequence[i+1]]['weight']
            except KeyError:
                # If there's no direct edge, calculate Euclidean distance
                loc1 = self.city.bins[bin_sequence[i]].location
                loc2 = self.city.bins[bin_sequence[i+1]].location
                total_distance += math.sqrt((loc1[0] - loc2[0])**2 + (loc1[1] - loc2[1])**2)
        
        # Add distance from last bin back to depot
        last_bin = self.city.bins[bin_sequence[-1]]
        total_distance += math.sqrt(last_bin.location[0]**2 + last_bin.location[1]**2)
        
        return total_distance
    
    def get_route_metrics(self):
        """
        Calculate metrics for all routes.
        
        Returns:
            dict: Dictionary of metrics including total distance, bin coverage, etc.
        """
        metrics = {
            'total_distance': sum(route['distance'] for route in self.routes),
            'total_bins_collected': sum(len(route['bins']) for route in self.routes),
            'high_priority_bins_collected': sum(route['priority_bins'] for route in self.routes),
            'route_count': len(self.routes),
            'average_bins_per_route': sum(len(route['bins']) for route in self.routes) / len(self.routes) if self.routes else 0,
            'average_distance_per_route': sum(route['distance'] for route in self.routes) / len(self.routes) if self.routes else 0,
            'truck_utilization': {truck.truck_id: truck.load_percentage for truck in self.trucks}
        }
        
        return metrics
    
    def handle_dynamic_updates(self, updated_bins):
        """
        Handle dynamic updates to bin fill levels.
        
        Args:
            updated_bins: List of tuples (bin_id, new_fill_level)
            
        Returns:
            bool: True if routes were updated, False otherwise
        """
        # Update bin fill levels
        bins_updated = []
        routes_to_reoptimize = set()
        
        for bin_id, new_fill_level in updated_bins:
            if bin_id in self.city.bins:
                old_fill_level = self.city.bins[bin_id].current_fill_level
                old_priority = self.city.bins[bin_id].is_high_priority
                
                # Update bin fill level
                self.city.bins[bin_id].update_fill_level(new_fill_level)
                
                new_priority = self.city.bins[bin_id].is_high_priority
                bins_updated.append((bin_id, old_fill_level, new_fill_level))
                
                # Find which truck has this bin assigned
                for route in self.routes:
                    truck = route['truck']
                    if bin_id in route['bins']:
                        # Update truck's load
                        bin_obj = self.city.bins[bin_id]
                        old_load = bin_obj.capacity * old_fill_level
                        new_load = bin_obj.capacity * new_fill_level
                        
                        # Adjust truck load
                        route['truck'].current_load = route['truck'].current_load - old_load + new_load
                        
                        # Mark route for reoptimization if priority changed
                        if old_priority != new_priority:
                            routes_to_reoptimize.add(id(route))
                        
                        # Update bin in truck's collection dictionary
                        if bin_id in truck.bins_to_collect:
                            truck.bins_to_collect[bin_id] = bin_obj
        
        # If any route needs reoptimization, do it
        if routes_to_reoptimize:
            for i, route in enumerate(self.routes):
                if id(route) in routes_to_reoptimize:
                    # Reoptimize the sequence
                    bin_ids = route['bins']
                    optimized_sequence, distance = self._optimize_route_sequence(bin_ids)
                    
                    # Update the route
                    route['bins'] = optimized_sequence
                    route['distance'] = distance
                    route['priority_bins'] = sum(1 for bin_id in optimized_sequence 
                                              if self.city.bins[bin_id].is_high_priority)
            
            return True
        
        return False