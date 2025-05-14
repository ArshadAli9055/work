import os
import argparse
import time
import math
import matplotlib.pyplot as plt
from matplotlib.backends.backend_pdf import PdfPages

from src.utils.data_loader import DataLoader
from src.routing.route_generator import RouteGenerator
from src.routing.optimizer import RouteOptimizer
from src.utils.metrics import calculate_metrics, format_metrics_report
from src.visualization.map_renderer import MapRenderer

def parse_arguments():
    """Parse command line arguments."""
    parser = argparse.ArgumentParser(description='Test Runner for Smart Trash Truck Routing System')
    parser.add_argument('-o', '--output', default='test_results',
                      help='Directory to save test results')
    
    return parser.parse_args()

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

def run_basic_test(output_dir):
    """Run basic functionality test."""
    print("\n======== Running Basic Functionality Test ========")
    test_dir = os.path.join(output_dir, 'basic_test')
    os.makedirs(test_dir, exist_ok=True)
    
    # Generate test data
    city, trucks = DataLoader.generate_test_scenario('basic', test_dir)
    
    print(f"Generated city with {len(city.bins)} bins and {len(trucks)} trucks")
    
    # Generate routes with Kruskal's MST approach
    route_generator = RouteGenerator(city, trucks, use_clustering=False)
    routes = route_generator.generate_routes()
    
    print(f"Generated {len(routes)} routes using Kruskal's MST")
    
    # Optimize routes with DP
    optimizer = RouteOptimizer(city)
    optimized_routes = optimizer.optimize_truck_routes(routes)
    
    # Calculate metrics
    metrics = calculate_metrics(city, optimized_routes)
    
    # Print routes
    print("\nOptimized Routes:")
    for i, route in enumerate(optimized_routes):
        print(f"\nRoute {i+1} - Truck {route['truck'].truck_id}:")
        print(f"  Bins: {' -> '.join(route['bins'])}")
        print(f"  Distance: {route['distance']:.2f}")
        print(f"  Priority Bins: {route['priority_bins']}")
    
    # Print metrics
    print("\nRoute Metrics:")
    for key, value in metrics.items():
        if key != 'truck_utilization':
            print(f"  {key}: {value}")
    
    # Save report
    save_test_report('Basic Functionality', city, optimized_routes, metrics, test_dir)
    
    return city, trucks, optimized_routes, metrics

def run_constraint_test(output_dir):
    """Run constraint handling & prioritization test."""
    print("\n======== Running Constraint Handling & Prioritization Test ========")
    test_dir = os.path.join(output_dir, 'constraint_test')
    os.makedirs(test_dir, exist_ok=True)
    
    # Generate test data
    city, trucks = DataLoader.generate_test_scenario('constraint', test_dir)
    
    print(f"Generated city with {len(city.bins)} bins and {len(trucks)} trucks")
    print(f"High priority bins: {len(city.get_high_priority_bins())}")
    
    # Generate routes with priority handling
    route_generator = RouteGenerator(city, trucks, use_clustering=True)
    routes = route_generator.generate_routes()
    
    print(f"Generated {len(routes)} routes with clustering and priority handling")
    
    # Specifically optimize for priority
    optimizer = RouteOptimizer(city)
    priority_routes = optimizer.optimize_for_priority(routes)
    
    # Calculate metrics
    metrics = calculate_metrics(city, priority_routes)
    
    # Print routes
    print("\nPriority-Optimized Routes:")
    for i, route in enumerate(priority_routes):
        print(f"\nRoute {i+1} - Truck {route['truck'].truck_id}:")
        print(f"  Bins: {' -> '.join(route['bins'])}")
        print(f"  Distance: {route['distance']:.2f}")
        print(f"  Priority Bins: {route['priority_bins']}")
        
        # Calculate position of high priority bins in the route
        high_priority_positions = []
        for j, bin_id in enumerate(route['bins']):
            if city.bins[bin_id].is_high_priority:
                high_priority_positions.append(j)
        
        if high_priority_positions:
            print(f"  High Priority Bin Positions: {high_priority_positions}")
            avg_position = sum(high_priority_positions) / len(high_priority_positions)
            print(f"  Average Position: {avg_position:.2f}/{len(route['bins'])-1} ({avg_position/(len(route['bins'])-1)*100:.2f}%)")
    
    # Print metrics
    print("\nRoute Metrics:")
    for key, value in metrics.items():
        if key != 'truck_utilization':
            print(f"  {key}: {value}")
    
    # Verify truck capacities are respected
    capacity_respected = True
    for route in priority_routes:
        truck = route['truck']
        if truck.load_percentage > 100:
            capacity_respected = False
            print(f"WARNING: Truck {truck.truck_id} is overloaded: {truck.load_percentage:.2f}%")
    
    if capacity_respected:
        print("\nAll truck capacity constraints are respected.")
    
    # Check high priority bin coverage
    high_priority_bins = city.get_high_priority_bins()
    collected_high_priority = set()
    
    for route in priority_routes:
        for bin_id in route['bins']:
            if bin_id in high_priority_bins:
                collected_high_priority.add(bin_id)
    
    hp_coverage = len(collected_high_priority) / len(high_priority_bins) if high_priority_bins else 1.0
    print(f"\nHigh Priority Bin Coverage: {hp_coverage*100:.2f}%")
    
    # Save report
    save_test_report('Constraint Handling', city, priority_routes, metrics, test_dir)
    
    return city, trucks, priority_routes, metrics

def run_algorithm_integration_test(output_dir):
    """Run algorithm integration & optimization test."""
    print("\n======== Running Algorithm Integration & Optimization Test ========")
    test_dir = os.path.join(output_dir, 'algorithm_test')
    os.makedirs(test_dir, exist_ok=True)
    
    # Generate test data
    city, trucks = DataLoader.generate_test_scenario('algorithm', test_dir)
    
    print(f"Generated city with {len(city.bins)} bins and {len(trucks)} trucks")
    
    # First, generate routes with Kruskal-only approach
    route_generator_kruskal = RouteGenerator(city, trucks, use_clustering=False)
    routes_kruskal = route_generator_kruskal.generate_routes()
    
    # Calculate metrics for Kruskal-only approach
    metrics_kruskal = calculate_metrics(city, routes_kruskal)
    
    # Now generate routes with clustering and full algorithm integration
    route_generator_integrated = RouteGenerator(city, trucks, use_clustering=True, distance_threshold=15)
    routes_integrated = route_generator_integrated.generate_routes()
    
    # Optimize with all algorithms
    optimizer = RouteOptimizer(city)
    optimized_routes = optimizer.optimize_truck_routes(routes_integrated)
    balanced_routes = optimizer.balance_loads(optimized_routes, trucks)
    priority_routes = optimizer.optimize_for_priority(balanced_routes)
    
    # Calculate metrics for integrated approach
    metrics_integrated = calculate_metrics(city, priority_routes)
    
    # Print comparison of approaches
    print("\nAlgorithm Comparison:")
    print(f"  Kruskal-only approach: {len(routes_kruskal)} routes, {metrics_kruskal['total_distance']:.2f} total distance")
    print(f"  Integrated approach: {len(priority_routes)} routes, {metrics_integrated['total_distance']:.2f} total distance")
    
    if metrics_integrated['total_distance'] < metrics_kruskal['total_distance']:
        improvement = ((metrics_kruskal['total_distance'] - metrics_integrated['total_distance']) / 
                      metrics_kruskal['total_distance'] * 100)
        print(f"  Improvement: {improvement:.2f}% reduction in total distance")
    
    # Print integrated routes
    print("\nIntegrated Algorithm Routes:")
    for i, route in enumerate(priority_routes):
        print(f"\nRoute {i+1} - Truck {route['truck'].truck_id}:")
        print(f"  Bins: {' -> '.join(route['bins'])}")
        print(f"  Distance: {route['distance']:.2f}")
        print(f"  Priority Bins: {route['priority_bins']}")
    
    # Generate cluster visualization
    from src.algorithms.kadane import adapted_kadane_for_bin_clusters
    clusters = adapted_kadane_for_bin_clusters(city, distance_threshold=15)
    
    print(f"\nIdentified {len(clusters)} clusters with distance threshold 15")
    for i, cluster in enumerate(clusters):
        print(f"  Cluster {i+1}: {len(cluster)} bins")
    
    # Save cluster visualization
    renderer = MapRenderer(city)
    fig, _ = renderer.render_clusters(clusters)
    fig.savefig(os.path.join(test_dir, 'clusters.png'), dpi=300, bbox_inches='tight')
    plt.close(fig)
    
    # Save comparative report
    save_test_report('Algorithm Integration', city, priority_routes, metrics_integrated, test_dir)
    
    # Save comparative metrics
    comparative_metrics = {
        'kruskal_only': metrics_kruskal,
        'integrated': metrics_integrated
    }
    
    # Create comparative visualization
    fig, ax = plt.subplots(figsize=(12, 8))
    metrics_to_compare = ['total_distance', 'efficiency_score', 
                         'average_distance_per_route', 'high_priority_coverage_percentage']
    
    bar_width = 0.35
    index = range(len(metrics_to_compare))
    
    kruskal_values = [metrics_kruskal[m] for m in metrics_to_compare]
    integrated_values = [metrics_integrated[m] for m in metrics_to_compare]
    
    ax.bar([i - bar_width/2 for i in index], kruskal_values, bar_width, label='Kruskal-only')
    ax.bar([i + bar_width/2 for i in index], integrated_values, bar_width, label='Integrated')
    
    ax.set_xlabel('Metric')
    ax.set_ylabel('Value')
    ax.set_title('Algorithm Comparison')
    ax.set_xticks(index)
    ax.set_xticklabels([m.replace('_', ' ').title() for m in metrics_to_compare], rotation=45, ha='right')
    ax.legend()
    
    plt.tight_layout()
    plt.savefig(os.path.join(test_dir, 'algorithm_comparison.png'), dpi=300, bbox_inches='tight')
    plt.close(fig)
    
    return city, trucks, priority_routes, metrics_integrated

def run_all_tests(output_dir):
    """Run all test cases."""
    print("======== Running All Test Cases ========")
    
    # Run basic test
    basic_city, basic_trucks, basic_routes, basic_metrics = run_basic_test(output_dir)
    
    # Run constraint test
    constraint_city, constraint_trucks, constraint_routes, constraint_metrics = run_constraint_test(output_dir)
    
    # Run algorithm test
    algo_city, algo_trucks, algo_routes, algo_metrics = run_algorithm_integration_test(output_dir)
    
    # Create summary report
    summary_file = os.path.join(output_dir, 'test_summary.txt')
    with open(summary_file, 'w') as f:
        f.write("Smart Trash Truck Routing System - Test Summary\n")
        f.write("=============================================\n\n")
        
        f.write("1. Basic Functionality Test\n")
        f.write("-------------------------\n")
        f.write(f"City: {len(basic_city.bins)} bins, {len(basic_trucks)} trucks\n")
        f.write(f"Routes: {len(basic_routes)}\n")
        f.write(f"Total Distance: {basic_metrics['total_distance']:.2f}\n")
        f.write(f"Efficiency Score: {basic_metrics['efficiency_score']:.2f}/100\n\n")
        
        f.write("2. Constraint Handling & Prioritization Test\n")
        f.write("------------------------------------------\n")
        f.write(f"City: {len(constraint_city.bins)} bins, {len(constraint_trucks)} trucks\n")
        f.write(f"High Priority Bins: {len(constraint_city.get_high_priority_bins())}\n")
        f.write(f"Routes: {len(constraint_routes)}\n")
        f.write(f"Total Distance: {constraint_metrics['total_distance']:.2f}\n")
        f.write(f"High Priority Coverage: {constraint_metrics['high_priority_coverage_percentage']:.2f}%\n")
        f.write(f"Efficiency Score: {constraint_metrics['efficiency_score']:.2f}/100\n\n")
        
        f.write("3. Algorithm Integration & Optimization Test\n")
        f.write("------------------------------------------\n")
        f.write(f"City: {len(algo_city.bins)} bins, {len(algo_trucks)} trucks\n")
        f.write(f"Routes: {len(algo_routes)}\n")
        f.write(f"Total Distance: {algo_metrics['total_distance']:.2f}\n")
        f.write(f"Efficiency Score: {algo_metrics['efficiency_score']:.2f}/100\n\n")
        
        f.write("Test Results Summary\n")
        f.write("-------------------\n")
        f.write("All test cases completed successfully.\n")
        f.write("Detailed reports are available in the test directories.\n")
    
    print(f"\nTest summary saved to {summary_file}")

def main():
    """Main function."""
    args = parse_arguments()
    
    # Create output directory
    os.makedirs(args.output, exist_ok=True)
    
    # Run all tests
    run_all_tests(args.output)

if __name__ == "__main__":
    main()