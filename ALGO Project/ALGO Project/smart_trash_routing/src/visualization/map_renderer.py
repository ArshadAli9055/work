import matplotlib.pyplot as plt
import networkx as nx
import numpy as np
import matplotlib.patches as mpatches
from matplotlib.collections import LineCollection
from matplotlib.colors import LinearSegmentedColormap

class MapRenderer:
    """Class for visualizing the city map and routes."""
    
    def __init__(self, city):
        """
        Initialize the map renderer.
        
        Args:
            city: City object with graph and bin information
        """
        self.city = city
        
    def render_city_map(self, figsize=(10, 8), show_roads=True, bin_size=100):
        """
        Render the city map with bins and roads.
        
        Args:
            figsize: Size of the figure (width, height)
            show_roads: Whether to show roads between bins
            bin_size: Size of bin markers
            
        Returns:
            tuple: (figure, axis)
        """
        fig, ax = plt.subplots(figsize=figsize)
        
        # Extract positions and fill levels of bins
        pos = {node: self.city.graph.nodes[node]['pos'] for node in self.city.graph.nodes()
              if 'pos' in self.city.graph.nodes[node]}
        
        # Color nodes based on fill level
        node_colors = []
        node_sizes = []
        fill_levels = []
        
        for node in self.city.graph.nodes():
            if node in self.city.bins:
                fill_level = self.city.bins[node].current_fill_level
                fill_levels.append(fill_level)
                # Color gradient from green (empty) to red (full)
                if fill_level <= 0.5:
                    # Green to yellow gradient
                    r = 2 * fill_level
                    g = 1.0
                    b = 0.0
                else:
                    # Yellow to red gradient
                    r = 1.0
                    g = 2 * (1 - fill_level)
                    b = 0.0
                node_colors.append((r, g, b))
                node_sizes.append(bin_size)
            elif 'is_facility' in self.city.graph.nodes[node] and self.city.graph.nodes[node]['is_facility']:
                # Disposal facilities are blue
                node_colors.append('blue')
                node_sizes.append(bin_size * 1.5)
            else:
                # Other nodes are gray
                node_colors.append('gray')
                node_sizes.append(bin_size * 0.8)
        
        # Draw the graph
        nx.draw_networkx_nodes(self.city.graph, pos, node_color=node_colors, 
                              node_size=node_sizes, alpha=0.8, ax=ax)
        
        if show_roads:
            # Draw edges with width proportional to weight
            edges = list(self.city.graph.edges(data=True))
            edge_colors = ['gray' for _ in edges]
            edge_widths = [1.0 for _ in edges]
            
            nx.draw_networkx_edges(self.city.graph, pos, edgelist=[e[:2] for e in edges],
                                 width=edge_widths, edge_color=edge_colors, alpha=0.4, ax=ax)
        
        # Draw labels
        labels = {node: node for node in self.city.graph.nodes() if node in self.city.bins}
        nx.draw_networkx_labels(self.city.graph, pos, labels=labels, font_size=8, ax=ax)
        
        # Add legend
        legend_elements = [
            mpatches.Patch(color=(0.0, 1.0, 0.0), label='Empty Bin'),
            mpatches.Patch(color=(1.0, 1.0, 0.0), label='Half Full Bin'),
            mpatches.Patch(color=(1.0, 0.0, 0.0), label='Full Bin'),
            mpatches.Patch(color='blue', label='Disposal Facility')
        ]
        ax.legend(handles=legend_elements, loc='best')
        
        # Add colorbar for fill levels if we have bins
        if fill_levels:
            cmap = LinearSegmentedColormap.from_list('fill_level', 
                                                    [(0, (0, 1, 0)), 
                                                     (0.5, (1, 1, 0)), 
                                                     (1, (1, 0, 0))])
            sm = plt.cm.ScalarMappable(cmap=cmap)
            sm.set_array([])
            cbar = plt.colorbar(sm, ax=ax)
            cbar.set_label('Bin Fill Level')
        
        ax.set_title('City Map with Bins')
        ax.set_xlabel('X Coordinate')
        ax.set_ylabel('Y Coordinate')
        ax.set_aspect('equal')
        
        return fig, ax
    
    def render_routes(self, routes, figsize=(12, 10), show_roads=True, separate_plots=False):
        """
        Render the routes for each truck.
        
        Args:
            routes: List of routes, where each route is a dict with truck, bins, etc.
            figsize: Size of the figure (width, height)
            show_roads: Whether to show roads between bins
            separate_plots: Whether to create a separate plot for each route
            
        Returns:
            tuple or list: (figure, axis) or list of (figure, axis) tuples
        """
        if separate_plots:
            return self._render_routes_separate(routes, figsize, show_roads)
        
        # Create a single plot with all routes
        fig, ax = self.render_city_map(figsize, show_roads)
        
        # Extract positions of bins
        pos = {node: self.city.graph.nodes[node]['pos'] for node in self.city.graph.nodes()
              if 'pos' in self.city.graph.nodes[node]}
        
        # Define colors for different trucks/routes
        route_colors = plt.cm.tab10.colors
        
        # Draw each route
        for i, route in enumerate(routes):
            color = route_colors[i % len(route_colors)]
            truck = route['truck']
            bins = route['bins']
            
            # Draw the route as a path
            route_edges = []
            for j in range(len(bins) - 1):
                if bins[j] in pos and bins[j+1] in pos:
                    route_edges.append((bins[j], bins[j+1]))
            
            nx.draw_networkx_edges(self.city.graph, pos, edgelist=route_edges,
                                 width=2.0, edge_color=color, ax=ax)
            
            # Draw arrows to show direction
            for j in range(len(bins) - 1):
                if bins[j] in pos and bins[j+1] in pos:
                    x1, y1 = pos[bins[j]]
                    x2, y2 = pos[bins[j+1]]
                    dx, dy = x2 - x1, y2 - y1
                    
                    # Draw arrow at midpoint
                    midx, midy = (x1 + x2) / 2, (y1 + y2) / 2
                    length = np.sqrt(dx*dx + dy*dy)
                    
                    if length > 0:
                        ax.arrow(midx - dx/length*2, midy - dy/length*2, 
                               dx/length*4, dy/length*4, 
                               head_width=1.5, head_length=1.5, 
                               fc=color, ec=color, alpha=0.7)
            
            # Add a label for the route
            label = f"Truck {truck.truck_id}: {len(bins)} bins, {route['distance']:.1f} units"
            ax.plot([], [], color=color, linewidth=2, label=label)
        
        ax.legend(loc='best')
        ax.set_title('Trash Collection Routes')
        
        return fig, ax
    
    def _render_routes_separate(self, routes, figsize, show_roads):
        """Render each route on a separate plot."""
        figures = []
        
        for i, route in enumerate(routes):
            fig, ax = self.render_city_map(figsize, show_roads)
            
            # Extract positions of bins
            pos = {node: self.city.graph.nodes[node]['pos'] for node in self.city.graph.nodes()
                  if 'pos' in self.city.graph.nodes[node]}
            
            color = plt.cm.tab10.colors[i % len(plt.cm.tab10.colors)]
            truck = route['truck']
            bins = route['bins']
            
            # Draw the route as a path
            route_edges = []
            for j in range(len(bins) - 1):
                if bins[j] in pos and bins[j+1] in pos:
                    route_edges.append((bins[j], bins[j+1]))
            
            nx.draw_networkx_edges(self.city.graph, pos, edgelist=route_edges,
                                 width=2.0, edge_color=color, ax=ax)
            
            # Draw arrows to show direction
            for j in range(len(bins) - 1):
                if bins[j] in pos and bins[j+1] in pos:
                    x1, y1 = pos[bins[j]]
                    x2, y2 = pos[bins[j+1]]
                    dx, dy = x2 - x1, y2 - y1
                    
                    # Draw arrow at midpoint
                    midx, midy = (x1 + x2) / 2, (y1 + y2) / 2
                    length = np.sqrt(dx*dx + dy*dy)
                    
                    if length > 0:
                        ax.arrow(midx - dx/length*2, midy - dy/length*2, 
                               dx/length*4, dy/length*4, 
                               head_width=1.5, head_length=1.5, 
                               fc=color, ec=color, alpha=0.7)
            
            # Add a title for the route
            ax.set_title(f"Route for Truck {truck.truck_id}: {len(bins)} bins, {route['distance']:.1f} units")
            
            figures.append((fig, ax))
        
        return figures
    
    def render_clusters(self, clusters, figsize=(12, 10), show_roads=True):
        """
        Render the clusters identified by the clustering algorithm.
        
        Args:
            clusters: List of clusters, where each cluster is a list of bin IDs
            figsize: Size of the figure (width, height)
            show_roads: Whether to show roads between bins
            
        Returns:
            tuple: (figure, axis)
        """
        fig, ax = self.render_city_map(figsize, show_roads=False)
        
        # Extract positions of bins
        pos = {node: self.city.graph.nodes[node]['pos'] for node in self.city.graph.nodes()
              if 'pos' in self.city.graph.nodes[node]}
        
        # Define colors for different clusters
        cluster_colors = plt.cm.tab10.colors
        
        # Draw each cluster
        for i, cluster in enumerate(clusters):
            color = cluster_colors[i % len(cluster_colors)]
            
            # Draw nodes in this cluster
            node_subset = [node for node in cluster if node in pos]
            nx.draw_networkx_nodes(self.city.graph, pos, 
                                 nodelist=node_subset,
                                 node_color=color, 
                                 node_size=100, 
                                 alpha=0.8, 
                                 ax=ax)
            
            # Draw edges between nodes in this cluster
            if show_roads:
                edge_subset = [(u, v) for u, v in self.city.graph.edges() 
                             if u in cluster and v in cluster]
                nx.draw_networkx_edges(self.city.graph, pos, 
                                     edgelist=edge_subset,
                                     width=1.0, 
                                     edge_color=color, 
                                     alpha=0.5, 
                                     ax=ax)
            
            # Draw a convex hull around the cluster
            if len(node_subset) > 2:
                points = np.array([pos[node] for node in node_subset])
                hull = self._compute_convex_hull(points)
                
                if len(hull) > 2:
                    hull_points = points[hull]
                    hull_points = np.append(hull_points, [hull_points[0]], axis=0)  # Close the hull
                    
                    ax.fill(hull_points[:, 0], hull_points[:, 1], 
                          color=color, alpha=0.2)
            
            # Add a label for the cluster
            label = f"Cluster {i+1}: {len(cluster)} bins"
            ax.plot([], [], color=color, linewidth=2, label=label)
        
        ax.legend(loc='best')
        ax.set_title('Bin Clusters')
        
        return fig, ax
    
    def _compute_convex_hull(self, points):
        """
        Compute the convex hull of a set of points.
        
        Args:
            points: numpy array of points (x, y)
            
        Returns:
            numpy array: Indices of hull vertices in the input points array
        """
        from scipy.spatial import ConvexHull
        hull = ConvexHull(points)
        return hull.vertices
    
    def render_metrics(self, metrics, figsize=(10, 6)):
        """
        Render metrics as bar charts.
        
        Args:
            metrics: Dictionary of metrics
            figsize: Size of the figure (width, height)
            
        Returns:
            tuple: (figure, axis)
        """
        fig, ax = plt.subplots(figsize=figsize)
        
        # Metrics to display
        display_metrics = [
            ('total_distance', 'Total Distance'),
            ('total_bins_collected', 'Total Bins Collected'),
            ('high_priority_bins_collected', 'High Priority Bins Collected'),
            ('route_count', 'Number of Routes'),
            ('average_bins_per_route', 'Avg. Bins per Route'),
            ('average_distance_per_route', 'Avg. Distance per Route')
        ]
        
        # Extract values
        labels = [label for _, label in display_metrics]
        values = [metrics[key] for key, _ in display_metrics]
        
        # Create bar chart
        bars = ax.bar(range(len(labels)), values)
        
        # Add value labels on top of bars
        for bar, value in zip(bars, values):
            height = bar.get_height()
            ax.text(bar.get_x() + bar.get_width()/2., height + 0.1,
                  f'{value:.1f}', ha='center', va='bottom')
        
        ax.set_title('Route Metrics')
        ax.set_ylabel('Value')
        
        # Fix: Set tick positions first, then labels
        ax.set_xticks(range(len(labels)))
        ax.set_xticklabels(labels, rotation=45, ha='right')
        
        plt.tight_layout()
        
        return fig, ax
    
    def save_visualizations(self, routes, output_dir, metrics=None):
        """
        Save all visualizations to files.
        
        Args:
            routes: List of routes, where each route is a dict with truck, bins, etc.
            output_dir: Directory to save the visualizations
            metrics: Optional metrics to visualize
        """
        import os
        os.makedirs(output_dir, exist_ok=True)
        
        try:
            # Try to import tqdm for progress bar
            from tqdm import tqdm
            use_tqdm = True
        except ImportError:
            use_tqdm = False
            
        print("Saving visualizations...")
        
        # Save city map
        fig, _ = self.render_city_map()
        fig.savefig(os.path.join(output_dir, 'city_map.png'), dpi=300, bbox_inches='tight')
        plt.close(fig)
        print("Saved city map")
        
        # Save combined routes
        fig, _ = self.render_routes(routes)
        fig.savefig(os.path.join(output_dir, 'routes.png'), dpi=300, bbox_inches='tight')
        plt.close(fig)
        print("Saved combined routes")
        
        # Save individual routes
        figs = self._render_routes_separate(routes, (10, 8), True)
        if use_tqdm:
            for i, (fig, _) in enumerate(tqdm(figs, desc="Saving individual routes")):
                fig.savefig(os.path.join(output_dir, f'route_{i+1}.png'), dpi=300, bbox_inches='tight')
                plt.close(fig)
        else:
            print(f"Saving {len(figs)} individual routes...")
            for i, (fig, _) in enumerate(figs):
                fig.savefig(os.path.join(output_dir, f'route_{i+1}.png'), dpi=300, bbox_inches='tight')
                plt.close(fig)
                if (i + 1) % 5 == 0 or i == len(figs) - 1:
                    print(f"Saved {i + 1}/{len(figs)} routes")
        
        # Save metrics if provided
        if metrics:
            fig, _ = self.render_metrics(metrics)
            fig.savefig(os.path.join(output_dir, 'metrics.png'), dpi=300, bbox_inches='tight')
            plt.close(fig)
            print("Saved metrics visualization")
            
        print(f"All visualizations saved to {output_dir}")