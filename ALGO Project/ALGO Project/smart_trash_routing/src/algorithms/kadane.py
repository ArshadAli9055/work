import numpy as np
from sklearn.cluster import DBSCAN
import math

def adapted_kadane_for_bin_clusters(city, grid_size=10, bin_weight_factor=1.0, 
                                  priority_weight_factor=2.0, distance_threshold=None):
    """
    Adapts Kadane's algorithm concept to identify clusters of bins in a 2D space.
    
    This approach divides the city into a grid and scores each cell based on:
    1. Number of bins in the cell
    2. Priority/fill level of those bins
    3. Proximity/density of bins
    
    Then uses a 2D version of Kadane's algorithm to find rectangular regions with
    the highest bin "density" scores.
    
    Alternatively, uses DBSCAN for non-rectangular clusters when distance_threshold is provided.
    
    Args:
        city: City object with bin information
        grid_size: Size of each grid cell
        bin_weight_factor: Weight factor for each bin in scoring
        priority_weight_factor: Additional weight for high priority bins
        distance_threshold: If provided, uses DBSCAN instead of grid-based approach
        
    Returns:
        list: List of clusters, where each cluster is a list of bin IDs
    """
    # If distance threshold is provided, use DBSCAN directly
    if distance_threshold is not None:
        return dbscan_clustering(city, distance_threshold)
    
    # Get the city bounds
    min_x, min_y = float('inf'), float('inf')
    max_x, max_y = float('-inf'), float('-inf')
    
    for bin_obj in city.bins.values():
        x, y = bin_obj.location
        min_x = min(min_x, x)
        min_y = min(min_y, y)
        max_x = max(max_x, x)
        max_y = max(max_y, y)
    
    # Calculate grid dimensions
    grid_width = math.ceil((max_x - min_x) / grid_size)
    grid_height = math.ceil((max_y - min_y) / grid_size)
    
    # Initialize grid
    grid = np.zeros((grid_height, grid_width))
    
    # Map bins to grid cells and calculate cell scores
    bin_to_cell = {}  # Maps bin_id to (row, col) in grid
    cell_to_bins = {}  # Maps (row, col) to list of bin_ids
    
    for bin_id, bin_obj in city.bins.items():
        x, y = bin_obj.location
        col = min(grid_width - 1, max(0, int((x - min_x) / grid_size)))
        row = min(grid_height - 1, max(0, int((y - min_y) / grid_size)))
        
        # Calculate bin's contribution to cell score
        bin_score = bin_weight_factor
        if bin_obj.is_high_priority:
            bin_score *= priority_weight_factor
        
        grid[row, col] += bin_score
        bin_to_cell[bin_id] = (row, col)
        
        if (row, col) not in cell_to_bins:
            cell_to_bins[(row, col)] = []
        cell_to_bins[(row, col)].append(bin_id)
    
    # Apply 2D Kadane's algorithm to find the maximum sum sub-rectangle
    clusters = []
    
    # Continue finding clusters until no positive-sum clusters remain
    while True:
        max_sum = 0
        max_rect = None
        
        # Try all possible left and right boundaries
        for left in range(grid_width):
            for right in range(left, grid_width):
                # For each left-right boundary, calculate the maximum sum subarray
                temp = np.zeros(grid_height)
                for i in range(grid_height):
                    temp[i] = np.sum(grid[i, left:right+1])
                
                # Apply 1D Kadane's algorithm
                current_sum = 0
                start = 0
                
                for i in range(grid_height):
                    if current_sum + temp[i] > 0:
                        current_sum += temp[i]
                    else:
                        current_sum = 0
                        start = i + 1
                        continue
                    
                    if current_sum > max_sum:
                        max_sum = current_sum
                        max_rect = (start, left, i, right)  # (top, left, bottom, right)
        
        # If no positive-sum cluster is found, break
        if max_sum <= 0 or max_rect is None:
            break
        
        # Extract bins in this cluster
        top, left, bottom, right = max_rect
        cluster_bins = []
        
        for row in range(top, bottom + 1):
            for col in range(left, right + 1):
                if (row, col) in cell_to_bins:
                    cluster_bins.extend(cell_to_bins[(row, col)])
        
        if cluster_bins:
            clusters.append(cluster_bins)
        
        # Zero out the grid cells in this cluster to find other non-overlapping clusters
        for row in range(top, bottom + 1):
            for col in range(left, right + 1):
                grid[row, col] = 0
    
    # Handle any remaining bins not in clusters
    remaining_bins = set(city.bins.keys()) - set(sum(clusters, []))
    if remaining_bins:
        clusters.append(list(remaining_bins))
    
    return clusters

def dbscan_clustering(city, distance_threshold):
    """
    Uses DBSCAN algorithm to cluster bins based on their geographic proximity.
    
    Args:
        city: City object with bin information
        distance_threshold: Maximum distance between bins in the same cluster
        
    Returns:
        list: List of clusters, where each cluster is a list of bin IDs
    """
    # Extract bin locations and IDs
    bin_ids = list(city.bins.keys())
    locations = np.array([city.bins[bin_id].location for bin_id in bin_ids])
    
    # Apply DBSCAN clustering
    dbscan = DBSCAN(eps=distance_threshold, min_samples=2, metric='euclidean')
    clusters_labels = dbscan.fit_predict(locations)
    
    # Group bins by cluster
    clusters = {}
    for i, label in enumerate(clusters_labels):
        if label not in clusters:
            clusters[label] = []
        clusters[label].append(bin_ids[i])
    
    # Convert to list of clusters
    result = list(clusters.values())
    
    return result