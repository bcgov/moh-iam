/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ca.bc.gov.hlth.importldaptokc;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.naming.NamingException;

/**
 * This program accepts a list of LDAP users, adds their roles to a Keycloak
 * client, and assigns the roles to Keycloak users.
 */
class KeycloakRoleLoader {

    private final String kcClientName;
    private final String kcAdminUser;
    private final String kcAdminPass;
    private final String kcBaseUrl;
    private final String kcRealmName;

    private final String kcAccessToken;
    
    private static final Logger LOG = Logger.getLogger(KeycloakRoleLoader.class.getName());
    
    KeycloakRoleLoader(String kcClientName, String kcAdminUser, String kcAdminPass, String kcBaseUrl, String kcRealmName) {
        this.kcClientName = kcClientName;
        this.kcAdminUser = kcAdminUser;
        this.kcAdminPass = kcAdminPass;
        this.kcBaseUrl = kcBaseUrl;
        this.kcRealmName = kcRealmName;

        try {
            kcAccessToken = getKcAccessToken();
        } catch (IOException | InterruptedException ex) {
            throw new IllegalStateException(ex);
        }
    }

    void loadRoles(List<LdapUser> ldapUsers) throws IOException, InterruptedException, NamingException {

        // Get the KC Client ID
        String kcClientId = queryKcClientId(kcClientName);

        // Add the Roles to the KC Client
        for (String role : ldapUsers.stream().map(LdapUser::getRole).collect(Collectors.toSet())) {
            addRolesToKcClient(kcClientId, role);
        }

        // Get the role IDs from the client
        HashMap<String, String> kcRolesAndRoleIds = queryKcRoleIds(kcClientId);

        // Get the list of User IDs    
        for (LdapUser ldapUser : ldapUsers) {

            String userId = queryKcUserId(ldapUser.getUsername());
            String role = ldapUser.getRole();
            String roleId = kcRolesAndRoleIds.get(role);

            LOG.info("Adding role: " + role + " to user: " + ldapUser.getUsername());
            addRolesToKcUser(kcClientId, userId, role, roleId);
        }
    }

    // Retrieve the list of all client roles for the input client id
    private HashMap<String, String> queryKcRoleIds(String clientId) throws IOException, InterruptedException {

        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(kcBaseUrl + "auth/admin/realms/" + kcRealmName + "/clients/" + clientId + "/roles"))
                .header("Authorization", "Bearer " + kcAccessToken)
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        String body = response.body();
        JsonArray clientRoles = JsonParser.parseString(body).getAsJsonArray();

        HashMap<String, String> rolesAndIds = new HashMap<>();

        for (JsonElement clientRole : clientRoles) {
            rolesAndIds.put(clientRole.getAsJsonObject().get("name").getAsString(), clientRole.getAsJsonObject().get("id").getAsString());
        }
        return rolesAndIds;
    }

    // Retrieve the client id from keycloak for a given client name
    private String queryKcClientId(String clientName) throws IOException, InterruptedException {

        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(kcBaseUrl + "auth/admin/realms/" + kcRealmName + "/clients?viewableOnly=true"))
                .header("Authorization", "Bearer " + kcAccessToken)
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        String body = response.body();
        JsonArray clientList = JsonParser.parseString(body).getAsJsonArray();
        String kcClientId = null;

        for (JsonElement kcClient : clientList) {
            if (kcClient.getAsJsonObject().get("clientId").getAsString().equals(clientName)) {
                kcClientId = kcClient.getAsJsonObject().get("id").getAsString();
            }
        }
        return kcClientId;
    }

    // Add a role to a keycloak client based on the client id and role name
    private void addRolesToKcClient(String clientId, String roleName) throws IOException, InterruptedException {

        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(kcBaseUrl + "auth/admin/realms/" + kcRealmName + "/clients/" + clientId + "/roles"))
                .header("Authorization", "Bearer " + kcAccessToken)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("cache-control", "no-cache")
                .POST(HttpRequest.BodyPublishers.ofString("{\"name\":\"" + roleName + "\"}"))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        String body = response.body();
        LOG.info(body);
    }

    // Retrieve a KeyCloak userid given their username
    private String queryKcUserId(String username) throws IOException, InterruptedException {

        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(kcBaseUrl + "auth/admin/realms/" + kcRealmName + "/users?username=" + username))
                .header("Authorization", "Bearer " + kcAccessToken)
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        String body = response.body();

        JsonArray userList = JsonParser.parseString(body).getAsJsonArray();

        String kcUser = null;
        if (userList.size() != 0) {
            JsonElement userJson = userList.get(0);
            kcUser = userJson.getAsJsonObject().get("id").getAsString();
        }

        return kcUser;
    }

    private void addRolesToKcUser(String clientId, String userId, String role, String roleId) throws IOException, InterruptedException {

        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(kcBaseUrl + "auth/admin/realms/" + kcRealmName + "/users/" + userId + "/role-mappings/clients/" + clientId))
                .header("Authorization", "Bearer " + kcAccessToken)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("cache-control", "no-cache")
                .POST(HttpRequest.BodyPublishers.ofString("[{\"id\":\"" + roleId + "\",\"name\":\"" + role + "\",\"clientRole\":true}]"))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();
            LOG.info(body);
        } catch (IOException ioe) {
            if (ioe.getMessage().contains("unexpected content length header with 204 response")) {
                // https://bugs.java.com/bugdatabase/view_bug.do?bug_id=8218662
                LOG.fine("Catching the exception caused by 204 response");
            } else {
                throw ioe;
            }
        }
    }

    // Retrieve a keycloak access token
    private String getKcAccessToken() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(kcBaseUrl + "auth/realms/master/protocol/openid-connect/token"))
                .header("Accept", "application/json")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("cache-control", "no-cache")
                .POST(HttpRequest.BodyPublishers.ofString("grant_type=password&username=" + kcAdminUser + "&password=" + kcAdminPass + "&client_id=admin-cli"))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        String body = response.body();
        String access_token = JsonParser.parseString(body).getAsJsonObject().get("access_token").getAsString();

        return access_token;
    }

}
