#!/usr/bin/env python3
"""
Fixed ASCII Test Runner with Real-time Updates

This script creates test data in the exact format expected by the Java implementation
and then runs the Python implementation on this data for proper comparison.
All output uses ASCII characters only for maximum compatibility.

Usage:
  python fixed_runner.py [options]

Options:
  -h, --help            Show this help message
  -o, --output DIR      Directory to save test results (default: java_compatible_test)
  -n, --num-bins INT    Number of bins to generate (default: 20)
  -t, --num-trucks INT  Number of trucks to generate (default: 3)
"""

import os
import argparse
import random
import csv
import math
import time
import matplotlib.pyplot as plt
from matplotlib.backends.backend_pdf import PdfPages

# Import Python implementation modules
from src.models.bin import Bin
from src.models.truck import Truck
from src.models.city import City
from src.routing.route_generator import RouteGenerator
from src.routing.optimizer import RouteOptimizer
from src.utils.metrics import calculate_metrics, format_metrics_report
from src.visualization.map_renderer import MapRenderer

def parse_arguments():
    """Parse command line arguments."""
    parser = argparse.ArgumentParser(description='Fixed ASCII Test Runner with Real-time Updates')
    parser.add_argument('-o', '--output', default='java_compatible_test',
                      help='Directory to save test results')
    parser.add_argument('-n', '--num-bins', type=int, default=20,
                      help='Number of bins to generate')
    parser.add_argument('-t', '--num-trucks', type=int, default=3,
                      help='Number of trucks to generate')
    
    return parser.parse_args()

def generate_java_compatible_data(num_bins, num_trucks, output_dir):
    """
    Generate test data in the format expected by the Java implementation.
    
    Args:
        num_bins: Number of bins to generate
        num_trucks: Number of trucks to generate
        output_dir: Directory to save the generated data
        
    Returns:
        City: City object populated with the generated data
        list: List of Truck objects
    """
    print(f"Generating data with {num_bins} bins and {num_trucks} trucks...")
    
    # Create output directory
    os.makedirs(output_dir, exist_ok=True)
    
    # Initialize empty city
    city = City()
    
    # Generate bins
    bins_data = []
    
    # Ensure we have some high priority bins (fill_level > 0.75)
    high_priority_count = max(3, num_bins // 4)  # At least 3 or 25% of bins
    
    for i in range(1, num_bins + 1):
        # Random position in a grid
        x = random.uniform(0, 100)
        y = random.uniform(0, 100)
        
        # Determine fill level - ensure some bins are high priority
        if i <= high_priority_count:
            fill_level = random.uniform(0.75, 0.95)  # High priority bins
        else:
            fill_level = random.uniform(0, 0.7)  # Regular bins
            
        capacity = random.uniform(100, 500)
        
        # Create a bin with ID starting from 1 (as Java implementation might expect)
        bin_id = str(i)
        bin_obj = Bin(bin_id, (x, y), capacity, fill_level)
        city.add_bin(bin_obj)
        
        # Add to data for CSV
        bins_data.append([i, x, y, fill_level, capacity])
    
    # Generate roads (connections between bins)
    roads_data = []
    connection_density = 0.3  # 30% of possible connections
    
    bin_ids = list(city.bins.keys())
    for i in range(len(bin_ids)):
        for j in range(i+1, len(bin_ids)):
            if random.random() < connection_density:
                bin_id1 = bin_ids[i]
                bin_id2 = bin_ids[j]
                
                # Calculate Euclidean distance
                loc1 = city.bins[bin_id1].location
                loc2 = city.bins[bin_id2].location
                distance = math.sqrt((loc1[0] - loc2[0])**2 + (loc1[1] - loc2[1])**2)
                
                # Add road to city
                city.add_road(bin_id1, bin_id2, distance)
                
                # Add to data for CSV (using integer IDs)
                roads_data.append([int(bin_id1), int(bin_id2), distance])
    
    # Generate trucks
    trucks = []
    trucks_data = []
    
    for i in range(1, num_trucks + 1):
        capacity = random.uniform(1000, 3000)  # Bigger capacity for trucks
        truck = Truck(str(i), capacity)
        trucks.append(truck)
        
        # Add to data for CSV
        trucks_data.append([i, capacity])
    
    # Write data to CSV files in Java-compatible format
    
    # bins.csv: id, x, y, fillLevel, capacity
    with open(os.path.join(output_dir, 'bins.csv'), 'w', newline='') as f:
        writer = csv.writer(f)
        writer.writerow(['id', 'x', 'y', 'fillLevel', 'capacity'])
        writer.writerows(bins_data)
    
    # roads.csv: startBinId, endBinId, distance
    with open(os.path.join(output_dir, 'roads.csv'), 'w', newline='') as f:
        writer = csv.writer(f)
        writer.writerow(['startBinId', 'endBinId', 'distance'])
        writer.writerows(roads_data)
    
    # trucks.csv: id, capacity
    with open(os.path.join(output_dir, 'trucks.csv'), 'w', newline='') as f:
        writer = csv.writer(f)
        writer.writerow(['id', 'capacity'])
        writer.writerows(trucks_data)
    
    print(f"Generated data saved to {output_dir}")
    print(f"- {len(city.bins)} bins")
    print(f"- {len(roads_data)} roads")
    print(f"- {len(trucks)} trucks")
    print(f"- {high_priority_count} high priority bins (fill level > 75%)")
    
    return city, trucks

def run_test(city, trucks, output_dir, algorithm_name="Python Implementation"):
    """
    Run the Python implementation on the given data.
    
    Args:
        city: City object with graph and bin information
        trucks: List of Truck objects
        output_dir: Directory to save test results
        algorithm_name: Name to use for the algorithm in reports
        
    Returns:
        list: List of optimized routes
        dict: Metrics for the routes
    """
    print(f"\n======== Running {algorithm_name} Test ========")
    test_dir = os.path.join(output_dir, algorithm_name.lower().replace(' ', '_'))
    os.makedirs(test_dir, exist_ok=True)
    
    # Check high priority bins
    high_priority_bins = city.get_high_priority_bins()
    print(f"Data has {len(high_priority_bins)} high priority bins (fill level > 75%)")
    
    # First, try Kruskal's MST approach
    print("Generating routes using Kruskal's algorithm...")
    route_generator = RouteGenerator(city, trucks, use_clustering=False)
    routes = route_generator.generate_routes()
    metrics = calculate_metrics(city, routes)
    
    # Print initial routes
    print("\nInitial Routes:")
    for i, route in enumerate(routes):
        print(f"\nRoute {i+1} - Truck {route['truck'].truck_id}:")
        print(f"  Bins: {' -> '.join(route['bins'])}")
        print(f"  Distance: {route['distance']:.2f}")
        print(f"  Priority Bins: {route['priority_bins']}")
        print(f"  Truck Load: {route['truck'].current_load:.2f}/{route['truck'].capacity:.2f} ({route['truck'].load_percentage:.1f}%)")
    
    # Generate a Java-compatible summary file for initial routes
    initial_summary_file = os.path.join(output_dir, 'python_summary_initial.txt')
    with open(initial_summary_file, 'w', encoding='ascii', errors='replace') as f:
        f.write("=== SMART TRASH TRUCK ROUTING RESULTS ===\n\n")
        for route in routes:
            truck = route['truck']
            f.write(f"TRUCK #{truck.truck_id}:\n")
            f.write(f"Capacity: {truck.capacity}\n")
            f.write(f"Total Load: {truck.current_load}\n")
            f.write(f"Total Distance: {route['distance']:.2f} units\n")
            f.write(f"Estimated Fuel Consumption: {route['distance'] * 0.1:.2f} liters\n\n")
            
            f.write("Route Sequence:\n")
            if not route['bins']:
                f.write("No bins assigned to this truck.\n")
            else:
                f.write("Depot -> ")
                for j, bin_id in enumerate(route['bins']):
                    bin_obj = city.bins[bin_id]
                    f.write(f"Bin {bin_id} (Fill: {bin_obj.current_fill_level*100:.1f}%)")
                    if j < len(route['bins']) - 1:
                        f.write(" -> ")
                f.write(" -> Depot\n")
            
            f.write("\n------------------------------------\n\n")
    
    print(f"Initial summary saved to {initial_summary_file}")
    
    # Perform a real-time update simulation
    print("\n=== SIMULATING REAL-TIME UPDATE ===\n")
    
    # Pick a bin with low fill level to update
    non_high_priority_bins = [bin_id for bin_id in city.bins if bin_id not in high_priority_bins]
    if non_high_priority_bins:
        update_bin_id = random.choice(non_high_priority_bins)
        bin_obj = city.bins[update_bin_id]
        old_fill_level = bin_obj.current_fill_level
        new_fill_level = min(1.0, old_fill_level + 0.5)  # Increase by 50%, but not more than 100%
        
        print(f"Real-time update received:")
        print(f"Bin #{update_bin_id} fill level changed from {old_fill_level*100:.1f}% to {new_fill_level*100:.1f}%")
        
        # Find which truck has this bin and update it
        affected_truck = None
        for route in routes:
            if update_bin_id in route['bins']:
                affected_truck = route['truck']
                print(f"Re-optimizing route for Truck #{affected_truck.truck_id}")
                break
        
        # Update the bin
        bin_obj.update_fill_level(new_fill_level)
        
        # Regenerate routes
        updated_routes = route_generator.generate_routes()
        updated_metrics = calculate_metrics(city, updated_routes)
        
        # Generate a Java-compatible summary file for updated routes
        updated_summary_file = os.path.join(output_dir, 'python_summary_updated.txt')
        with open(updated_summary_file, 'w', encoding='ascii', errors='replace') as f:
            f.write("=== UPDATED ROUTES ===\n\n")
            f.write("=== SMART TRASH TRUCK ROUTING RESULTS ===\n\n")
            for route in updated_routes:
                truck = route['truck']
                f.write(f"TRUCK #{truck.truck_id}:\n")
                f.write(f"Capacity: {truck.capacity}\n")
                f.write(f"Total Load: {truck.current_load}\n")
                f.write(f"Total Distance: {route['distance']:.2f} units\n")
                f.write(f"Estimated Fuel Consumption: {route['distance'] * 0.1:.2f} liters\n\n")
                
                f.write("Route Sequence:\n")
                if not route['bins']:
                    f.write("No bins assigned to this truck.\n")
                else:
                    f.write("Depot -> ")
                    for j, bin_id in enumerate(route['bins']):
                        bin_obj = city.bins[bin_id]
                        f.write(f"Bin {bin_id} (Fill: {bin_obj.current_fill_level*100:.1f}%)")
                        if j < len(route['bins']) - 1:
                            f.write(" -> ")
                    f.write(" -> Depot\n")
                
                f.write("\n------------------------------------\n\n")
        
        print(f"Updated summary saved to {updated_summary_file}")
        
        # Combine both summaries into one file
        final_summary_file = os.path.join(output_dir, 'python_summary.txt')
        with open(final_summary_file, 'w', encoding='ascii', errors='replace') as f:
            # Copy initial summary
            with open(initial_summary_file, 'r', encoding='ascii', errors='replace') as f_initial:
                f.write(f_initial.read())
            
            f.write("\n=== SIMULATING REAL-TIME UPDATE ===\n\n")
            f.write(f"Real-time update received:\n")
            f.write(f"Bin #{update_bin_id} fill level changed from {old_fill_level*100:.1f}% to {new_fill_level*100:.1f}%\n")
            
            if affected_truck:
                f.write(f"Re-optimizing route for Truck #{affected_truck.truck_id}\n\n")
            
            # Copy updated summary
            with open(updated_summary_file, 'r', encoding='ascii', errors='replace') as f_updated:
                f.write(f_updated.read())
        
        print(f"Final summary saved to {final_summary_file}")
        
        # Return the updated routes and metrics
        return updated_routes, updated_metrics
    else:
        # If no non-high-priority bins, just return the initial routes
        print("No bins available for update simulation.")
        
        # Generate a Java-compatible summary file
        summary_file = os.path.join(output_dir, 'python_summary.txt')
        with open(summary_file, 'w', encoding='ascii', errors='replace') as f:
            f.write("=== SMART TRASH TRUCK ROUTING RESULTS ===\n\n")
            for route in routes:
                truck = route['truck']
                f.write(f"TRUCK #{truck.truck_id}:\n")
                f.write(f"Capacity: {truck.capacity}\n")
                f.write(f"Total Load: {truck.current_load}\n")
                f.write(f"Total Distance: {route['distance']:.2f} units\n")
                f.write(f"Estimated Fuel Consumption: {route['distance'] * 0.1:.2f} liters\n\n")
                
                f.write("Route Sequence:\n")
                if not route['bins']:
                    f.write("No bins assigned to this truck.\n")
                else:
                    f.write("Depot -> ")
                    for j, bin_id in enumerate(route['bins']):
                        bin_obj = city.bins[bin_id]
                        f.write(f"Bin {bin_id} (Fill: {bin_obj.current_fill_level*100:.1f}%)")
                        if j < len(route['bins']) - 1:
                            f.write(" -> ")
                    f.write(" -> Depot\n")
                
                f.write("\n------------------------------------\n\n")
        
        print(f"Summary saved to {summary_file}")
        
        return routes, metrics

def main():
    """Main function."""
    args = parse_arguments()
    
    # Create or ensure output directory exists
    os.makedirs(args.output, exist_ok=True)
    
    # Generate Java-compatible test data
    data_dir = os.path.join(args.output, 'data')
    city, trucks = generate_java_compatible_data(args.num_bins, args.num_trucks, data_dir)
    
    # Run Python implementation
    try:
        routes, metrics = run_test(city, trucks, args.output)
        
        print("\n======== Test Complete ========")
        print(f"Results are saved in {args.output}")
        print(f"Java-compatible data is in {data_dir}")
        print(f"Use these files with your Java implementation:")
        print(f"  - {os.path.join(data_dir, 'bins.csv')}")
        print(f"  - {os.path.join(data_dir, 'roads.csv')}")
        print(f"  - {os.path.join(data_dir, 'trucks.csv')}")
        print("\nAfter running the Java implementation, compare the routes")
        print("and distances to evaluate which implementation performs better.")
    except Exception as e:
        print(f"\n====== ERROR RUNNING TEST ======")
        print(f"Error: {e}")
        import traceback
        traceback.print_exc()
        print("\nHowever, the data files have been generated successfully.")
        print(f"You can still use the CSV files in {data_dir} with your Java implementation.")

if __name__ == "__main__":
    main()