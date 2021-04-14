package texel.texel_pocketmaps.DataClasses;

public class Order {
    private Integer type;
    private String passengerName;
    private Double p1Lat, p1Lon, p2Lat, p2Lon;         //GeoPoInteger Values

    public Order() {
    }

    public Order(Integer type, String passengerName, Double p1Lat, Double p1Lon, Double p2Lat, Double p2Lon) {
        this.type = type;
        this.passengerName = passengerName;
        this.p1Lat = p1Lat;
        this.p1Lon = p1Lon;
        this.p2Lat = p2Lat;
        this.p2Lon = p2Lon;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getPassengerName() {
        return passengerName;
    }

    public void setPassengerName(String passengerName) {
        this.passengerName = passengerName;
    }

    public Double getP1Lat() {
        return p1Lat;
    }

    public void setP1Lat(Double p1Lat) {
        this.p1Lat = p1Lat;
    }

    public Double getP1Lon() {
        return p1Lon;
    }

    public void setP1Lon(Double p1Lon) {
        this.p1Lon = p1Lon;
    }

    public Double getP2Lat() {
        return p2Lat;
    }

    public void setP2Lat(Double p2Lat) {
        this.p2Lat = p2Lat;
    }

    public Double getP2Lon() {
        return p2Lon;
    }

    public void setP2Lon(Double p2Lon) {
        this.p2Lon = p2Lon;
    }
}
