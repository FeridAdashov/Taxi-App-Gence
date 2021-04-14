package texel.texel_pocketmaps.DataClasses;

public class CardViewTaxiInfo {
    public String userName;
    public boolean active;
    public int category;

    public CardViewTaxiInfo(String userName, Integer category, Boolean active) {
        this.userName = userName;
        this.active = active == null ? false : active;
        this.category = category == null ? -1 : category;
    }
}

