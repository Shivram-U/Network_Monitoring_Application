package dataArchiveServices;

class Metrics {
    public double min;
    public double max;
    public double sum;
    public double avg;

    public Metrics(double min, double max, double avg, double sum, long count) {
        this.min = min;
        this.max = max;
        this.avg = avg;
        this.sum = sum;
    }
}