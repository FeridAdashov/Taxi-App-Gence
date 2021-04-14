package texel.texel_pocketmaps.DataClasses;

public class Message {
    public String time, message;
    public boolean status;

    public Message(String time, String message, boolean status) {
        this.time = time;
        this.message = message;
        this.status = status;
    }
}

