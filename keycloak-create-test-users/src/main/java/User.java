import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.resource.*;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.*;

/*
Model - User.java: represents the data for a single user,
with no logic attached.

The model's job is to simply manage the data.
 Whether the data is from a database, API, or a JSON object,
 the model is responsible for managing it.

https://stackoverflow.com/questions/26396313/functions-in-mvc-model-class


An MVC model contains all of your application logic that is not contained in a view or a controller.
The model should contain all of your application business logic, validation logic, and database access logic.
For example, if you are using the Microsoft Entity Framework to access your database,
then you would create your Entity Framework classes (your .edmx file) in the Models folder.

A view should contain only logic related to generating the user interface.
A controller should only contain the bare minimum of logic required to return the right view or redirect the user
 to another action (flow control). Everything else should be contained in the model.

In general, you should strive for fat models and skinny controllers.
Your controller methods should contain only a few lines of code.
If a controller action gets too fat, then you should consider moving the logic out to a new class in the Models folder.

Note that models only care about the data they manage.
Models do not render that data to the screen, nor do they handle saving that data to some kind of persistent data store.
Their only concern is enforcing the application's rules regarding changes to those data,
and notifying anyone who cares about changes to that data.
Models are the most narcissistic of objects: they only know and care about about themselves.
* */

public class User {
    private UserRepresentation userRepresentation;
    private String password;
    private HashMap<String, Set<String>> clientRoles;

    public User(String username) {
        init();
        userRepresentation.setUsername(username);
    }

    public User(String username, String password) {
        init();
        userRepresentation.setUsername(username);
        this.password = password;
    }

    public void init() {
        userRepresentation = new UserRepresentation();
        userRepresentation.setEnabled(true);
        clientRoles = new HashMap<>();
    }

    public UserRepresentation getUserRepresentation() {
        return userRepresentation;
    }

    public String getPassword() {
        return password;
    }

    public String getUsername() {
        return userRepresentation.getUsername();
    }

    public Set<String> getClientIDs(){
        return clientRoles.keySet();
    }

    public Set<String> getClientRoles(String clientID){
        return clientRoles.get(clientID);
    }

    public HashMap<String, Set<String>> getClientRoles() {
        return clientRoles;
    }



    public void setPassword(String password) {
        this.password = password;
    }

    public void recordClientRoles(String clientName, String role) {
        if (!clientRoles.containsKey(clientName)) {
            clientRoles.put(clientName, new HashSet<String>());
        }
        clientRoles.get(clientName).add(role);
    }
}

