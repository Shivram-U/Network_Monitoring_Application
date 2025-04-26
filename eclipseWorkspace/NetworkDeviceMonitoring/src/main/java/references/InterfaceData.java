package references;


class InterfaceData {
    public Metrics inTraffic;
    public Metrics outTraffic;
    public Metrics discards;
    public Metrics errors;

    public InterfaceData(Metrics inTraffic, Metrics outTraffic, Metrics discards, Metrics errors) {
        this.inTraffic = inTraffic;
        this.outTraffic = outTraffic;
        this.discards = discards;
        this.errors = errors;
    }
}