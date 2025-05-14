def calculate_metrics(city, routes):
    """
    Calculate performance metrics for a set of routes.
    
    Args:
        city: City object with graph and bin information
        routes: List of routes, where each route is a dict with truck, bins, etc.
        
    Returns:
        dict: Dictionary of metrics
    """
    # Initialize metrics
    metrics = {
        'total_distance': 0,
        'total_bins_collected': 0,
        'high_priority_bins_collected': 0,
        'route_count': len(routes),
        'average_bins_per_route': 0,
        'average_distance_per_route': 0,
        'truck_utilization': {},
        'estimated_fuel': 0
    }
    
    # Total number of bins in the city
    total_bins = len(city.bins)
    
    # Number of high priority bins in the city
    high_priority_bins = len(city.get_high_priority_bins())
    
    # Calculate metrics for each route
    for route in routes:
        metrics['total_distance'] += route['distance']
        metrics['total_bins_collected'] += len(route['bins'])
        metrics['high_priority_bins_collected'] += route['priority_bins']
        
        # Calculate truck utilization
        truck = route['truck']
        metrics['truck_utilization'][truck.truck_id] = truck.load_percentage
    
    # Calculate average metrics
    if len(routes) > 0:
        metrics['average_bins_per_route'] = metrics['total_bins_collected'] / len(routes)
        metrics['average_distance_per_route'] = metrics['total_distance'] / len(routes)
    
    # Calculate bin coverage percentage
    metrics['bin_coverage_percentage'] = (metrics['total_bins_collected'] / total_bins) * 100 if total_bins > 0 else 0
    
    # Calculate high priority bin coverage percentage
    metrics['high_priority_coverage_percentage'] = (metrics['high_priority_bins_collected'] / high_priority_bins) * 100 if high_priority_bins > 0 else 0
    
    # Estimate fuel consumption (simple model: 0.1 liter per distance unit)
    metrics['estimated_fuel'] = metrics['total_distance'] * 0.1
    
    # Calculate efficiency score (higher is better)
    # Weighted combination of bin coverage, priority coverage, and inverse of distance
    if metrics['total_distance'] > 0:
        coverage_weight = 0.4
        priority_weight = 0.4
        distance_weight = 0.2
        
        normalized_distance = 100 / (1 + metrics['total_distance'] / total_bins)  # Lower distance is better
        
        metrics['efficiency_score'] = (
            coverage_weight * metrics['bin_coverage_percentage'] +
            priority_weight * metrics['high_priority_coverage_percentage'] +
            distance_weight * normalized_distance
        )
    else:
        metrics['efficiency_score'] = 0
    
    return metrics

def compare_metrics(metrics1, metrics2):
    """
    Compare two sets of metrics and return the differences.
    
    Args:
        metrics1: First set of metrics
        metrics2: Second set of metrics
        
    Returns:
        dict: Dictionary of metric differences (metrics2 - metrics1)
    """
    diff = {}
    
    for key in metrics1:
        if key == 'truck_utilization':
            diff[key] = {}
            for truck_id in set(metrics1[key].keys()).union(metrics2[key].keys()):
                if truck_id in metrics1[key] and truck_id in metrics2[key]:
                    diff[key][truck_id] = metrics2[key][truck_id] - metrics1[key][truck_id]
                elif truck_id in metrics2[key]:
                    diff[key][truck_id] = metrics2[key][truck_id]
                else:
                    diff[key][truck_id] = -metrics1[key][truck_id]
        else:
            if key in metrics2:
                diff[key] = metrics2[key] - metrics1[key]
    
    return diff

def format_metrics_report(metrics):
    """
    Format metrics as a text report.
    
    Args:
        metrics: Dictionary of metrics
        
    Returns:
        str: Formatted report
    """
    report = "Route Optimization Metrics Report\n"
    report += "==============================\n\n"
    
    report += "Route Statistics:\n"
    report += f"  Total Routes: {metrics['route_count']}\n"
    report += f"  Total Distance: {metrics['total_distance']:.2f} units\n"
    report += f"  Estimated Fuel Consumption: {metrics['estimated_fuel']:.2f} liters\n"
    report += f"  Average Distance per Route: {metrics['average_distance_per_route']:.2f} units\n\n"
    
    report += "Bin Collection:\n"
    report += f"  Total Bins Collected: {metrics['total_bins_collected']}\n"
    report += f"  High Priority Bins Collected: {metrics['high_priority_bins_collected']}\n"
    report += f"  Bin Coverage: {metrics['bin_coverage_percentage']:.2f}%\n"
    report += f"  High Priority Bin Coverage: {metrics['high_priority_coverage_percentage']:.2f}%\n"
    report += f"  Average Bins per Route: {metrics['average_bins_per_route']:.2f}\n\n"
    
    report += "Truck Utilization:\n"
    for truck_id, utilization in metrics['truck_utilization'].items():
        report += f"  Truck {truck_id}: {utilization:.2f}%\n"
    
    report += f"\nOverall Efficiency Score: {metrics['efficiency_score']:.2f}/100\n"
    
    return report