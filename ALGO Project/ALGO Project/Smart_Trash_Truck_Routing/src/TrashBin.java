class TrashBin {
    private int id;
    private double x;
    private double y;
    private double fillLevel; // 0.0 to 1.0 (0% to 100%)
    private double capacity;
    
    public TrashBin(int id, double x, double y, double fillLevel, double capacity) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.fillLevel = fillLevel;
        this.capacity = capacity;
    }
    
    public int getId() {
        return id;
    }
    
    public double getX() {
        return x;
    }
    
    public double getY() {
        return y;
    }
    
    public double getFillLevel() {
        return fillLevel;
    }
    
    public void setFillLevel(double fillLevel) {
        this.fillLevel = fillLevel;
    }
    
    public double getCapacity() {
        return capacity;
    }
    
    public double getActualWaste() {
        return fillLevel * capacity;
    }
    
    public boolean isNearlyFull() {
        return fillLevel >= 0.8; 
    }
}