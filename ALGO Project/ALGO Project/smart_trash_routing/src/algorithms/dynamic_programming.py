import numpy as np
import itertools
import math

def optimal_bin_sequence(city, bin_ids):
    """
    Use dynamic programming to determine the optimal sequence for visiting a set of bins.
    
    This is similar to solving the Traveling Salesman Problem (TSP) for a small subset of nodes.
    For small to medium-sized clusters (up to ~20 bins), we use an exact DP approach.
    For larger clusters, we use a heuristic approach.
    
    Args:
        city: City object with graph and bin information
        bin_ids: List of bin IDs to sequence
        
    Returns:
        tuple: (optimal_sequence, total_distance)
    """
    if not bin_ids:
        return [], 0
        
    if len(bin_ids) == 1:
        # For a single bin, calculate distance from depot to bin and back
        bin_obj = city.bins[bin_ids[0]]
        depot_to_bin_distance = math.sqrt(bin_obj.location[0]**2 + bin_obj.location[1]**2)
        return bin_ids, depot_to_bin_distance * 2
    
    if len(bin_ids) == 2:
        # For two bins, calculate best order based on distances
        bin1 = city.bins[bin_ids[0]]
        bin2 = city.bins[bin_ids[1]]
        
        # Calculate distances from depot to each bin
        depot_to_bin1 = math.sqrt(bin1.location[0]**2 + bin1.location[1]**2)
        depot_to_bin2 = math.sqrt(bin2.location[0]**2 + bin2.location[1]**2)
        
        # Calculate distance between bins
        try:
            bin1_to_bin2 = city.graph[bin_ids[0]][bin_ids[1]]['weight']
        except KeyError:
            # If no direct edge, calculate Euclidean distance
            loc1 = bin1.location
            loc2 = bin2.location
            bin1_to_bin2 = math.sqrt((loc1[0] - loc2[0])**2 + (loc1[1] - loc2[1])**2)
        
        # Calculate two possible route distances
        dist1 = depot_to_bin1 + bin1_to_bin2 + depot_to_bin2  # depot -> bin1 -> bin2 -> depot
        dist2 = depot_to_bin2 + bin1_to_bin2 + depot_to_bin1  # depot -> bin2 -> bin1 -> depot
        
        if dist1 <= dist2:
            return bin_ids, dist1
        else:
            return [bin_ids[1], bin_ids[0]], dist2
    
    # For larger problems, use heuristic
    if len(bin_ids) > 15:
        return nearest_neighbor_tsp(city, bin_ids)
    
    # Otherwise, use exact dynamic programming approach
    return held_karp_tsp(city, bin_ids)

def held_karp_tsp(city, bin_ids):
    """
    Implements the Held-Karp algorithm (dynamic programming) for solving the TSP.
    
    Args:
        city: City object with graph information
        bin_ids: List of bin IDs to sequence
        
    Returns:
        tuple: (optimal_sequence, total_distance)
    """
    n = len(bin_ids)
    
    # Create distance matrix between all pairs of bins
    distances = np.zeros((n+1, n+1))  # +1 for the depot (index 0)
    
    # Calculate distances from depot to each bin
    for i in range(n):
        bin_obj = city.bins[bin_ids[i]]
        depot_to_bin = math.sqrt(bin_obj.location[0]**2 + bin_obj.location[1]**2)
        distances[0, i+1] = depot_to_bin
        distances[i+1, 0] = depot_to_bin
    
    # Calculate distances between all pairs of bins
    for i in range(n):
        for j in range(n):
            if i != j:
                try:
                    distances[i+1, j+1] = city.graph[bin_ids[i]][bin_ids[j]]['weight']
                except KeyError:
                    # If no direct edge, calculate Euclidean distance
                    loc1 = city.bins[bin_ids[i]].location
                    loc2 = city.bins[bin_ids[j]].location
                    distances[i+1, j+1] = math.sqrt((loc1[0] - loc2[0])**2 + (loc1[1] - loc2[1])**2)
    
    # Initialize DP table
    # dp[S][i] = min cost to visit all nodes in subset S and end at node i
    dp = {}
    
    # Base case: Start at depot and visit each bin
    for i in range(1, n+1):
        dp[(1 << i), i] = distances[0, i]
    
    # For each subset size
    for size in range(2, n+1):
        # For each subset of size 'size' that includes depot
        for subset in itertools.combinations(range(1, n+1), size):
            # Convert subset to bitmask
            subset_bits = 0
            for j in subset:
                subset_bits |= 1 << j
            
            # For each possible end node in the subset
            for end in subset:
                # Calculate min cost to visit all nodes in subset and end at 'end'
                min_cost = float('inf')
                prev_subset_bits = subset_bits & ~(1 << end)
                
                # Try all possible previous nodes
                for prev in subset:
                    if prev == end:
                        continue
                    
                    # Calculate cost through prev
                    if (prev_subset_bits, prev) in dp:
                        cost = dp[(prev_subset_bits, prev)] + distances[prev, end]
                        min_cost = min(min_cost, cost)
                
                dp[(subset_bits, end)] = min_cost
    
    # Calculate optimal tour length
    all_bits = 0
    for i in range(1, n+1):
        all_bits |= 1 << i
    
    # Find the best end node
    min_cost = float('inf')
    end_node = -1
    for i in range(1, n+1):
        if (all_bits, i) in dp:
            cost = dp[(all_bits, i)] + distances[i, 0]  # Return to depot
            if cost < min_cost:
                min_cost = cost
                end_node = i
    
    # Reconstruct the tour
    tour = []
    state = all_bits
    node = end_node
    
    while node != -1:
        tour.append(node)
        new_state = state & ~(1 << node)
        
        # Find the previous node
        new_node = -1
        min_cost = float('inf')
        for i in range(1, n+1):
            if (i != node) and ((state >> i) & 1):  # i is in state
                if (new_state, i) in dp:
                    cost = dp[(new_state, i)] + distances[i, node]
                    if cost < min_cost:
                        min_cost = cost
                        new_node = i
        
        state = new_state
        node = new_node
        
        # Stop if we've visited all nodes
        if state == 0:
            break
    
    # Convert node indices back to bin_ids
    tour.reverse()  # Reverse to get the correct order
    optimal_sequence = [bin_ids[i-1] for i in tour]
    
    # Calculate the total distance of the tour
    total_distance = distances[0, tour[0]]  # From depot to first bin
    for i in range(len(tour) - 1):
        total_distance += distances[tour[i], tour[i+1]]
    total_distance += distances[tour[-1], 0]  # Return to depot
    
    return optimal_sequence, total_distance

def nearest_neighbor_tsp(city, bin_ids):
    """
    Implements the Nearest Neighbor heuristic for solving the TSP.
    This is faster but less optimal than the Held-Karp algorithm.
    
    Args:
        city: City object with graph information
        bin_ids: List of bin IDs to sequence
        
    Returns:
        tuple: (sequence, total_distance)
    """
    n = len(bin_ids)
    
    # Calculate distances from depot to each bin
    depot_distances = {}
    for bin_id in bin_ids:
        bin_obj = city.bins[bin_id]
        depot_distances[bin_id] = math.sqrt(bin_obj.location[0]**2 + bin_obj.location[1]**2)
    
    # Start with the bin closest to the depot
    current_bin = min(bin_ids, key=lambda b: depot_distances[b])
    tour = [current_bin]
    unvisited = set(bin_ids) - {current_bin}
    total_distance = depot_distances[current_bin]
    
    # Visit the nearest unvisited bin until all bins are visited
    while unvisited:
        next_bin = None
        min_distance = float('inf')
        
        for bin_id in unvisited:
            try:
                # Try to get distance from city graph
                distance = city.graph[current_bin][bin_id]['weight']
            except KeyError:
                # Calculate Euclidean distance if not in graph
                loc1 = city.bins[current_bin].location
                loc2 = city.bins[bin_id].location
                distance = math.sqrt((loc1[0] - loc2[0])**2 + (loc1[1] - loc2[1])**2)
            
            if distance < min_distance:
                min_distance = distance
                next_bin = bin_id
        
        if next_bin:
            tour.append(next_bin)
            unvisited.remove(next_bin)
            total_distance += min_distance
            current_bin = next_bin
        else:
            break
    
    # Add distance back to depot
    total_distance += depot_distances[tour[-1]]
    
    return tour, total_distance

def optimize_cluster_routes(city, clusters):
    """
    Optimize the sequence of bin visits within each cluster.
    
    Args:
        city: City object with graph information
        clusters: List of clusters, where each cluster is a list of bin IDs
        
    Returns:
        dict: Dictionary mapping cluster index to (sequence, distance)
    """
    optimized_routes = {}
    
    for i, cluster in enumerate(clusters):
        if cluster:  # Skip empty clusters
            sequence, distance = optimal_bin_sequence(city, cluster)
            optimized_routes[i] = (sequence, distance)
    
    return optimized_routes