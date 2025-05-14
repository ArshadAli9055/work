import java.util.*;

class CityMap {
    private Map<Integer, TrashBin> binsMap;
    private Map<Integer, List<Road>> adjacencyList;
    private List<Truck> trucks;
    private BPlusTree binsTree;
    
    public CityMap() {
        binsMap = new HashMap<>();
        adjacencyList = new HashMap<>();
        trucks = new ArrayList<>();
        binsTree = new BPlusTree(4); // Order 4 B+ Tree
    }

    public void addBin(TrashBin bin) {
        binsMap.put(bin.getId(), bin);
        adjacencyList.put(bin.getId(), new ArrayList<>());
        binsTree.insert(bin.getId(), bin); // Add to B+ Tree
    }

    public void addRoad(int startBinId, int endBinId, double distance) {
        Road road = new Road(startBinId, endBinId, distance);
        adjacencyList.get(startBinId).add(road);
        Road reverseRoad = new Road(endBinId, startBinId, distance);
        adjacencyList.get(endBinId).add(reverseRoad);
    }

    public void addTruck(Truck truck) {
        trucks.add(truck);
    }

    public List<TrashBin> getAllBins() {
        return new ArrayList<>(binsMap.values());
    }

    public List<Truck> getTrucks() {
        return new ArrayList<>(trucks);
    }

    public TrashBin getBin(int id) {
        TrashBin bin = (TrashBin) binsTree.search(id);
        if (bin == null) {
            // Fallback to HashMap
            return binsMap.get(id);
        }
        return bin;
    }

    public double getDistance(int startBinId, int endBinId) {
        List<Road> roads = adjacencyList.get(startBinId);
        for (Road road : roads) {
            if (road.getEndBinId() == endBinId) {
                return road.getDistance();
            }
        }
        TrashBin startBin = getBin(startBinId);
        TrashBin endBin = getBin(endBinId);
        
        if (startBin != null && endBin != null) {
            double dx = startBin.getX() - endBin.getX();
            double dy = startBin.getY() - endBin.getY();
            return Math.sqrt(dx * dx + dy * dy);
        }
        
        return Double.MAX_VALUE; // No path exists
    }

    public List<Road> applyKruskalsAlgorithm() {
        List<Road> allRoads = new ArrayList<>();
        for (int binId : adjacencyList.keySet()) {
            for (Road road : adjacencyList.get(binId)) {
                if (road.getStartBinId() < road.getEndBinId()) {
                    allRoads.add(road);
                }
            }
        }
        Collections.sort(allRoads, Comparator.comparingDouble(Road::getDistance));
        Map<Integer, Integer> parent = new HashMap<>();
        for (int binId : binsMap.keySet()) {
            parent.put(binId, binId); // Each bin is its own parent initially
        }
        List<Road> mstRoads = new ArrayList<>();
        for (Road road : allRoads) {
            int startRoot = find(parent, road.getStartBinId());
            int endRoot = find(parent, road.getEndBinId());
            if (startRoot != endRoot) {
                mstRoads.add(road);
                union(parent, startRoot, endRoot);
            }
            if (mstRoads.size() == binsMap.size() - 1) {
                break;
            }
        }
        
        return mstRoads;
    }

    private int find(Map<Integer, Integer> parent, int x) {
        if (parent.get(x) != x) {
            parent.put(x, find(parent, parent.get(x))); // Path compression
        }
        return parent.get(x);
    }

    private void union(Map<Integer, Integer> parent, int x, int y) {
        parent.put(x, y);
    }

    public List<List<TrashBin>> identifyClusters() {
        List<List<TrashBin>> clusters = new ArrayList<>();
        List<TrashBin> allBins = new ArrayList<>(binsMap.values());
        Collections.sort(allBins, Comparator.comparingDouble(TrashBin::getX));
        List<TrashBin> currentCluster = new ArrayList<>();
        double maxDistance = 10.0; // Maximum distance to consider bins in the same cluster
        
        for (int i = 0; i < allBins.size(); i++) {
            TrashBin bin = allBins.get(i);
            
            if (currentCluster.isEmpty()) {
                currentCluster.add(bin);
            } else {
                TrashBin lastBin = currentCluster.get(currentCluster.size() - 1);
                double distance = Math.abs(bin.getX() - lastBin.getX());
                
                if (distance <= maxDistance) {
                    currentCluster.add(bin);
                } else {
                    if (currentCluster.size() > 1) {
                        clusters.add(new ArrayList<>(currentCluster));
                    }
                    currentCluster.clear();
                    currentCluster.add(bin);
                }
            }
        }
        if (currentCluster.size() > 1) {
            clusters.add(currentCluster);
        }
        Collections.sort(allBins, Comparator.comparingDouble(TrashBin::getY));
        currentCluster.clear();
        
        for (int i = 0; i < allBins.size(); i++) {
            TrashBin bin = allBins.get(i);
            
            if (currentCluster.isEmpty()) {
                currentCluster.add(bin);
            } else {
                TrashBin lastBin = currentCluster.get(currentCluster.size() - 1);
                double distance = Math.abs(bin.getY() - lastBin.getY());
                
                if (distance <= maxDistance) {
                    currentCluster.add(bin);
                } else {
                    if (currentCluster.size() > 1) {
                        // Check if this cluster overlaps with existing ones
                        boolean overlap = false;
                        for (List<TrashBin> existingCluster : clusters) {
                            Set<Integer> existingIds = new HashSet<>();
                            for (TrashBin existingBin : existingCluster) {
                                existingIds.add(existingBin.getId());
                            }
                            
                            int overlapCount = 0;
                            for (TrashBin clusterBin : currentCluster) {
                                if (existingIds.contains(clusterBin.getId())) {
                                    overlapCount++;
                                }
                            }
                            
                            if (overlapCount > 0) {
                                overlap = true;
                                break;
                            }
                        }
                        
                        if (!overlap) {
                            clusters.add(new ArrayList<>(currentCluster));
                        }
                    }
                    currentCluster.clear();
                    currentCluster.add(bin);
                }
            }
        }
        if (currentCluster.size() > 1) {
            boolean overlap = false;
            for (List<TrashBin> existingCluster : clusters) {
                Set<Integer> existingIds = new HashSet<>();
                for (TrashBin existingBin : existingCluster) {
                    existingIds.add(existingBin.getId());
                }
                
                int overlapCount = 0;
                for (TrashBin clusterBin : currentCluster) {
                    if (existingIds.contains(clusterBin.getId())) {
                        overlapCount++;
                    }
                }
                
                if (overlapCount > 0) {
                    overlap = true;
                    break;
                }
            }
            
            if (!overlap) {
                clusters.add(currentCluster);
            }
        }
        
        return clusters;
    }

    public PriorityQueue<TrashBin> prioritizeBins() {
        PriorityQueue<TrashBin> priorityQueue = new PriorityQueue<>(
            Comparator.comparingDouble(TrashBin::getFillLevel).reversed()
        );
        
        for (TrashBin bin : binsMap.values()) {
            priorityQueue.add(bin);
        }
        
        return priorityQueue;
    }
}