#!/usr/bin/env python3
"""
Smart Trash Truck Routing System

This program optimizes trash collection routes for city garbage trucks,
prioritizing nearly-full bins while minimizing total distance traveled.

Usage:
  python main.py [options]

Options:
  -h, --help            Show this help message
  -t, --test SCENARIO   Run a test scenario (basic, constraint, algorithm)
  -l, --load DATA_DIR   Load data from specified directory
  -s, --save OUTPUT_DIR Save results to specified directory
  -v, --visualize       Show visualizations
  -c, --clustering      Use clustering algorithm (default: True)
  -d, --distance DIST   Distance threshold for clustering (default: None)
"""

import os
import sys
import argparse
import matplotlib.pyplot as plt

from src.data_structures.b_plus_tree import BPlusTree
from src.models.bin import Bin
from src.models.truck import Truck
from src.models.city import City
from src.algorithms.kruskal import kruskal_mst
from src.algorithms.kadane import adapted_kadane_for_bin_clusters
from src.algorithms.dynamic_programming import optimize_cluster_routes
from src.routing.route_generator import RouteGenerator
from src.routing.optimizer import RouteOptimizer
from src.utils.data_loader import DataLoader
from src.utils.metrics import calculate_metrics
from src.visualization.map_renderer import MapRenderer

def parse_arguments():
    """Parse command line arguments."""
    parser = argparse.ArgumentParser(description='Smart Trash Truck Routing System')
    parser.add_argument('-t', '--test', choices=['basic', 'constraint', 'algorithm'],
                      help='Run a test scenario')
    parser.add_argument('-l', '--load', help='Load data from specified directory')
    parser.add_argument('-s', '--save', help='Save results to specified directory')
    parser.add_argument('-v', '--visualize', action='store_true', help='Show visualizations')
    parser.add_argument('-c', '--clustering', action='store_true', default=True,
                      help='Use clustering algorithm')
    parser.add_argument('-d', '--distance', type=float, default=None,
                      help='Distance threshold for clustering')
    
    return parser.parse_args()

def run_test_scenario(scenario_type, output_dir=None, use_clustering=True, distance_threshold=None,
                    show_visualizations=False):
    """Run a specific test scenario."""
    print(f"Running {scenario_type} test scenario...")
    
    # Generate test data
    city, trucks = DataLoader.generate_test_scenario(scenario_type, output_dir)
    
    print(f"Generated city with {len(city.bins)} bins and {len(trucks)} trucks")
    
    # Create indexed bins for efficient lookup
    bin_index = BPlusTree()
    for bin_id, bin_obj in city.bins.items():
        bin_index.insert(bin_id, bin_obj)
    
    # Generate routes
    route_generator = RouteGenerator(city, trucks, use_clustering, distance_threshold)
    routes = route_generator.generate_routes()
    
    print(f"Generated {len(routes)} routes")
    
    # Optimize routes
    optimizer = RouteOptimizer(city)
    optimized_routes = optimizer.optimize_truck_routes(routes)
    
    # Prioritize high-priority bins
    priority_routes = optimizer.optimize_for_priority(optimized_routes)
    
    # Calculate metrics
    metrics = calculate_metrics(city, priority_routes)
    
    # Print metrics
    print("\nRoute Metrics:")
    for key, value in metrics.items():
        if key != 'truck_utilization':
            print(f"  {key}: {value}")
    
    print("\nTruck Utilization:")
    for truck_id, utilization in metrics.get('truck_utilization', {}).items():
        print(f"  Truck {truck_id}: {utilization:.1f}%")
    
    # Print routes
    print("\nOptimized Routes:")
    for i, route in enumerate(priority_routes):
        print(f"\nRoute {i+1} - Truck {route['truck'].truck_id}:")
        print(f"  Bins: {' -> '.join(route['bins'])}")
        print(f"  Distance: {route['distance']:.2f}")
        print(f"  Priority Bins: {route['priority_bins']}")
    
    # Visualize if requested
    if show_visualizations:
        renderer = MapRenderer(city)
        
        # Show city map
        fig_map, _ = renderer.render_city_map()
        fig_map.suptitle(f"{scenario_type.capitalize()} Scenario - City Map", fontsize=16)
        
        # Show routes
        fig_routes, _ = renderer.render_routes(priority_routes)
        fig_routes.suptitle(f"{scenario_type.capitalize()} Scenario - Routes", fontsize=16)
        
        # Show metrics
        fig_metrics, _ = renderer.render_metrics(metrics)
        fig_metrics.suptitle(f"{scenario_type.capitalize()} Scenario - Metrics", fontsize=16)
        
        plt.show()
    
    # Save results if output_dir is provided
    if output_dir:
        os.makedirs(output_dir, exist_ok=True)
        
        # Save routes
        DataLoader.save_routes_to_file(priority_routes, os.path.join(output_dir, 'routes.json'))
        
        # Save visualizations
        renderer = MapRenderer(city)
        renderer.save_visualizations(priority_routes, output_dir, metrics)
        
        print(f"\nResults saved to {output_dir}")
    
    return city, trucks, priority_routes, metrics

def load_and_run(data_dir, output_dir=None, use_clustering=True, distance_threshold=None,
               show_visualizations=False):
    """Load data from files and run the routing algorithm."""
    print(f"Loading data from {data_dir}...")
    
    # Load data
    bins_file = os.path.join(data_dir, 'bins.csv')
    roads_file = os.path.join(data_dir, 'roads.csv')
    trucks_file = os.path.join(data_dir, 'trucks.csv')
    
    city = DataLoader.load_city_from_files(bins_file, roads_file)
    trucks = DataLoader.load_trucks_from_csv(trucks_file)
    
    print(f"Loaded city with {len(city.bins)} bins and {len(trucks)} trucks")
    
    # Create indexed bins for efficient lookup
    bin_index = BPlusTree()
    for bin_id, bin_obj in city.bins.items():
        bin_index.insert(bin_id, bin_obj)
    
    # Generate routes
    route_generator = RouteGenerator(city, trucks, use_clustering, distance_threshold)
    routes = route_generator.generate_routes()
    
    print(f"Generated {len(routes)} routes")
    
    # Optimize routes
    optimizer = RouteOptimizer(city)
    optimized_routes = optimizer.optimize_truck_routes(routes)
    
    # Prioritize high-priority bins
    priority_routes = optimizer.optimize_for_priority(optimized_routes)
    
    # Calculate metrics
    metrics = calculate_metrics(city, priority_routes)
    
    # Print metrics
    print("\nRoute Metrics:")
    for key, value in metrics.items():
        if key != 'truck_utilization':
            print(f"  {key}: {value}")
    
    print("\nTruck Utilization:")
    for truck_id, utilization in metrics.get('truck_utilization', {}).items():
        print(f"  Truck {truck_id}: {utilization:.1f}%")
    
    # Print routes
    print("\nOptimized Routes:")
    for i, route in enumerate(priority_routes):
        print(f"\nRoute {i+1} - Truck {route['truck'].truck_id}:")
        print(f"  Bins: {' -> '.join(route['bins'])}")
        print(f"  Distance: {route['distance']:.2f}")
        print(f"  Priority Bins: {route['priority_bins']}")
    
    # Visualize if requested
    if show_visualizations:
        renderer = MapRenderer(city)
        
        # Show city map
        fig_map, _ = renderer.render_city_map()
        fig_map.suptitle("City Map", fontsize=16)
        
        # Show routes
        fig_routes, _ = renderer.render_routes(priority_routes)
        fig_routes.suptitle("Optimized Routes", fontsize=16)
        
        # Show metrics
        fig_metrics, _ = renderer.render_metrics(metrics)
        fig_metrics.suptitle("Route Metrics", fontsize=16)
        
        plt.show()
    
    # Save results if output_dir is provided
    if output_dir:
        os.makedirs(output_dir, exist_ok=True)
        
        # Save routes
        DataLoader.save_routes_to_file(priority_routes, os.path.join(output_dir, 'routes.json'))
        
        # Save visualizations
        renderer = MapRenderer(city)
        renderer.save_visualizations(priority_routes, output_dir, metrics)
        
        print(f"\nResults saved to {output_dir}")
    
    return city, trucks, priority_routes, metrics

def main():
    """Main function."""
    args = parse_arguments()
    
    if args.test:
        # Run a test scenario
        run_test_scenario(args.test, args.save, args.clustering, args.distance, args.visualize)
    elif args.load:
        # Load data and run
        load_and_run(args.load, args.save, args.clustering, args.distance, args.visualize)
    else:
        print("Please specify a test scenario or data directory.")
        print("Run with -h for help.")
        sys.exit(1)

if __name__ == "__main__":
    main()