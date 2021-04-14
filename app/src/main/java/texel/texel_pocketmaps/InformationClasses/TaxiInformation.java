package texel.texel_pocketmaps.InformationClasses;

public class TaxiInformation extends CommonInformation {

    private String identificationCardNumber, carNumber, carColor, carYear, carModel, registrationNumber;
    private int category;

    public TaxiInformation(String name, String carNumber, String carColor, String carYear, String carModel,
                           String phoneNumber, String identificationCardNumber,
                           String registrationNumber, int category, String password) {
        super(name, phoneNumber, password, 3);
        this.identificationCardNumber = identificationCardNumber;
        this.carNumber = carNumber;
        this.carColor = carColor;
        this.carYear = carYear;
        this.carModel = carModel;
        this.registrationNumber = registrationNumber;
        this.category = category;
    }

    public String getIdentificationCardNumber() {
        return identificationCardNumber;
    }

    public void setIdentificationCardNumber(String identificationCardNumber) {
        this.identificationCardNumber = identificationCardNumber;
    }

    public String getCarNumber() {
        return carNumber;
    }

    public void setCarNumber(String carNumber) {
        this.carNumber = carNumber;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
    }

    public int getCategory() {
        return category;
    }

    public void setCategory(int category) {
        this.category = category;
    }

    public String getCarColor() {
        return carColor;
    }

    public void setCarColor(String carColor) {
        this.carColor = carColor;
    }

    public String getCarYear() {
        return carYear;
    }

    public void setCarYear(String carYear) {
        this.carYear = carYear;
    }

    public String getCarModel() {
        return carModel;
    }

    public void setCarModel(String carModel) {
        this.carModel = carModel;
    }
}
