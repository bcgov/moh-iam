import java.util.*;


public class Client {
    private String clientName;
    private String clientID;
    private ArrayList<String> roles;

    /**
     * constructor
     */
    public Client(String clientName,String clientID,ArrayList<String> roles) {
        this.clientID = clientID;
        this.clientName = clientName;
        this.roles = roles;
    }


    public String getClientID() {
        return clientID;
    }

    public String getClientName() {
        return clientName;
    }

    public ArrayList<String> getRoles() {
        return roles;
    }
}





