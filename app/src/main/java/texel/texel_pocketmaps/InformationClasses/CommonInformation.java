package texel.texel_pocketmaps.InformationClasses;

public class CommonInformation {
    private String name, phoneNumber, password;
    private int statusId;

    CommonInformation(String name, String phoneNumber, String password, int statusId) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.statusId = statusId;
        this.password = password;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getStatusId() {
        return statusId;
    }

    public void setStatusId(int statusId) {
        this.statusId = statusId;
    }
}
