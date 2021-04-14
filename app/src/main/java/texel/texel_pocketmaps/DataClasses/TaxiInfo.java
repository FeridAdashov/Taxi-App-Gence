package texel.texel_pocketmaps.DataClasses;

import android.text.TextUtils;

public class TaxiInfo {
    public String registerId, name, carNumber, carColor, carYear, carModel, phone;
    public int category;

    public TaxiInfo(String registerId, String name,
                    String carNumber, String carColor, String carYear, String carModel,
                    String phone, Integer category) {

        this.registerId = TextUtils.isEmpty(registerId) ? "" : registerId;
        this.name = TextUtils.isEmpty(name) ? "" : name;
        this.carNumber = TextUtils.isEmpty(carNumber) ? "" : carNumber;
        this.carColor = TextUtils.isEmpty(carColor) ? "" : carColor;
        this.carYear = TextUtils.isEmpty(carYear) ? "" : carYear;
        this.carModel = TextUtils.isEmpty(carModel) ? "" : carModel;
        this.phone = TextUtils.isEmpty(phone) ? "" : phone;
        this.category = category == null ? 0 : category;
    }
}

