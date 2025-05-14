class Road {
    private int startBinId;
    private int endBinId;
    private double distance;
    
    public Road(int startBinId, int endBinId, double distance) {
        this.startBinId = startBinId;
        this.endBinId = endBinId;
        this.distance = distance;
    }
    
    public int getStartBinId() {
        return startBinId;
    }
    
    public int getEndBinId() {
        return endBinId;
    }
    
    public double getDistance() {
        return distance;
    }
}