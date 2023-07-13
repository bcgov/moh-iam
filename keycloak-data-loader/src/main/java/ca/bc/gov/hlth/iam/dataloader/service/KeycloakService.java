package ca.bc.gov.hlth.iam.dataloader.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

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

import ca.bc.gov.hlth.iam.dataloader.model.csv.UserData;

public class KeycloakService {

	private static final String ZERO_WIDTH_NOBREAK_SPACE = "\ufeff"; //Zero Width No-Break Space (BOM, ZWNBSP) https://www.compart.com/en/unicode/U+FEFF

	private static final String AT_IDIR = "@idir";
	
	private RealmResource realmResource;
	
	public KeycloakService(Properties configProperties, EnvironmentEnum environment) {
		super();
		init(configProperties, environment);
	}

	public void init(Properties configProperties, EnvironmentEnum environment) {
		System.out.println("Initializing Keycloak connection against: " + configProperties.getProperty("url"));

		Keycloak keycloak = KeycloakBuilder.builder()
				.serverUrl(configProperties.getProperty("url"))
				.realm(configProperties.getProperty("realm"))
				.grantType(OAuth2Constants.PASSWORD)
				.clientId(configProperties.getProperty("client-id")) //
				.username(configProperties.getProperty("username"))
				.password(getUserPassword(environment))
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
			
			if (userRepresentation != null) {
				processRoles(clientRepresentation, usersResource, clientRoles, ud, username, userRepresentation);
			} else {
				throw new RuntimeException(String.format("Could not find user for %s", username));
			}
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
			//TODO (dbarrett) Look to use usersResource.get(id) as a better way to get the user.			
			userSearchResults = usersResource.search(username);
		} 
		
		if (userSearchResults.size() != 1) {
			System.out.println(String.format("Found %d users for %s", userSearchResults.size(), username));
			return null;
		}
		
		UserRepresentation userRepresentation = userSearchResults.get(0);
		System.out.println(String.format("Using user: %s; ID: %s", userRepresentation.getUsername(), userRepresentation.getId()));
		
		return userRepresentation;
	}

	private void processRoles(ClientRepresentation clientRepresentation, UsersResource usersResource,
			Map<String, RoleRepresentation> clientRoles, UserData ud, String username,
			UserRepresentation userRepresentation) {
		System.out.println("\r\nProcessing roles for user...");
		
		UserResource userResource = usersResource.get(userRepresentation.getId());
		RoleMappingResource roleMappingResource = userResource.roles();
		System.out.println("\r\nEffective Roles for User " + username + " before update...");
		List<RoleRepresentation> effectiveRoleRepresentationsBefore = roleMappingResource.clientLevel(clientRepresentation.getId()).listEffective();
		List<String> effectiveRolesBefore = effectiveRoleRepresentationsBefore.stream().map(ef ->  ef.getName()).collect(Collectors.toList());
		System.out.println("\t" + Arrays.toString(effectiveRolesBefore.toArray()));
		
		List<String> requestedRoles = getRequestedRoles(ud);
		
		if (!requestedRoles.isEmpty()) {
			List<RoleRepresentation> requestedRoleRepresentations = new ArrayList<RoleRepresentation>();
			
			requestedRoles.forEach(rr -> {
				// Check if user has a requested role already, if so no need to add it
				if (!userHasRole(effectiveRolesBefore, rr)) {
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
			if (!requestedRoleRepresentations.isEmpty()) { 
				System.out.println("Adding requested roles: " + Arrays.toString(requestedRoleRepresentations.toArray()));
				roleMappingResource.clientLevel(clientRepresentation.getId()).add(requestedRoleRepresentations);
			}
		}
		
		System.out.println("\r\nEffective Roles for User " + username + " after update...");
		List<RoleRepresentation> effectiveRoleRepresentationsAfter = roleMappingResource.clientLevel(clientRepresentation.getId()).listEffective();
		List<String> effectiveRolesAfter = effectiveRoleRepresentationsAfter.stream().map(ef ->  ef.getName()).collect(Collectors.toList());
		System.out.println("\t" + Arrays.toString(effectiveRolesAfter.toArray()));
		
		if (!validateRoles(effectiveRolesBefore, effectiveRolesAfter)) {
			System.out.println("Previously existing roles are no longer assigned to user after update.");
			throw new RuntimeException("Previously existing roles are no longer assigned to user after update.");
		}
		
		System.out.println("Finished processing roles for user.");
	}

	private List<String> getRequestedRoles(UserData ud) {
		String[] requestedRolesArray = ud.getRoles().split(",");
		System.out.println("\r\n\tRequested Roles: " + Arrays.toString(requestedRolesArray));
		return Arrays.asList(requestedRolesArray);
	}

	private boolean validateRoles(List<String> effectiveRolesBefore,
			List<String> effectiveRolesAfter) {
		return effectiveRolesBefore.stream().allMatch(efb -> effectiveRolesAfter.contains(efb));
	}

	private boolean userHasRole(List<String> effectiveRoles, String rr) {
		return effectiveRoles.stream().anyMatch(ef -> { return StringUtils.equalsIgnoreCase(ef, StringUtils.strip(rr)); });
	}

	private String buildUsername(UserData userData) {
		System.out.println("\r\nUsername specified as: " + userData.getUsername());
		
		String username = StringUtils.strip(userData.getUsername());
		if(username.contains(ZERO_WIDTH_NOBREAK_SPACE)) {
			System.out.println("\r\nUsername contained \\ufeff: " + username);			
		}
		username = username.replace(ZERO_WIDTH_NOBREAK_SPACE, "");
		if(username.contains(ZERO_WIDTH_NOBREAK_SPACE)) {
			throw new RuntimeException("Username still contains " + ZERO_WIDTH_NOBREAK_SPACE + ": " + userData.getUsername());
		}

		username = addIdirSuffix(StringUtils.strip(username));
		System.out.println("Processing as Username: " + username);
		return username;
	}

	private String addIdirSuffix(String username) {
		if (!username.endsWith(AT_IDIR)) {
			username += AT_IDIR;
		}
		return username;
	}

    private static String getUserPassword(EnvironmentEnum environment) {        
        return System.getenv(environment.getPasswordKey());
    }

}
