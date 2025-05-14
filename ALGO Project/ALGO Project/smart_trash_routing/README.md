# Smart Trash Truck Routing System

This project implements an advanced routing system for city trash collection trucks, optimizing routes to prioritize nearly-full bins while minimizing travel distance and respecting truck capacity constraints.

## Features

- **City Modeling**: Represents the city as a graph with bin locations as nodes and roads as edges
- **Multiple Algorithms**:
  - **Kruskal's Algorithm**: Finds a Minimum Spanning Tree of the bin network for foundational route planning
  - **Kadane's Algorithm (Adapted)**: Identifies high-density clusters of bins for efficient collection
  - **Dynamic Programming**: Optimizes the sequence of bin visits to minimize travel distance
  - **B+ Tree**: Efficiently stores and queries bin data by location, ID, and fill level
- **Prioritization**: Ensures high-priority bins (nearly full) are collected first
- **Constraint Handling**: Respects truck capacity limits and other constraints
- **Visualization**: Generates detailed visualizations of the city, routes, and performance metrics
- **Dynamic Updates**: Can handle real-time updates to bin fill levels (optional feature)

## Project Structure

```
smart_trash_routing/
├── data/                    # Input data files
├── src/                     # Source code
│   ├── models/              # Data models
│   │   ├── bin.py           # Trash bin model
│   │   ├── truck.py         # Truck model
│   │   └── city.py          # City graph model
│   ├── data_structures/     # Custom data structures
│   │   └── b_plus_tree.py   # B+ Tree implementation
│   ├── algorithms/          # Algorithm implementations
│   │   ├── kruskal.py       # Kruskal's MST algorithm
│   │   ├── kadane.py        # Kadane's adaptation for clustering
│   │   └── dynamic_programming.py  # DP for sequencing
│   ├── routing/             # Route planning
│   │   ├── route_generator.py  # Generate routes
│   │   └── optimizer.py     # Optimize routes
│   ├── utils/               # Utility functions
│   │   ├── data_loader.py   # Load input data
│   │   └── metrics.py       # Calculate performance metrics
│   └── visualization/       # Visualization
│       └── map_renderer.py  # Render routes on map
├── tests/                   # Unit tests
├── main.py                  # Entry point
├── test_runner.py           # Test scenario runner
└── requirements.txt         # Dependencies
```

## Setup Instructions

1. **Create a Virtual Environment**:
   ```bash
   python -m venv venv
   source venv/bin/activate  # On Windows: venv\Scripts\activate
   ```

2. **Install Dependencies**:
   ```bash
   pip install -r requirements.txt
   ```

3. **Run the Project**:
   ```bash
   # Run with a test scenario
   python main.py --test basic --visualize
   
   # Run with custom data
   python main.py --load data/my_city --save results/my_city --visualize
   ```

## Test Scenarios

The project includes predefined test scenarios for evaluation:

1. **Basic Functionality Test**:
   - Small city map (5-7 bins, simple connections)
   - All bins have low-to-medium fill levels
   - 1 truck with high capacity
   ```bash
   python test_runner.py -o test_results/basic
   ```

2. **Constraint Handling & Prioritization Test**:
   - Medium city map (10-15 bins)
   - Several bins marked as "nearly full" (high priority)
   - 1-2 trucks with limited capacity
   ```bash
   python test_runner.py -o test_results/constraint
   ```

3. **Algorithm Integration & Optimization Test**:
   - Larger city map (20+ bins) with clear clusters
   - Varying fill levels
   - Multiple trucks
   ```bash
   python test_runner.py -o test_results/algorithm
   ```

4. **Run All Tests**:
   ```bash
   python test_runner.py -o test_results
   ```

## Generating Custom Data

You can generate custom test data using the DataLoader class:

```python
from src.utils.data_loader import DataLoader

# Generate random data
city, trucks = DataLoader.generate_random_data(
    num_bins=20,
    num_trucks=3,
    grid_size=100,
    min_capacity=100,
    max_capacity=500,
    connection_density=0.3,
    save_dir='data/custom'
)
```

## Evaluating Results

The test runner generates detailed reports including:

- Visualization of the city map with bin locations
- Visualization of optimized routes for each truck
- Performance metrics (distance, bin coverage, fuel consumption, etc.)
- Comparison of different algorithmic approaches

Results are saved to the specified output directory as PNG images and a PDF report.

## Key Components and Algorithms

1. **City Graph Representation**: The city is modeled as a graph where nodes represent bin locations and edges represent roads.

2. **Kruskal's Algorithm**: Used to find a Minimum Spanning Tree (MST) of the bin network, which provides a foundational structure for route planning.

3. **Kadane's Algorithm Adaptation**: Identifies high-density clusters of bins based on location and priority. This helps optimize collection by keeping trucks in areas with many bins.

4. **Dynamic Programming for Sequencing**: Within each cluster or route segment, dynamic programming finds the optimal sequence to visit bins, similar to solving a Traveling Salesman Problem (TSP) for small sets of nodes.

5. **B+ Tree**: Efficiently stores and queries bin data, enabling quick lookup by location, ID, or fill level.

6. **Route Generation and Optimization**: Integrates the outputs of the different algorithms to generate complete, optimized collection routes for trucks while respecting constraints like truck capacity.

7. **Priority Handling**: Ensures that high-priority bins (those that are nearly full) are included in routes, potentially visited earlier to prevent overflow.

8. **Visualization**: Provides clear visual representation of the city, routes, and performance metrics.

## Project Requirements

- Python 3.8+
- NetworkX (for graph operations)
- NumPy (for numerical operations)
- Matplotlib (for visualization)
- scikit-learn (for clustering algorithms)
- pandas (for data handling)

## License

This project is licensed under the MIT License - see the LICENSE file for details.