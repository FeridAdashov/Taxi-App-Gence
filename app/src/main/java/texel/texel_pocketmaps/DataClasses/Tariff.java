package texel.texel_pocketmaps.DataClasses;

public class Tariff {
    private Double from, to, every, money;

    public Tariff() {
    }

    public Tariff(Double from, Double to, Double every, Double money) {
        this.from = from;
        this.to = to;
        this.every = every;
        this.money = money;
    }

    public Double getFrom() {
        return from;
    }

    public void setFrom(Double from) {
        this.from = from;
    }

    public Double getTo() {
        return to;
    }

    public void setTo(Double to) {
        this.to = to;
    }

    public Double getEvery() {
        return every;
    }

    public void setEvery(Double every) {
        this.every = every;
    }

    public Double getMoney() {
        return money;
    }

    public void setMoney(Double money) {
        this.money = money;
    }
}
