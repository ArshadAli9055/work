#!/usr/bin/env python3
"""
ASCII-Compatible Test Runner for Smart Trash Truck Routing System

This script creates test data in the exact format expected by the Java implementation
and then runs the Python implementation on this data for proper comparison.
All output uses ASCII characters only for maximum compatibility.

Usage:
  python ascii_compatible_test_runner.py [options]

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
    parser = argparse.ArgumentParser(description='ASCII-Compatible Test Runner for Smart Trash Truck Routing System')
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

def load_java_compatible_data(data_dir):
    """
    Load data from CSV files in the Java-compatible format.
    
    Args:
        data_dir: Directory containing the CSV files
        
    Returns:
        City: City object populated with the loaded data
        list: List of Truck objects
    """
    print(f"Loading data from {data_dir}...")
    
    # Initialize empty city
    city = City()
    trucks = []
    
    # Load bins.csv
    bins_file = os.path.join(data_dir, 'bins.csv')
    if os.path.exists(bins_file):
        with open(bins_file, 'r') as f:
            reader = csv.DictReader(f)
            for row in reader:
                bin_id = str(row['id'])
                x = float(row['x'])
                y = float(row['y'])
                fill_level = float(row['fillLevel'])
                capacity = float(row['capacity'])
                
                bin_obj = Bin(bin_id, (x, y), capacity, fill_level)
                city.add_bin(bin_obj)
    else:
        print(f"Warning: {bins_file} not found")
    
    # Load roads.csv
    roads_file = os.path.join(data_dir, 'roads.csv')
    if os.path.exists(roads_file):
        with open(roads_file, 'r') as f:
            reader = csv.DictReader(f)
            for row in reader:
                start_bin_id = str(row['startBinId'])
                end_bin_id = str(row['endBinId'])
                distance = float(row['distance'])
                
                # Add road to city
                city.add_road(start_bin_id, end_bin_id, distance)
    else:
        print(f"Warning: {roads_file} not found")
    
    # Load trucks.csv
    trucks_file = os.path.join(data_dir, 'trucks.csv')
    if os.path.exists(trucks_file):
        with open(trucks_file, 'r') as f:
            reader = csv.DictReader(f)
            for row in reader:
                truck_id = str(row['id'])
                capacity = float(row['capacity'])
                
                truck = Truck(truck_id, capacity)
                trucks.append(truck)
    else:
        print(f"Warning: {trucks_file} not found")
    
    print(f"Loaded data:")
    print(f"- {len(city.bins)} bins")
    print(f"- {len(city.graph.edges())} roads")
    print(f"- {len(trucks)} trucks")
    
    return city, trucks

def save_test_report(test_name, city, routes, metrics, output_dir):
    """
    Save test report including visualizations to PDF.
    
    Args:
        test_name: Name of the test
        city: City object
        routes: List of routes
        metrics: Dictionary of metrics
        output_dir: Directory to save the report
    """
    # Create output directory
    os.makedirs(output_dir, exist_ok=True)
    
    # Create PDF report
    pdf_file = os.path.join(output_dir, f"{test_name}_report.pdf")
    with PdfPages(pdf_file) as pdf:
        # Create a MapRenderer
        renderer = MapRenderer(city)
        
        # Add title page
        fig = plt.figure(figsize=(8.5, 11))
        fig.suptitle(f"Smart Trash Truck Routing\n{test_name} Test Report", fontsize=16)
        plt.figtext(0.5, 0.5, f"Generated on: {time.strftime('%Y-%m-%d %H:%M:%S')}", 
                  ha='center', fontsize=12)
        pdf.savefig(fig)
        plt.close(fig)
        
        # Add city map
        fig, _ = renderer.render_city_map()
        fig.suptitle("City Map", fontsize=16)
        pdf.savefig(fig)
        plt.close(fig)
        
        # Add route visualization
        fig, _ = renderer.render_routes(routes)
        fig.suptitle("Optimized Routes", fontsize=16)
        pdf.savefig(fig)
        plt.close(fig)
        
        # Add individual route visualizations
        figs = renderer._render_routes_separate(routes, (10, 8), True)
        for i, (fig, _) in enumerate(figs):
            fig.suptitle(f"Route {i+1}", fontsize=16)
            pdf.savefig(fig)
            plt.close(fig)
        
        # Add metrics visualization
        fig, _ = renderer.render_metrics(metrics)
        fig.suptitle("Route Metrics", fontsize=16)
        pdf.savefig(fig)
        plt.close(fig)
        
        # Add textual report
        fig = plt.figure(figsize=(8.5, 11))
        fig.suptitle("Metrics Report", fontsize=16)
        plt.figtext(0.1, 0.1, format_metrics_report(metrics), fontsize=10, 
                  va='top', ha='left', wrap=True)
        pdf.savefig(fig)
        plt.close(fig)
    
    print(f"Report saved to {pdf_file}")
    
    # Save visualization files individually
    renderer.save_visualizations(routes, output_dir, metrics)

def run_test(city, trucks, output_dir, algorithm_name="Python Algorithm"):
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
    route_generator_kruskal = RouteGenerator(city, trucks, use_clustering=False)
    routes_kruskal = route_generator_kruskal.generate_routes()
    metrics_kruskal = calculate_metrics(city, routes_kruskal)
    
    # Now try with clustering (if we have enough bins)
    if len(city.bins) >= 10:
        try:
            print("Generating routes using integrated algorithms (clustering + DP)...")
            route_generator_integrated = RouteGenerator(city, trucks, use_clustering=True, distance_threshold=15)
            routes_integrated = route_generator_integrated.generate_routes()
            
            # Optimize with all algorithms
            optimizer = RouteOptimizer(city)
            optimized_routes = optimizer.optimize_truck_routes(routes_integrated)
            balanced_routes = optimizer.balance_loads(optimized_routes, trucks)
            priority_routes = optimizer.optimize_for_priority(balanced_routes)
            
            # Calculate metrics for integrated approach
            metrics_integrated = calculate_metrics(city, priority_routes)
            
            # Print comparison
            print("\nAlgorithm Comparison:")
            print(f"  Kruskal-only: {len(routes_kruskal)} routes, {metrics_kruskal['total_distance']:.2f} total distance")
            print(f"  Integrated: {len(priority_routes)} routes, {metrics_integrated['total_distance']:.2f} total distance")
            
            if metrics_integrated['total_distance'] < metrics_kruskal['total_distance']:
                improvement = ((metrics_kruskal['total_distance'] - metrics_integrated['total_distance']) / 
                              metrics_kruskal['total_distance'] * 100) if metrics_kruskal['total_distance'] > 0 else 0
                print(f"  Improvement: {improvement:.2f}% reduction in total distance")
            
            # Choose the better algorithm
            if metrics_integrated['efficiency_score'] > metrics_kruskal['efficiency_score']:
                print("\nUsing integrated algorithm (better efficiency score)")
                final_routes = priority_routes
                final_metrics = metrics_integrated
            else:
                print("\nUsing Kruskal-only algorithm (better efficiency score)")
                final_routes = routes_kruskal
                final_metrics = metrics_kruskal
                
        except Exception as e:
            print(f"Error with integrated algorithms: {e}")
            print("Falling back to Kruskal-only algorithm")
            final_routes = routes_kruskal
            final_metrics = metrics_kruskal
    else:
        print("Not enough bins for clustering, using Kruskal-only algorithm")
        final_routes = routes_kruskal
        final_metrics = metrics_kruskal
    
    # Print routes
    print("\nOptimized Routes:")
    for i, route in enumerate(final_routes):
        print(f"\nRoute {i+1} - Truck {route['truck'].truck_id}:")
        print(f"  Bins: {' -> '.join(route['bins'])}")
        print(f"  Distance: {route['distance']:.2f}")
        print(f"  Priority Bins: {route['priority_bins']}")
    
    # Save report
    save_test_report(algorithm_name, city, final_routes, final_metrics, test_dir)
    
    # Save routes in a format that can be compared with Java output
    routes_file = os.path.join(test_dir, 'routes.txt')
    with open(routes_file, 'w', encoding='ascii', errors='replace') as f:
        f.write(f"{algorithm_name} Routes\n")
        f.write("======================\n\n")
        
        for i, route in enumerate(final_routes):
            f.write(f"TRUCK #{route['truck'].truck_id}:\n")
            f.write(f"Capacity: {route['truck'].capacity}\n")
            f.write(f"Total Load: {route['truck'].current_load}\n")
            f.write(f"Total Distance: {route['distance']:.2f} units\n")
            f.write(f"Estimated Fuel Consumption: {route['distance'] * 0.1:.2f} liters\n\n")
            
            f.write("Route Sequence:\n")
            if not route['bins']:
                f.write("No bins assigned to this truck.\n")
            else:
                f.write("Depot -> ")
                for j, bin_id in enumerate(route['bins']):
                    bin_obj = city.bins[bin_id]
                    f.write(f"Bin {bin_id} (Fill: {bin_obj.fill_percentage:.1f}%)")
                    if j < len(route['bins']) - 1:
                        f.write(" -> ")
                f.write(" -> Depot\n")
            
            f.write("\n------------------------------------\n\n")
    
    print(f"Routes saved to {routes_file}")
    
    # Generate a Java-compatible summary file
    summary_file = os.path.join(output_dir, 'python_summary.txt')
    with open(summary_file, 'w', encoding='ascii', errors='replace') as f:
        f.write("=== SMART TRASH TRUCK ROUTING RESULTS ===\n\n")
        for i, route in enumerate(final_routes):
            f.write(f"TRUCK #{route['truck'].truck_id}:\n")
            f.write(f"Capacity: {route['truck'].capacity}\n")
            f.write(f"Total Load: {route['truck'].current_load}\n")
            f.write(f"Total Distance: {route['distance']:.2f} units\n")
            f.write(f"Estimated Fuel Consumption: {route['distance'] * 0.1:.2f} liters\n\n")
            
            f.write("Route Sequence:\n")
            if not route['bins']:
                f.write("No bins assigned to this truck.\n")
            else:
                f.write("Depot -> ")
                for j, bin_id in enumerate(route['bins']):
                    bin_obj = city.bins[bin_id]
                    f.write(f"Bin {bin_id} (Fill: {bin_obj.fill_percentage:.1f}%)")
                    if j < len(route['bins']) - 1:
                        f.write(" -> ")
                f.write(" -> Depot\n")
            
            f.write("\n------------------------------------\n\n")
    
    print(f"Summary saved to {summary_file}")
    
    return final_routes, final_metrics

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
        routes, metrics = run_test(city, trucks, args.output, "Python Implementation")
        
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
        print("\nHowever, the data files have been generated successfully.")
        print(f"You can still use the CSV files in {data_dir} with your Java implementation.")

if __name__ == "__main__":
    main()