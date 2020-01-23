/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.bc.gov.hlth.importldaptokc;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

public class ImportLdapToKc {
    
    private static String kcAccessToken;
    private static String ldapUrl;
    private static String ldapCredentials; 
    private static String objectClass;
    private static String userAttribute;
    private static String kcClientName;
    private static String kcAdminUser;
    private static String kcAdminPass;
    private static String kcBaseUrl;
    private static String kcRealmName;
    
    public static void main(String[] args) throws NamingException, InterruptedException, IOException {
        
        loadProperties();
        kcAccessToken = getKcAccessToken();
       
        importFromLdapToKcAndCreateRoles();
    }
    
    private static void loadProperties() throws IOException {
        Properties configProperties = new Properties();
        InputStream inputStream = ImportLdapToKc.class.getResourceAsStream("/configuration.properties");
        configProperties.load(inputStream);
        
        ldapUrl = configProperties.getProperty("ldap_url");
        ldapCredentials = configProperties.getProperty("ldap_credentials"); 
        objectClass = configProperties.getProperty("object_class");
        userAttribute = configProperties.getProperty("user_attribute");
        kcClientName = configProperties.getProperty("kc_client");
        kcAdminUser = configProperties.getProperty("kc_admin_user");
        kcAdminPass = configProperties.getProperty("kc_admin_pass");
        kcBaseUrl = configProperties.getProperty("kc_base_url");
        kcRealmName = configProperties.getProperty("kc_realm_name");
    }
    
    
    private static void importFromLdapToKcAndCreateRoles() throws IOException, InterruptedException, NamingException {
        
        //Get the LDAP Users and Their roles for the configured application
        HashMap<String, String> ldapUsersAndRoles = queryLdapUsersAndRoles(ldapUrl, ldapCredentials, objectClass, userAttribute);
        Set<String> ldapRoles = new HashSet<>(ldapUsersAndRoles.values()); 
        
        //Get the KC Client ID
        String kcClientId = queryKcClientId(kcClientName);
        
        //Add the Roles to the KC Client
        for (String roleName : ldapRoles) {
            addRolesToKcClient(kcClientId, roleName);
        }
        
        //Get the role Id's from the client
        HashMap<String, String> kcRolesAndRoleIds = queryKcRoleIds(kcClientId);
        
        //Get the list of User ID's    
        for (Map.Entry<String, String> entry : ldapUsersAndRoles.entrySet()) {
            
            String userId = queryKcUserId(entry.getKey());
            String role = entry.getValue();
            String roleId = kcRolesAndRoleIds.get(role);
            
            System.out.println("Adding role: " + role + " to user: " + entry.getKey());
            addRolesToKcUser(kcClientId, userId, role, roleId);        
        }       
    }
    
    //Retrieve the list of users and associated role from LDAP that map to the input object class and attribute
    private static HashMap<String, String> queryLdapUsersAndRoles(String serverUrl, String password, String objectClass, String attributeName) throws NamingException {
        Hashtable env = new Hashtable(11);
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, serverUrl);
        env.put(Context.SECURITY_PRINCIPAL, "uid=admin,o=HNET,st=BC,c=CA");
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_CREDENTIALS, password);

        NamingEnumeration<?> namingEnum = null;
        LdapContext ctx = null;
        HashMap<String, String> usersAndRoles = new HashMap<>();
             
        try {
            ctx = new InitialLdapContext(env, null);
            ctx.setRequestControls(null);
            namingEnum = ctx.search("o=hnet,st=bc,c=ca", "(objectClass=" + objectClass + ")", getSimpleSearchControls());
            while (namingEnum.hasMore()) {
                SearchResult result = (SearchResult) namingEnum.next();
                Attributes attrs = result.getAttributes();
                
                //Build a map of the users and their role
                usersAndRoles.put(attrs.get("uid").get().toString(), attrs.get(attributeName).get().toString());

            }
        } finally {
            if (ctx != null) {
                ctx.close();
            }
            if (namingEnum != null) {
                namingEnum.close();
            }
        }
        
        return usersAndRoles;      
    }
    

    //Set the search Controls for LDAP Query
    private static SearchControls getSimpleSearchControls() {
        SearchControls searchControls = new SearchControls();
        searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        searchControls.setReturningAttributes(new String[]{"uid", "fmdbuserrole"});
        searchControls.setTimeLimit(30000);
        return searchControls;
    }
    
    
    //Retrieve the list of all client roles for the input client id
    private static HashMap<String, String> queryKcRoleIds(String clientId) throws IOException, InterruptedException {

        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(kcBaseUrl + "auth/admin/realms/"+kcRealmName+"/clients/" + clientId + "/roles"))
                .header("Authorization", "Bearer " + kcAccessToken)
                .GET()
                .build();
        HttpResponse<String> response2 = httpClient.send(request, BodyHandlers.ofString());
        String body = response2.body();
        JsonArray clientRoles = JsonParser.parseString(body).getAsJsonArray();
        
        HashMap<String, String> rolesAndIds = new HashMap<>();
        
        for (JsonElement clientRole : clientRoles) {
            rolesAndIds.put(clientRole.getAsJsonObject().get("name").getAsString(), clientRole.getAsJsonObject().get("id").getAsString());
        }
        return rolesAndIds;
    }
    
    
    //Retrieve the client id from keycloak for a given client name
    private static String queryKcClientId(String clientName) throws IOException, InterruptedException {

        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(kcBaseUrl + "auth/admin/realms/"+kcRealmName+"/clients?viewableOnly=true"))
                .header("Authorization", "Bearer " + kcAccessToken)
                .GET()
                .build();
        HttpResponse<String> response2 = httpClient.send(request, BodyHandlers.ofString());
        String body = response2.body();
        JsonArray clientList = JsonParser.parseString(body).getAsJsonArray();
        String kcClientId = null;
        
        for (JsonElement kcClient : clientList) {
            if (kcClient.getAsJsonObject().get("clientId").getAsString().equals(clientName)) {
                kcClientId = kcClient.getAsJsonObject().get("id").getAsString();
            }    
        }
        return kcClientId;
    }
    
    
    //Add a role to a keycloak client based on the client id and role name
    private static void addRolesToKcClient(String clientId, String roleName) throws IOException, InterruptedException {
        
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(kcBaseUrl + "auth/admin/realms/"+kcRealmName+"/clients/" + clientId + "/roles"))
                .header("Authorization", "Bearer " + kcAccessToken)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("cache-control", "no-cache")                
                .POST(BodyPublishers.ofString("{\"name\":\"" + roleName + "\"}"))
                .build();
                
        HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
        String body = response.body();
        System.out.println(body);               
    }
    
    
    //Retrieve a KeyCloak userid given their username
    private static String queryKcUserId(String username) throws IOException, InterruptedException {    
        
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(kcBaseUrl + "auth/admin/realms/"+kcRealmName+"/users?username=" + username))
                .header("Authorization", "Bearer " + kcAccessToken)
                .GET()
                .build();
        HttpResponse<String> response2 = httpClient.send(request, BodyHandlers.ofString());
        String body = response2.body(); 
        
        JsonArray userList = JsonParser.parseString(body).getAsJsonArray();

        String kcUser = null;
        if (userList.size() != 0) {
            JsonElement userJson = userList.get(0);
            kcUser = userJson.getAsJsonObject().get("id").getAsString();
        }       
        
        return kcUser;
    }
    
    
    //TODO accept role and username and client id as parameters
    private static void addRolesToKcUser(String clientId, String userId, String role, String roleId) throws IOException, InterruptedException {
    
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(kcBaseUrl + "auth/admin/realms/"+kcRealmName+"/users/" + userId + "/role-mappings/clients/" + clientId))
                .header("Authorization", "Bearer " + kcAccessToken)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("cache-control", "no-cache")                
                .POST(BodyPublishers.ofString("[{\"id\":\"" + roleId + "\",\"name\":\"" + role + "\",\"clientRole\":true}]"))
                .build();
                
        try {
            HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
            String body = response.body();
            System.out.println(body);
        } catch (IOException ioe) {
            System.out.println("Just catching the 204 response with no content");
        }                   
    }
    
    
    //TODO Paramaterize admin credentials
    //Retrieve a keycloak access token
    private static String getKcAccessToken() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(kcBaseUrl + "auth/realms/master/protocol/openid-connect/token"))
                .header("Accept", "application/json")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("cache-control", "no-cache")
                .POST(BodyPublishers.ofString("grant_type=password&username=" + kcAdminUser + "&password=" + kcAdminPass + "&client_id=admin-cli"))
                .build();
        HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
        String body = response.body();
        String access_token = JsonParser.parseString(body).getAsJsonObject().get("access_token").getAsString();
        
        return access_token;
    }
}
