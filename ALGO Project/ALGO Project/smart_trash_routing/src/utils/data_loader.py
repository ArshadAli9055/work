import csv
import json
import os
import random
from src.models.bin import Bin
from src.models.truck import Truck
from src.models.city import City

class DataLoader:
    """Class for loading and generating data for the routing system."""
    
    @staticmethod
    def load_bins_from_csv(file_path):
        """
        Load bin data from a CSV file.
        
        Args:
            file_path: Path to the CSV file
            
        Returns:
            list: List of Bin objects
        """
        bins = []
        
        with open(file_path, 'r') as f:
            reader = csv.DictReader(f)
            for row in reader:
                bin_id = row['bin_id']
                x = float(row['x'])
                y = float(row['y'])
                capacity = float(row['capacity'])
                fill_level = float(row['fill_level'])
                
                bins.append(Bin(bin_id, (x, y), capacity, fill_level))
        
        return bins
    
    @staticmethod
    def load_roads_from_csv(file_path):
        """
        Load road network data from a CSV file.
        
        Args:
            file_path: Path to the CSV file
            
        Returns:
            list: List of (bin_id1, bin_id2, distance) tuples
        """
        roads = []
        
        with open(file_path, 'r') as f:
            reader = csv.DictReader(f)
            for row in reader:
                bin_id1 = row['bin_id1']
                bin_id2 = row['bin_id2']
                distance = float(row['distance'])
                
                roads.append((bin_id1, bin_id2, distance))
        
        return roads
    
    @staticmethod
    def load_trucks_from_csv(file_path):
        """
        Load truck data from a CSV file.
        
        Args:
            file_path: Path to the CSV file
            
        Returns:
            list: List of Truck objects
        """
        trucks = []
        
        with open(file_path, 'r') as f:
            reader = csv.DictReader(f)
            for row in reader:
                truck_id = row['truck_id']
                capacity = float(row['capacity'])
                current_load = float(row.get('current_load', 0))
                
                # Check if starting location is provided
                if 'start_x' in row and 'start_y' in row:
                    start_x = float(row['start_x'])
                    start_y = float(row['start_y'])
                    starting_location = (start_x, start_y)
                else:
                    starting_location = (0, 0)
                
                trucks.append(Truck(truck_id, capacity, current_load, starting_location))
        
        return trucks
    
    @staticmethod
    def load_city_from_files(bins_file, roads_file):
        """
        Load a city from bin and road data files.
        
        Args:
            bins_file: Path to the bins CSV file
            roads_file: Path to the roads CSV file
            
        Returns:
            City: City object with loaded data
        """
        city = City()
        
        # Load bins
        bins = DataLoader.load_bins_from_csv(bins_file)
        for bin_obj in bins:
            city.add_bin(bin_obj)
        
        # Load roads
        roads = DataLoader.load_roads_from_csv(roads_file)
        for bin_id1, bin_id2, distance in roads:
            city.add_road(bin_id1, bin_id2, distance)
        
        return city
    
    @staticmethod
    def generate_random_data(num_bins=20, num_trucks=3, grid_size=100, 
                           min_capacity=100, max_capacity=500, 
                           connection_density=0.3, save_dir=None):
        """
        Generate random data for testing.
        
        Args:
            num_bins: Number of bins to generate
            num_trucks: Number of trucks to generate
            grid_size: Size of the city grid
            min_capacity: Minimum bin capacity
            max_capacity: Maximum bin capacity
            connection_density: Density of connections between bins
            save_dir: Directory to save generated data
            
        Returns:
            tuple: (City object, list of Truck objects)
        """
        # Create city
        city = City()
        
        # Generate random bins
        for i in range(num_bins):
            bin_id = f"bin_{i}"
            x = random.uniform(0, grid_size)
            y = random.uniform(0, grid_size)
            capacity = random.uniform(min_capacity, max_capacity)
            fill_level = random.uniform(0, 1)
            
            bin_obj = Bin(bin_id, (x, y), capacity, fill_level)
            city.add_bin(bin_obj)
        
        # Generate random roads
        bin_ids = list(city.bins.keys())
        for i in range(len(bin_ids)):
            for j in range(i+1, len(bin_ids)):
                if random.random() < connection_density:
                    bin_id1 = bin_ids[i]
                    bin_id2 = bin_ids[j]
                    # Distance is based on Euclidean distance
                    location1 = city.bins[bin_id1].location
                    location2 = city.bins[bin_id2].location
                    distance = ((location1[0] - location2[0])**2 + (location1[1] - location2[1])**2)**0.5
                    
                    city.add_road(bin_id1, bin_id2, distance)
        
        # Generate random trucks
        trucks = []
        for i in range(num_trucks):
            truck_id = f"truck_{i}"
            capacity = random.uniform(max_capacity * 5, max_capacity * 10)  # Trucks should have larger capacity
            
            trucks.append(Truck(truck_id, capacity))
        
        # Save data if requested
        if save_dir:
            os.makedirs(save_dir, exist_ok=True)
            
            # Save bins
            with open(os.path.join(save_dir, 'bins.csv'), 'w', newline='') as f:
                writer = csv.writer(f)
                writer.writerow(['bin_id', 'x', 'y', 'capacity', 'fill_level'])
                for bin_id, bin_obj in city.bins.items():
                    writer.writerow([bin_id, 
                                   bin_obj.location[0], 
                                   bin_obj.location[1], 
                                   bin_obj.capacity, 
                                   bin_obj.current_fill_level])
            
            # Save roads
            with open(os.path.join(save_dir, 'roads.csv'), 'w', newline='') as f:
                writer = csv.writer(f)
                writer.writerow(['bin_id1', 'bin_id2', 'distance'])
                for bin_id1, bin_id2, attr in city.graph.edges(data=True):
                    writer.writerow([bin_id1, bin_id2, attr['weight']])
            
            # Save trucks
            with open(os.path.join(save_dir, 'trucks.csv'), 'w', newline='') as f:
                writer = csv.writer(f)
                writer.writerow(['truck_id', 'capacity', 'current_load'])
                for truck in trucks:
                    writer.writerow([truck.truck_id, truck.capacity, truck.current_load])
        
        return city, trucks
    
    @staticmethod
    def generate_test_scenario(scenario_type, save_dir=None):
        """
        Generate a specific test scenario.
        
        Args:
            scenario_type: Type of scenario to generate (basic, constraint, algorithm)
            save_dir: Directory to save generated data
            
        Returns:
            tuple: (City object, list of Truck objects)
        """
        if scenario_type == 'basic':
            # Basic scenario: 5-7 bins, simple connections, 1 truck
            return DataLoader.generate_random_data(
                num_bins=7, 
                num_trucks=1, 
                grid_size=50, 
                min_capacity=100, 
                max_capacity=200, 
                connection_density=0.6,
                save_dir=save_dir
            )
        elif scenario_type == 'constraint':
            # Constraint scenario: 10-15 bins, several high priority, 1-2 trucks
            city, trucks = DataLoader.generate_random_data(
                num_bins=15, 
                num_trucks=2, 
                grid_size=70, 
                min_capacity=100, 
                max_capacity=300,
                connection_density=0.4,
                save_dir=save_dir
            )
            
            # Set some bins to high priority (nearly full)
            high_priority_count = random.randint(3, 5)
            bin_ids = list(city.bins.keys())
            selected_bins = random.sample(bin_ids, high_priority_count)
            
            for bin_id in selected_bins:
                city.bins[bin_id].update_fill_level(random.uniform(0.8, 0.95))
            
            return city, trucks
        elif scenario_type == 'algorithm':
            # Algorithm scenario: 20+ bins with clear clusters, multiple trucks
            city, trucks = DataLoader.generate_random_data(
                num_bins=25, 
                num_trucks=3, 
                grid_size=100, 
                min_capacity=100, 
                max_capacity=400,
                connection_density=0.3,
                save_dir=save_dir
            )
            
            # Create clusters by positioning bins
            clusters = 4
            bins_per_cluster = len(city.bins) // clusters
            bin_ids = list(city.bins.keys())
            
            for i in range(clusters):
                # Create a cluster center
                center_x = random.uniform(20, 80)
                center_y = random.uniform(20, 80)
                
                # Position bins around this center
                start_idx = i * bins_per_cluster
                end_idx = (i + 1) * bins_per_cluster if i < clusters - 1 else len(bin_ids)
                
                for j in range(start_idx, end_idx):
                    bin_id = bin_ids[j]
                    # Position within a small radius of the center
                    radius = random.uniform(0, 10)
                    angle = random.uniform(0, 2 * 3.14159)
                    x = center_x + radius * math.cos(angle)
                    y = center_y + radius * math.sin(angle)
                    
                    # Update bin location
                    city.bins[bin_id].location = (x, y)
                    city.graph.nodes[bin_id]['pos'] = (x, y)
            
            # Set some bins to high priority
            high_priority_count = random.randint(5, 8)
            selected_bins = random.sample(bin_ids, high_priority_count)
            
            for bin_id in selected_bins:
                city.bins[bin_id].update_fill_level(random.uniform(0.8, 0.95))
            
            return city, trucks
        else:
            raise ValueError(f"Unknown scenario type: {scenario_type}")
    
    @staticmethod
    def save_routes_to_file(routes, file_path):
        """
        Save routes to a JSON file.
        
        Args:
            routes: List of routes, where each route is a dict with truck, bins, etc.
            file_path: Path to save the JSON file
        """
        routes_data = []
        
        for route in routes:
            route_data = {
                'truck_id': route['truck'].truck_id,
                'bins': route['bins'],
                'distance': route['distance'],
                'priority_bins': route['priority_bins']
            }
            routes_data.append(route_data)
        
        with open(file_path, 'w') as f:
            json.dump(routes_data, f, indent=2)
    
    @staticmethod
    def load_routes_from_file(file_path, city, trucks):
        """
        Load routes from a JSON file.
        
        Args:
            file_path: Path to the JSON file
            city: City object
            trucks: List of Truck objects
            
        Returns:
            list: List of routes, where each route is a dict with truck, bins, etc.
        """
        with open(file_path, 'r') as f:
            routes_data = json.load(f)
        
        # Create a map of truck IDs to Truck objects
        truck_map = {truck.truck_id: truck for truck in trucks}
        
        routes = []
        for route_data in routes_data:
            truck_id = route_data['truck_id']
            
            if truck_id in truck_map:
                route = {
                    'truck': truck_map[truck_id],
                    'bins': route_data['bins'],
                    'distance': route_data['distance'],
                    'priority_bins': route_data['priority_bins']
                }
                routes.append(route)
        
        return routes