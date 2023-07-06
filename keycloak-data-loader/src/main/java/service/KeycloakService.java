package service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import model.csv.UserData;

public class KeycloakService {

	private static final String AT_IDIR = "@idir";
	
	private RealmResource realmResource;
	
	public KeycloakService(Properties configProperties) {
		super();
		init(configProperties);
	}

	public void init(Properties configProperties) {
		System.out.println("Initializing Keycloak connection...");
		
		Keycloak keycloak = KeycloakBuilder.builder()
				.serverUrl(configProperties.getProperty("url"))
				.realm(configProperties.getProperty("realm"))
				.grantType(OAuth2Constants.PASSWORD)
				.clientId(configProperties.getProperty("client-id")) //
				.username(configProperties.getProperty("username"))
				.password(configProperties.getProperty("password"))
				.build();
				
		realmResource = keycloak.realm(configProperties.getProperty("realm"));
		
		System.out.println("Keycloak connection initialized.");	
	}

	public void updateKeycloakData(String clientId, List<UserData> userDataList) {

		System.out.println("Begin updating Keycloak data...");
		
		List<ClientRepresentation> clientRepresentations = realmResource.clients().findByClientId(clientId);
		ClientRepresentation clientRepresentation = clientRepresentations.get(0);
		ClientResource clientResource = realmResource.clients().get(clientRepresentation.getId());

		System.out.println("\r\nGetting roles for Client: " + clientId);
		Map<String, RoleRepresentation> clientRoles = retrieveClientRoles(clientResource);
		
		UsersResource usersResource = realmResource.users();
		
		userDataList.forEach(ud -> {
			String username = buildUsername(ud);
			
			UserRepresentation userRepresentation = processUsername(usersResource, username);
			
			processRoles(clientRepresentation, usersResource, clientRoles, ud, username, userRepresentation);
		});

		System.out.println("Completed updating Keycloak data...");
	}

	private Map<String, RoleRepresentation> retrieveClientRoles(ClientResource clientResource) {		
		
		Map<String, RoleRepresentation> clientRoles = new HashMap<>();
		List<RoleRepresentation> roleRepresentationList = clientResource.roles().list();		
		roleRepresentationList.forEach(rr -> {
			clientRoles.put(rr.getName().toLowerCase(), rr);
			System.out.println("Role: " + rr.getName());
		});
		
		return clientRoles;
	}
	
	private UserRepresentation processUsername(UsersResource usersResource, String username) {
		System.out.println("Processing username...\r\n");
		
		List<UserRepresentation> userSearchResults = usersResource.search(username, true);
		if (userSearchResults.isEmpty()) {
			System.out.println("User did not exist.");
			UserRepresentation userRepresentation = new UserRepresentation();
			userRepresentation.setUsername(username);
			userRepresentation.setEnabled(true);

			Response newUser = usersResource.create(userRepresentation);
			if (newUser.getStatus() == HttpStatus.SC_CREATED) {
				System.out.println("User created with resource URL path: " + newUser.getLocation().getPath());
			}
			userSearchResults = usersResource.search(username);
		}
		
		UserRepresentation userRepresentation = userSearchResults.get(0);
		return userRepresentation;
	}

	private void processRoles(ClientRepresentation clientRepresentation, UsersResource usersResource,
			Map<String, RoleRepresentation> clientRoles, UserData ud, String username,
			UserRepresentation userRepresentation) {
		System.out.println("Processing roles for user...");
		
		UserResource userResource = usersResource.get(userRepresentation.getId());
		RoleMappingResource roleMappingResource = userResource.roles();
		System.out.println("\r\nEffective Roles for User " + username + " before update...");
		final List<RoleRepresentation> effectiveRoles = roleMappingResource.clientLevel(clientRepresentation.getId()).listEffective();
		for (RoleRepresentation ef : effectiveRoles) {
			System.out.println("Role: " + ef.getId() + ", " + ef.getName());
		}
		
		String[] requestedRolesArray = ud.getRoles().split(",");
		System.out.println("\r\nRequested Roles: " + Arrays.toString(requestedRolesArray));
		List<String> requestedRoles = Arrays.asList(requestedRolesArray);
		if (!requestedRoles.isEmpty()) {
			List<RoleRepresentation> requestedRoleRepresentations = new ArrayList<RoleRepresentation>();
			
			requestedRoles.forEach(rr -> {
				// Check if user has a requested role already, if so no need to add it
				if (!userHasRole(effectiveRoles, rr)) {
					RoleRepresentation roleRepresentation = clientRoles.get(StringUtils.strip(rr).toLowerCase());
					if (roleRepresentation != null) {
						requestedRoleRepresentations.add(roleRepresentation);
					} else {
						System.out.println("Requested Role " + rr + " does not exist for client.");
					}
				} else {
					System.out.println("User already has requested Role: " + rr);
				}
				
			});
			if (!requestedRoleRepresentations.isEmpty()) { System.out.println("Adding requested roles: " + Arrays.toString(requestedRoleRepresentations.toArray()));
				roleMappingResource.clientLevel(clientRepresentation.getId()).add(requestedRoleRepresentations);
			}
		}
		
		System.out.println("\r\nEffective Roles for User " + username + " after update...");
		List<RoleRepresentation> effectiveRolesAfter = userResource.roles().clientLevel(clientRepresentation.getId()).listEffective();
		for (RoleRepresentation ef : effectiveRolesAfter) {
			System.out.println("Role: " + ef.getId() + ", " + ef.getName());
		}
		
		System.out.println("Finished processing roles for user.");
	}

	private boolean userHasRole(List<RoleRepresentation> effectiveRoles, String rr) {
		return effectiveRoles.stream().anyMatch(ef -> { return StringUtils.equalsIgnoreCase(ef.getName(), rr); });
	}

	private String buildUsername(UserData ud) {
		System.out.println("\r\nUsername specified as: " + ud.getUsername());
		String username = addIdirSuffix(ud.getUsername());
		System.out.println("Processing as Username: " + username);
		return username;
	}

	private String addIdirSuffix(String username) {
		if (!username.endsWith(AT_IDIR)) {
			username += AT_IDIR;
		}
		return username;
	}

}
