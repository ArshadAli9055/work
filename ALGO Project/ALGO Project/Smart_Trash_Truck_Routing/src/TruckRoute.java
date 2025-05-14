import java.util.*;

class TruckRoute {
    private Truck truck;
    private List<TrashBin> bins;
    private List<TrashBin> binSequence;
    private double currentLoad;
    private double totalDistance;
    
    public TruckRoute(Truck truck) {
        this.truck = truck;
        this.bins = new ArrayList<>();
        this.binSequence = new ArrayList<>();
        this.currentLoad = 0;
        this.totalDistance = 0;
    }
    
    public Truck getTruck() {
        return truck;
    }
    
    public List<TrashBin> getBins() {
        return bins;
    }
    
    public List<TrashBin> getBinSequence() {
        return binSequence;
    }
    
    public double getCurrentLoad() {
        return currentLoad;
    }
    
    public double getTotalDistance() {
        return totalDistance;
    }

    public boolean canAddBin(TrashBin bin) {
        return currentLoad + bin.getActualWaste() <= truck.getCapacity();
    }

    public void addBin(TrashBin bin) {
        bins.add(bin);
        currentLoad += bin.getActualWaste();
    }

    public boolean containsBin(TrashBin bin) {
        for (TrashBin routeBin : bins) {
            if (routeBin.getId() == bin.getId()) {
                return true;
            }
        }
        return false;
    }

    public void optimizeSequence(CityMap cityMap) {
        int n = bins.size();
        
        if (n == 0) {
            binSequence = new ArrayList<>();
            totalDistance = 0;
            return;
        }
        
        if (n == 1) {
            binSequence = new ArrayList<>(bins);
            TrashBin bin = bins.get(0);
            totalDistance = Math.sqrt(bin.getX() * bin.getX() + bin.getY() * bin.getY()) * 2;
            return;
        }

        List<TrashBin> nearlyFullBins = new ArrayList<>();
        List<TrashBin> regularBins = new ArrayList<>();
        
        for (TrashBin bin : bins) {
            if (bin.isNearlyFull()) {
                nearlyFullBins.add(bin);
            } else {
                regularBins.add(bin);
            }
        }
        
        binSequence = new ArrayList<>();
        binSequence.addAll(nearlyFullBins);
        if (!regularBins.isEmpty()) {
            List<TrashBin> optimalSequence = new ArrayList<>();
            
            if (regularBins.size() <= 10) {
                optimalSequence = findOptimalSequenceExact(regularBins, cityMap);
            } else {
                optimalSequence = findOptimalSequenceHeuristic(regularBins, cityMap);
            }
            
            binSequence.addAll(optimalSequence);
        }

        totalDistance = 0;
        if (!binSequence.isEmpty()) {
            TrashBin firstBin = binSequence.get(0);
            // Assuming depot is at (0,0)
            totalDistance += Math.sqrt(firstBin.getX() * firstBin.getX() + firstBin.getY() * firstBin.getY());
            
            // Distance between consecutive bins
            for (int i = 0; i < binSequence.size() - 1; i++) {
                TrashBin currentBin = binSequence.get(i);
                TrashBin nextBin = binSequence.get(i + 1);
                totalDistance += cityMap.getDistance(currentBin.getId(), nextBin.getId());
            }
            
            // Distance from last bin to depot
            TrashBin lastBin = binSequence.get(binSequence.size() - 1);
            totalDistance += Math.sqrt(lastBin.getX() * lastBin.getX() + lastBin.getY() * lastBin.getY());
        }
    }
    
    private List<TrashBin> findOptimalSequenceExact(List<TrashBin> bins, CityMap cityMap) {
        List<TrashBin> bestSequence = null;
        double minDistance = Double.MAX_VALUE;
        
        List<List<TrashBin>> permutations = generatePermutations(bins);
        
        for (List<TrashBin> sequence : permutations) {
            double distance = calculateRouteDistance(sequence, cityMap);
            
            if (distance < minDistance) {
                minDistance = distance;
                bestSequence = new ArrayList<>(sequence);
            }
        }
        
        return bestSequence != null ? bestSequence : new ArrayList<>(bins);
    }

    private List<List<TrashBin>> generatePermutations(List<TrashBin> bins) {
        List<List<TrashBin>> result = new ArrayList<>();
        generatePermutationsHelper(bins, 0, result);
        return result;
    }

    private void generatePermutationsHelper(List<TrashBin> bins, int start, List<List<TrashBin>> result) {
        if (start == bins.size() - 1) {
            result.add(new ArrayList<>(bins));
            return;
        }
        
        for (int i = start; i < bins.size(); i++) {
            // Swap
            TrashBin temp = bins.get(start);
            bins.set(start, bins.get(i));
            bins.set(i, temp);
            generatePermutationsHelper(bins, start + 1, result);
            temp = bins.get(start);
            bins.set(start, bins.get(i));
            bins.set(i, temp);
        }
    }

    private List<TrashBin> findOptimalSequenceHeuristic(List<TrashBin> bins, CityMap cityMap) {
        List<TrashBin> result = new ArrayList<>();
        boolean[] visited = new boolean[bins.size()];
        int startIndex = 0;
        double minDistToDepot = Double.MAX_VALUE;
        
        for (int i = 0; i < bins.size(); i++) {
            TrashBin bin = bins.get(i);
            double distToDepot = Math.sqrt(bin.getX() * bin.getX() + bin.getY() * bin.getY());
            
            if (distToDepot < minDistToDepot) {
                minDistToDepot = distToDepot;
                startIndex = i;
            }
        }
        
        result.add(bins.get(startIndex));
        visited[startIndex] = true;
        while (result.size() < bins.size()) {
            TrashBin lastBin = result.get(result.size() - 1);
            int nextIndex = -1;
            double minDistance = Double.MAX_VALUE;
            
            for (int i = 0; i < bins.size(); i++) {
                if (!visited[i]) {
                    double distance = cityMap.getDistance(lastBin.getId(), bins.get(i).getId());
                    
                    if (distance < minDistance) {
                        minDistance = distance;
                        nextIndex = i;
                    }
                }
            }
            
            if (nextIndex != -1) {
                result.add(bins.get(nextIndex));
                visited[nextIndex] = true;
            }
        }
        
        return result;
    }

    private double calculateRouteDistance(List<TrashBin> sequence, CityMap cityMap) {
        double distance = 0;
        if (!sequence.isEmpty()) {
            TrashBin firstBin = sequence.get(0);
            distance += Math.sqrt(firstBin.getX() * firstBin.getX() + firstBin.getY() * firstBin.getY());
            
            for (int i = 0; i < sequence.size() - 1; i++) {
                TrashBin currentBin = sequence.get(i);
                TrashBin nextBin = sequence.get(i + 1);
                distance += cityMap.getDistance(currentBin.getId(), nextBin.getId());
            }
            TrashBin lastBin = sequence.get(sequence.size() - 1);
            distance += Math.sqrt(lastBin.getX() * lastBin.getX() + lastBin.getY() * lastBin.getY());
        }
        
        return distance;
    }
}