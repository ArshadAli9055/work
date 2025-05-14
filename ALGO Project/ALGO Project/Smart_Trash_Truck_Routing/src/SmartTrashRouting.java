import java.io.*;
import java.util.*;

public class SmartTrashRouting {
    public static void main(String[] args) {
        String binFilePath = "bins.csv";
        String roadNetworkFilePath = "roads.csv";
        String truckFilePath = "trucks.csv";
        CityMap cityMap = loadCityData(binFilePath, roadNetworkFilePath, truckFilePath);
        List<TruckRoute> routes = generateRoutes(cityMap);
        printRoutes(routes);
        simulateRealTimeUpdate(cityMap, routes);
    }

    private static CityMap loadCityData(String binFilePath, String roadNetworkFilePath, String truckFilePath) {
        CityMap cityMap = new CityMap();
        
        try {
            // Load bins
            BufferedReader binReader = new BufferedReader(new FileReader(binFilePath));
            String line;
            binReader.readLine(); // Skip header
            
            while ((line = binReader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue; // Skip empty lines
                
                String[] parts = line.split(",");
                if (parts.length != 5) {
                    System.err.println("Skipping invalid line in bins.csv: " + line);
                    continue;
                }
                
                try {
                    int id = Integer.parseInt(parts[0].trim());
                    double x = Double.parseDouble(parts[1].trim());
                    double y = Double.parseDouble(parts[2].trim());
                    double fillLevel = Double.parseDouble(parts[3].trim());
                    double capacity = Double.parseDouble(parts[4].trim());
                    
                    TrashBin bin = new TrashBin(id, x, y, fillLevel, capacity);
                    cityMap.addBin(bin);
                } catch (NumberFormatException e) {
                    System.err.println("Skipping line with invalid number format in bins.csv: " + line);
                    continue;
                }
            }
            binReader.close();
            
            // Load roads
            BufferedReader roadReader = new BufferedReader(new FileReader(roadNetworkFilePath));
            roadReader.readLine(); // Skip header
            
            while ((line = roadReader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue; // Skip empty lines
                
                String[] parts = line.split(",");
                if (parts.length != 3) {
                    System.err.println("Skipping invalid line in roads.csv: " + line);
                    continue;
                }
                
                try {
                    int startBinId = Integer.parseInt(parts[0].trim());
                    int endBinId = Integer.parseInt(parts[1].trim());
                    double distance = Double.parseDouble(parts[2].trim());
                    
                    cityMap.addRoad(startBinId, endBinId, distance);
                } catch (NumberFormatException e) {
                    System.err.println("Skipping line with invalid number format in roads.csv: " + line);
                    continue;
                }
            }
            roadReader.close();
            
            // Load trucks
            BufferedReader truckReader = new BufferedReader(new FileReader(truckFilePath));
            truckReader.readLine(); // Skip header
            
            while ((line = truckReader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue; // Skip empty lines
                
                String[] parts = line.split(",");
                if (parts.length != 2) {
                    System.err.println("Skipping invalid line in trucks.csv: " + line);
                    continue;
                }
                
                try {
                    int id = Integer.parseInt(parts[0].trim());
                    double capacity = Double.parseDouble(parts[1].trim());
                    
                    Truck truck = new Truck(id, capacity);
                    cityMap.addTruck(truck);
                } catch (NumberFormatException e) {
                    System.err.println("Skipping line with invalid number format in trucks.csv: " + line);
                    continue;
                }
            }
            truckReader.close();
            
        } catch (IOException e) {
            System.err.println("Error loading city data: " + e.getMessage());
            e.printStackTrace();
        }
        
        return cityMap;
    }

    private static List<TruckRoute> generateRoutes(CityMap cityMap) {
        List<Road> mstRoads = cityMap.applyKruskalsAlgorithm();
        List<List<TrashBin>> clusters = cityMap.identifyClusters();
        PriorityQueue<TrashBin> priorityBins = cityMap.prioritizeBins();
        List<TruckRoute> routes = new ArrayList<>();
        List<Truck> trucks = cityMap.getTrucks();
        for (Truck truck : trucks) {
            routes.add(new TruckRoute(truck));
        }
        while (!priorityBins.isEmpty()) {
            TrashBin bin = priorityBins.poll();
            boolean assigned = false;
            for (TruckRoute route : routes) {
                if (route.canAddBin(bin)) {
                    route.addBin(bin);
                    assigned = true;
                    break;
                }
            }
            if (!assigned) {
                System.out.println("Warning: Not enough truck capacity for bin " + bin.getId());
            }
        }
        
        for (TruckRoute route : routes) {
            if (!route.getBins().isEmpty()) {
                route.optimizeSequence(cityMap);
            }
        }
        
        return routes;
    }
    
    private static void printRoutes(List<TruckRoute> routes) {
        System.out.println("\n=== SMART TRASH TRUCK ROUTING RESULTS ===\n");
        
        for (int i = 0; i < routes.size(); i++) {
            TruckRoute route = routes.get(i);
            System.out.println("TRUCK #" + route.getTruck().getId() + ":");
            System.out.println("Capacity: " + route.getTruck().getCapacity());
            System.out.println("Total Load: " + route.getCurrentLoad());
            System.out.println("Total Distance: " + String.format("%.2f", route.getTotalDistance()) + " units");
            System.out.println("Estimated Fuel Consumption: " + String.format("%.2f", route.getTotalDistance() * 0.1) + " liters");
            
            System.out.println("\nRoute Sequence:");
            List<TrashBin> binSequence = route.getBinSequence();
            if (binSequence.isEmpty()) {
                System.out.println("No bins assigned to this truck.");
            } else {
                System.out.print("Depot → ");
                for (int j = 0; j < binSequence.size(); j++) {
                    TrashBin bin = binSequence.get(j);
                    System.out.print("Bin " + bin.getId() + " (Fill: " + String.format("%.1f%%", bin.getFillLevel() * 100) + ")");
                    if (j < binSequence.size() - 1) {
                        System.out.print(" → ");
                    }
                }
                System.out.println(" → Depot");
            }
            System.out.println("\n------------------------------------\n");
        }
    }
    
    private static void simulateRealTimeUpdate(CityMap cityMap, List<TruckRoute> routes) {
        System.out.println("=== SIMULATING REAL-TIME UPDATE ===\n");
        List<TrashBin> allBins = cityMap.getAllBins();
        if (!allBins.isEmpty()) {
            Random random = new Random();
            TrashBin binToUpdate = allBins.get(random.nextInt(allBins.size()));
            double newFillLevel = Math.min(1.0, binToUpdate.getFillLevel() + 0.5); // Increase fill level by 50%
            
            System.out.println("Real-time update received:");
            System.out.println("Bin #" + binToUpdate.getId() + " fill level changed from " + 
                String.format("%.1f%%", binToUpdate.getFillLevel() * 100) + " to " + 
                String.format("%.1f%%", newFillLevel * 100));
            
            binToUpdate.setFillLevel(newFillLevel);
            
            boolean binAssigned = false;
            TruckRoute assignedRoute = null;
            
            for (TruckRoute route : routes) {
                if (route.containsBin(binToUpdate)) {
                    binAssigned = true;
                    assignedRoute = route;
                    break;
                }
            }
            if (newFillLevel > 0.8 && !binAssigned) {
                System.out.println("Bin #" + binToUpdate.getId() + " is now critical and needs to be added to a route.");
                for (TruckRoute route : routes) {
                    if (route.canAddBin(binToUpdate)) {
                        route.addBin(binToUpdate);
                        route.optimizeSequence(cityMap); // Re-optimize the sequence
                        System.out.println("Added Bin #" + binToUpdate.getId() + " to Truck #" + route.getTruck().getId());
                        binAssigned = true;
                        assignedRoute = route;
                        break;
                    }
                }
                
                if (!binAssigned) {
                    System.out.println("No truck has available capacity for this bin. Need additional trip.");
                }
            }
            
            if (binAssigned && assignedRoute != null) {
                System.out.println("Re-optimizing route for Truck #" + assignedRoute.getTruck().getId());
                assignedRoute.optimizeSequence(cityMap);
            }
            System.out.println("\n=== UPDATED ROUTES ===\n");
            printRoutes(routes);
        }
    }
}