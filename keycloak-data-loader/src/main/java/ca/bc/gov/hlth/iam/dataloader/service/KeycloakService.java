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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.hlth.iam.dataloader.model.csv.UserData;

public class KeycloakService {

	private static final Logger logger = LoggerFactory.getLogger(KeycloakService.class);

	private static final String CONFIG_PROPERTY_URL = "url";

	private static final String CONFIG_PROPERTY_REALM = "realm";

	private static final String CONFIG_PROPERTY_CLIENT_ID = "client-id";

	private static final String CONFIG_PROPERTY_USERNAME = "username";
	
	private static final String CONFIG_PROPERTY_USERNAME_TYPE = "username-type";

	private static final String ZERO_WIDTH_NOBREAK_SPACE = "\ufeff"; //Zero Width No-Break Space (BOM, ZWNBSP) https://www.compart.com/en/unicode/U+FEFF

	private UsernameTypeEnum usernameTypeEnum;

	private String realm;
	
	private RealmResource realmResource;

	private List<String> usersCreated = new ArrayList<>();
	
	public KeycloakService(Properties configProperties, EnvironmentEnum environment) {
		super();
		init(configProperties, environment);
	}

	public void init(Properties configProperties, EnvironmentEnum environment) {
		logger.info("Initializing Keycloak connection against: {}", configProperties.getProperty(CONFIG_PROPERTY_URL));
		realm = configProperties.getProperty(CONFIG_PROPERTY_REALM);
		logger.info("Using Realm: {}", realm);

		determineUsernameType(configProperties);			

		Keycloak keycloak = KeycloakBuilder.builder()
				.serverUrl(configProperties.getProperty(CONFIG_PROPERTY_URL))
				.realm(realm)
				.grantType(OAuth2Constants.PASSWORD)
				.clientId(configProperties.getProperty(CONFIG_PROPERTY_CLIENT_ID)) //
				.username(configProperties.getProperty(CONFIG_PROPERTY_USERNAME))
				.password(getUserPassword(environment))
				.build();
				
		realmResource = keycloak.realm(realm);
		
		logger.info("Keycloak connection initialized.");	
	}

	private void determineUsernameType(Properties configProperties) {
		String usernameType = configProperties.getProperty(CONFIG_PROPERTY_USERNAME_TYPE);
		logger.info("{} property is: {}", CONFIG_PROPERTY_USERNAME_TYPE, usernameType);
		try {
            usernameTypeEnum = UsernameTypeEnum.valueOf(usernameType);
            logger.info("Username Type Enum: {} with '{}' {}", usernameTypeEnum, usernameTypeEnum.getValue(), usernameTypeEnum.isPrefix() ? "prefix" : "suffix");
		} catch(IllegalArgumentException iae) {
	        logger.error("{} property {} is not valid. A valid username type from the allowed list must be provided.", CONFIG_PROPERTY_USERNAME_TYPE, usernameType);
			throw new RuntimeException(String.format("{} property %s is not valid. A valid username type from the allowed list must be provided.", CONFIG_PROPERTY_USERNAME_TYPE, usernameType));
		}
	}

	public void updateKeycloakData(String clientId, List<UserData> userDataList) {

		logger.info("Begin updating Keycloak data...");
		
		List<ClientRepresentation> clientRepresentations = realmResource.clients().findByClientId(clientId);
		if (clientRepresentations.size() != 1) {
			logger.error("Found incorrect number of ClientRepresentations, {}, for Client: {}", clientRepresentations.size(), clientId);
			throw new RuntimeException(String.format("Found incorrect number of ClientRepresentations, {}, for Client: {}", clientRepresentations.size(), clientId));
		}
		ClientRepresentation clientRepresentation = clientRepresentations.get(0);
		ClientResource clientResource = realmResource.clients().get(clientRepresentation.getId());

		logger.info("Getting roles for Client: {}", clientId);
		Map<String, RoleRepresentation> clientRoles = retrieveClientRoles(clientResource);
		
		UsersResource usersResource = realmResource.users();
		
		userDataList.forEach(ud -> {
			String username = buildUsername(ud);
			
			UserRepresentation userRepresentation = processUsername(usersResource, username);
			
			if (userRepresentation == null) {
				logger.error("Could not find user for {}", username);
				throw new RuntimeException(String.format("Could not find user for {}", username));
			}
			processRoles(clientRepresentation, usersResource, clientRoles, ud, username, userRepresentation);
		});

		printSummary();
		
		logger.info("Completed updating Keycloak data...");
	}

	private Map<String, RoleRepresentation> retrieveClientRoles(ClientResource clientResource) {		
		
		Map<String, RoleRepresentation> clientRoles = new HashMap<>();
		List<RoleRepresentation> roleRepresentationList = clientResource.roles().list();		
		roleRepresentationList.forEach(rr -> {
			clientRoles.put(rr.getName().toLowerCase(), rr);
			logger.debug("Role: {}", rr.getName());
		});
		
		return clientRoles;
	}
	
	private UserRepresentation processUsername(UsersResource usersResource, String username) {
		logger.info("Processing username: {}...", username);

		List<UserRepresentation> userSearchResults = usersResource.search(username, true);
		if (userSearchResults.isEmpty()) {
			logger.info("User did not exist.");
			UserRepresentation userRepresentation = new UserRepresentation();
			userRepresentation.setUsername(username);
			userRepresentation.setEnabled(true);

			Response createUserResponse = usersResource.create(userRepresentation);
			
			if (createUserResponse.getStatus() != HttpStatus.SC_CREATED) {
				logger.error("User not created due to: {}", createUserResponse.getStatus());
				throw new RuntimeException(String.format("User not created due to: {}", createUserResponse.getStatus()));
			}
			
			usersCreated.add(String.format("Username: %s; Path: %s", userRepresentation.getUsername(), createUserResponse.getLocation().getPath()));
			logger.info("User created with resource URL path: {}", createUserResponse.getLocation().getPath());
			
			//TODO (dbarrett) Look to use usersResource.get(id) as a better way to get the user.			
			userSearchResults = usersResource.search(username);
			createUserResponse.getLocation().getPath();
		} else if (userSearchResults.size() > 1) {
			logger.info("Found {} users for {}", userSearchResults.size(), username);
			return null;
		}
		
		UserRepresentation userRepresentation = userSearchResults.get(0);
		logger.info("Using user: {}; ID: {}", userRepresentation.getUsername(), userRepresentation.getId());
		
		return userRepresentation;
	}

	private void processRoles(ClientRepresentation clientRepresentation, UsersResource usersResource,
			Map<String, RoleRepresentation> clientRoles, UserData ud, String username,
			UserRepresentation userRepresentation) {
		logger.info("Processing roles for user...");
		
		UserResource userResource = usersResource.get(userRepresentation.getId());
		RoleMappingResource roleMappingResource = userResource.roles();
		logger.info("Effective Roles for User {} before update...", username);
		List<RoleRepresentation> effectiveRoleRepresentationsBefore = roleMappingResource.clientLevel(clientRepresentation.getId()).listEffective();
		List<String> effectiveRolesBefore = effectiveRoleRepresentationsBefore.stream().map(ef ->  ef.getName()).collect(Collectors.toList());
		logger.info("\t" + Arrays.toString(effectiveRolesBefore.toArray()));
		
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
						logger.error("Requested Role {} does not exist for client.", rr);
						throw new RuntimeException(String.format("Requested Role {} does not exist for client.", rr));
					}
				} else {
					logger.debug("User already has requested Role: {}", rr);
				}
				
			});
			if (!requestedRoleRepresentations.isEmpty()) { 
				logger.debug("Adding requested roles: {}", Arrays.toString(requestedRoleRepresentations.toArray()));
				roleMappingResource.clientLevel(clientRepresentation.getId()).add(requestedRoleRepresentations);
			}
		}
		
		logger.info("Effective Roles for User {} after update...", username);
		List<RoleRepresentation> effectiveRoleRepresentationsAfter = roleMappingResource.clientLevel(clientRepresentation.getId()).listEffective();
		List<String> effectiveRolesAfter = effectiveRoleRepresentationsAfter.stream().map(ef ->  ef.getName()).collect(Collectors.toList());
		logger.info("\t" + Arrays.toString(effectiveRolesAfter.toArray()));
		
		if (!validateRoles(effectiveRolesBefore, effectiveRolesAfter)) {
			logger.error("Previously existing roles are no longer assigned to user after update.");
			throw new RuntimeException("Previously existing roles are no longer assigned to user after update.");
		}
		
		logger.info("Finished processing roles for user.");
	}

	private List<String> getRequestedRoles(UserData ud) {
		String[] requestedRolesArray = ud.getRoles().split(",");
		logger.debug("\tRequested Roles: {}", Arrays.toString(requestedRolesArray));
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
		logger.debug("Username specified as: {}", userData.getUsername());
		
		String username = StringUtils.strip(userData.getUsername());
		if (username.contains(ZERO_WIDTH_NOBREAK_SPACE)) {
			logger.debug("Username contained \\ufeff: {}", username);			
			username = username.replace(ZERO_WIDTH_NOBREAK_SPACE, "");
		}

		username = addUsernameSuffix(StringUtils.strip(username));
		logger.debug("Processing as Username: {}", username);
		return username;
	}

	private String addUsernameSuffix(String username) {
		if (usernameTypeEnum != UsernameTypeEnum.NONE && !usernameComplete(username)) {
			username += usernameTypeEnum.getValue();
		}
		return username;
	}

	/*
	 * Check if the required prefix/suffix is already in the username
	 */
	private boolean usernameComplete(String username) {
		boolean isComplete = false;
		if (usernameTypeEnum.isPrefix()) {
			isComplete = StringUtils.startsWithIgnoreCase(username, usernameTypeEnum.getValue());
		} else {
			isComplete = StringUtils.endsWithIgnoreCase(username, usernameTypeEnum.getValue());
		}
		return isComplete;
	}

    private static String getUserPassword(EnvironmentEnum environment) {        
        return System.getenv(environment.getPasswordKey());
    }

	private void printSummary() {
		logger.info("**************************************************************************************************");		
		logger.info("********************************  DATA UPLOAD SUMMARY  *******************************************");		
		logger.info("**************************************************************************************************");		
		logger.info("Total Users Created: {}", usersCreated.size());
		usersCreated.forEach(uc -> {
			logger.info("Created User: {}", uc);
		});
	}

}
