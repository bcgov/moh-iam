import org.apache.commons.text.StringSubstitutor;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.*;
import org.keycloak.representations.idm.*;

import java.io.*;
import java.util.*;
//split into classes: writing, importing, deleting

public class ClientService {
    // todo: set enum, : payara, normal or data.
    private Keycloak keycloak;
    private RealmResource realmResource;
    private FileWriter resourceFW;
    private FileWriter importFW;

    private String environmentModule;
    private String realmName;
    private String clientName;
    private String targetClientID;
    //todo: change this into enum/map thing
    private Boolean isPayaraClient = false;
    private Boolean isDataSource =false;

    private Map globals;
//    todo: add this to a map(?)
    private final String terraformImport = "terraform import ";
    //todo: add this into a map? make it include data types.
    private final String currentModule = (isPayaraClient)? "module.payara_client": "keycloak_openid_client";

    //todo: figure out a better way to do this
    private Set<String> requiredClients;

    /**
     * constructor for UserService
     * @param configPath the path to the configuration file
     */
    public ClientService(String configPath){
        try {
            setConfigurations(configPath);
            globals = new HashMap<>();
            globals.put("environmentModule",environmentModule);
            globals.put("realmName",realmName);
            globals.put("clientName",clientName);
            globals.put("targetClientID",targetClientID);
            globals.put("currentModule",currentModule);

            requiredClients = new HashSet<>();
            //todo: the method calls should be here instead of main
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * sets up keycloak. Also sets up the RealmResource
     * @param configPath
     * @throws IOException
     */
    private void setConfigurations(String configPath) throws Exception {
        Properties configProperties = new Properties();
        File file = new File(configPath);

        InputStream inputStream;
        if (file.exists()) {
            inputStream = new FileInputStream(file);
        } else {
            inputStream = Main.class.getResourceAsStream(configPath);
        }
        Objects.requireNonNull(inputStream, String.format("Configuration file not found at '%s'.", configPath));
        configProperties.load(inputStream);
        String realm = configProperties.getProperty("realm");

        resourceFW = new FileWriter(configProperties.getProperty("outputFile"));
        importFW = new FileWriter(configProperties.getProperty("importFile"));
        keycloak =  KeycloakBuilder.builder() //
                .serverUrl(configProperties.getProperty("serverURL")) //
                .realm(realm) //
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS) //
                .clientId(configProperties.getProperty("clientID")) //
                .clientSecret(configProperties.getProperty("clientSecret")) //
                .build();
        realmResource = keycloak.realm(realm);
        realmName = realm;
        clientName = configProperties.getProperty("targetClientID");
        targetClientID = getClientUUID(configProperties.getProperty("targetClientID"));
        environmentModule = configProperties.getProperty("environmentModule");
        isPayaraClient = configProperties.getProperty("isPayaraClient").equalsIgnoreCase("true");
    }

    public void createResources(){
        if(isPayaraClient) {
            writePayaraModule();
        }else{
            writeClientResource();
            writeAllRoles();
        }
            writeAllMapperResources();
            writeScopeMappingResources();
            writeServiceAccountRoles();

            writeModuleInRealm();
        }


    public void createImports(){
        if(isPayaraClient) {
            importPayaraClient();
        } else {
            importClient("");
            importAllRoles("");
        }

        importAllMapperResources();
        importScopeMappingResources();
        ImportServiceAccountRoles();
    }



// other methods


// WRITE AND IMPORT METHODS

    // "writes" the module "client_name" found on the realm tf file. it just prints it out in the console.
    // todo: rename this. consider making this write somewhere else (different file?)
    private void writeModuleInRealm() {
        String module = """
            module "${clientName}" {
                source = "./${clientNameLowerCase}"
                ${clientDependencies}
            }
            """;

        Map variables = new HashMap();
        variables.put("clientName",clientName);
        variables.put("clientNameLowerCase",clientName.toLowerCase(Locale.ROOT));

        String clientDependencies = "";
        for (String client: requiredClients){
            clientDependencies += String.format("""
                        %s= "${module.%s}"
                        """,client,client);
        }
        variables.put("clientDependencies",clientDependencies);

        System.out.println(new StringSubstitutor(variables).replace(module));
    }

//    writes output and variables todo: split this
    private void writeClientVariables() {
        String outputs = """
                        output "CLIENTS" {
                            value = ${clientType}.CLIENT
                        }
                        output "ROLES" {
                            value = ${clientType}.ROLES
                        }
                        ${variables}
                        
                        """;

        Map values = new HashMap();
        values.put("clientType",(isPayaraClient)? "module.payara-client": (isDataSource)? "data.keycloak_openid_client":"keycloak_openid_client" );

        String variables = "";
        for (String client: requiredClients){
            variables +="variable \""+client+"\" {\n" + "}\n";
        }
        values.put("variables",variables);
        write(resourceFW,new  StringSubstitutor(values).replace(outputs));
    }

    // writes the roles as a data source
    private void writeClientRolesDataSource() {
        ClientResource clientResource = realmResource.clients().get(targetClientID);
        String resource = """
                module "client-roles-data-source" {
                    source = "../../../../modules/client-roles-data-source"
                    client_id = data.${currentModule}.CLIENT.id
                    realm_id = data.${currentModule}.CLIENT.realm_id
                    roles = {${roles}
                    }
                }
                
                """;

        Map valuesMap = new HashMap();
        valuesMap.put("currentModule", currentModule);
        String roles = "";
        for (RoleRepresentation roleRepresentation : clientResource.roles().list()) {
            String roleName = roleRepresentation.getName();
            roles += String.format("\n\t\t\"%s\" = \"%s\",",roleName,roleName);
        }
        valuesMap.put("roles",roles);

        write(resourceFW,new StringSubstitutor(valuesMap).replace(resource));
    }

    // writes client as payara client
    private void writePayaraModule() {
        ClientResource clientResource = realmResource.clients().get(targetClientID);
        String module = """
                module "payara_client" {
                	source = "../../../../modules/payara-client"
                	claim_name  = "${claimName}"
                	client_id   = "${clientID}"
                	base_url    = "${baseURL}"
                	description = "${description}"
                	valid_redirect_uris = ${validRedirectURIS}
                	roles = ${roles}
                }
                """;

        Map valuesMap = new HashMap();
        valuesMap.putAll(getClientValues(clientResource.toRepresentation()));
        valuesMap.put("claimName",(clientName.toLowerCase() + "_role"));
        valuesMap.put("roles",writePayaraModuleRoles(clientResource));

        write(resourceFW,new StringSubstitutor(valuesMap).replace(module));
    }

    // helper for writePayaraModule. writes the roles in the payara module
    private String writePayaraModuleRoles(ClientResource cr) {
        String roles = "{\t\n";
        for (RoleRepresentation roleRepresentation : cr.roles().list()) {
            Map roleValue = getRoleValues(roleRepresentation);// todo: memoization?
            String role = """
                            "${name}" = {
                                "name" = ${name}
                                "description" = "${description}" 
                            },
                    """;
            roles  += new StringSubstitutor(roleValue).replace(role);
        }
        return roles +"\t}";
    }


    // write and import a normal client
    private void writeClientResource() {
        ClientRepresentation clientRepresentation = realmResource.clients().get(targetClientID).toRepresentation();

        String resource = """
                resource "keycloak_openid_client" "CLIENT" {
                    access_token_lifespan = "${accessTokenLifeSpan}"
                    access_type = "${accessType}"
                    admin_url   = "${adminURL}"
                    backchannel_logout_session_required = false
                    base_url    = "${baseURL}"
                    client_authenticator_type = "client-secret"
                    client_id   = "${clientID}"
                    consent_required = false
                    description = "${description}"
                    direct_access_grants_enabled = false
                    enabled = true
                    frontchannel_logout_enabled = false
                    full_scope_allowed          = false
                    implicit_flow_enabled       = false
                    name = "${clientName}"
                    realm_id = "${realmName}"
                    service_accounts_enabled =${serviceAccountsEnabled}
                    standard_flow_enabled = ${standardFlowEnabled}
                    use_refresh_tokens = ${useRefreshToken}
                    valid_redirect_uris = ${validRedirectURIS}
                    web_origins = ${webOrigins}
                }
                """;
        write(resourceFW,new StringSubstitutor(getClientValues(clientRepresentation)).replace(resource));
    }

    // write and import all mappers. if payara, ignore the usermodel.
    //todo: do other types of resources
    private void writeAllMapperResources() {

        writeAudienceMapperResources();
        writeUserAttributeMapperResources();
        if(!isPayaraClient){
            writeUserModelClientRoleMapperResources();
        }
        writeUserSessionNoteMapperResources();
    }


    /**writes the audience mapper resource**/
    private void writeAudienceMapperResources() {
        for(ProtocolMapperRepresentation pmr : realmResource.clients().get(targetClientID).toRepresentation().getProtocolMappers()) {
            if(pmr.getProtocolMapper().equalsIgnoreCase("oidc-audience-mapper")){
                String resource = """
                            resource "keycloak_openid_audience_protocol_mapper" "${name-hyphenated}" {
                                add_to_id_token = ${addToIdToken}
                                client_id = ${currentModule}.id
                                included_client_audience = ${includedClientAudience}
                                name = "${name}"
                                realm_id = ${currentModule}.realm_id
                            }
                            """;

                write(resourceFW,new StringSubstitutor(getMapperValues(pmr)).replace(resource));
            }
        }
    }

    /**writes the user attribute mapper resources**/
    private void writeUserAttributeMapperResources() {
        for(ProtocolMapperRepresentation pmr : realmResource.clients().get(targetClientID).toRepresentation().getProtocolMappers()) {
            if(pmr.getProtocolMapper().equalsIgnoreCase("oidc-usermodel-attribute-mapper")){

                String resource = """
                            resource "keycloak_openid_user_attribute_protocol_mapper" "${name-hyphenated}" {
                                add_to_id_token = ${addToIdToken}
                                claim_name = "${claimName}"
                                client_id = ${currentModule}.id
                                name = "${name}"
                                user_attribute = "${userAttribute}"
                                realm_id = ${currentModule}.realm_id
                            }
                            """;

                write(resourceFW,new StringSubstitutor(getMapperValues(pmr)).replace(resource));
            }
        }
    }
    //todo: do this
    /**writes the user model client role mappers**/
    private void writeUserModelClientRoleMapperResources() {
        try{
            for(ProtocolMapperRepresentation pmr : realmResource.clients().get(targetClientID).toRepresentation().getProtocolMappers()) {
                if(pmr.getProtocolMapper().equalsIgnoreCase("oidc-usermodel-client-role-mapper")){

                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    /** writes the user session note mappers**/
    private void writeUserSessionNoteMapperResources() {
        for(ProtocolMapperRepresentation pmr : realmResource.clients().get(targetClientID).toRepresentation().getProtocolMappers()) {
            if(pmr.getProtocolMapper().equalsIgnoreCase("oidc-usersessionmodel-note-mapper")){

                String resource = """
                        resource "keycloak_openid_user_session_note_protocol_mapper" "${name-hyphenated}" {
                            add_to_id_token = ${addToIdToken}
                            claim_name = "${claimName}"
                            claim_value_type = "${claimValueType}"
                            client_id = ${currentModule}.id
                            name = "${name}"
                            realm_id = ${currentModule}.realm_id
                            session_note = "${sessionNote}"
                        }
                        """;

                write(resourceFW, new StringSubstitutor(getMapperValues(pmr)).replace(resource));
            }
        }
    }


    /** writes both realm and client level scope mapping resources **/
    private void writeScopeMappingResources(){
            String module = """
                    module "scope-mappings" {
                        source = "../../../../modules/scope-mappings"
                        realm_id = ${currentModule}.CLIENT.realm_id
                        client_id = ${currentModule}.CLIENT.id
                        roles = ${roles}
                    }
                    """;
            Map values = new HashMap<>();
            values.put("roles", writeScopeMappingRoles());
            values.putAll(globals);
            write(resourceFW,new StringSubstitutor(values).replace(module));
    }

    /** helper for writeScopeMappingResources. gets the roles**/
    private String writeScopeMappingRoles() {
        String roles = "{";
        RoleMappingResource roleMappingResource = realmResource.clients().get(targetClientID).getScopeMappings();
        Map <String, ClientMappingsRepresentation> map = roleMappingResource.getAll().getClientMappings();

        if(!(map == null || map.isEmpty())){
            for (String client : roleMappingResource.getAll().getClientMappings().keySet()) {
                for(RoleRepresentation rr: map.get(client).getMappings()) {
                    if(client.equalsIgnoreCase(clientName)){
                        roles += String.format("\n\t\t\"$%s/${name}\" = %s.CLIENT.ROLES[\"${name}\"].id,",
                                client,currentModule);
                    }else {
                        requiredClients.add(client);
                        roles += String.format("\n\t\t\"%s/${name}\" = var.%s.ROLES[\"${name}\"].id,",
                                client,client);
                    }
                    roles = new StringSubstitutor(getRoleValues(rr)).replace(roles);
                }
            }
        }
        //todo: this hasn't been tested yet
        List <RoleRepresentation> realmList = roleMappingResource.getAll().getRealmMappings();
        if(!(realmList == null || realmList.isEmpty())){
            for (RoleRepresentation rr : realmList) {
                roles += "\n\t\t\"realm/"+rr.getName()+"\" = \""+rr.getId()+"\","; // todo: change this into a terraform env
            }
        }
        return roles;
    }


    /**writes the client-roles module**/
    private void writeAllRoles() {
            ClientResource clientResource = realmResource.clients().get(targetClientID);
            ClientRepresentation clientRepresentation = realmResource.clients().get(targetClientID).toRepresentation();

            write(resourceFW,"module \"client-roles\" {\n");

            write(resourceFW,"\tsource = \"../../../../modules/client-roles\"\n");
            write(resourceFW,"\tclient_id = " + currentModule + ".CLIENT.id\n");
            write(resourceFW,"\trealm_id = " + currentModule + ".CLIENT.realm_id\n");
            write(resourceFW,"\troles = {\n");
            for (RoleRepresentation roleRepresentation : clientResource.roles().list()) {
                write(resourceFW,"\t\t\"" + roleRepresentation.getName() + "\" = {\n");
                write(resourceFW,"\t\t\t\"name\"        = \"" + roleRepresentation.getName() + "\"\n");
                String roleDescription = (roleRepresentation.getDescription() == null) ? "" : roleRepresentation.getDescription();
                write(resourceFW,"\t\t\t\"description\" = \"" + roleDescription + "\"\n");

                write(resourceFW,"\t\t\t\"composite_roles\" = [\n");
                for(RoleRepresentation rr : clientResource.roles().get(roleRepresentation.getName()).getRoleComposites()){

                    write(resourceFW,"\t\t\t\t\""+rr.getId()+"\",\n");
                };
                write(resourceFW,"\t\t\t]\n");
                write(resourceFW,"\t\t},\n");
            }
            write(resourceFW,"\t}\n");
            write(resourceFW,"}\n");
            importAllRoles(".module.client-roles");

            String value =(isPayaraClient)? "payara-client":"client-roles";
            write(resourceFW,"output \"ROLES\" {\n" +
                    "  value = module."+ value+".ROLES\n" +
                    "}\n");
    }

    /**writes the service-account-roles module**/
    private void writeServiceAccountRoles() {
            ClientResource clientResource = realmResource.clients().get(targetClientID);
            if (clientResource.toRepresentation().isServiceAccountsEnabled()) {
                String id = clientResource.getServiceAccountUser().getId();
                MappingsRepresentation mappingsRepresentation = realmResource.users().get(id).roles().getAll();

                String module = """                   
                        module "service-account-roles" {
                        	source = "../../../../modules/service-account-roles"
                        	realm_id = ${currentModule}.CLIENT.realm_id
                        	client_id = ${currentModule}.CLIENT.id
                        	service_account_user_id = ${currentModule}.CLIENT.service_account_user_id
                        	realm_roles = ${realm_roles}
                        	client_roles = ${client_roles}
                        }
                        """;
                Map values = new HashMap();
                values.put("currentModule",currentModule);
                values.put("realm_roles",writeServiceAccountRealmRoles(mappingsRepresentation));
                values.put("client_roles",writeServiceAccountClientRoles(mappingsRepresentation));

                write(resourceFW, new StringSubstitutor(values).replace(module));

            }
    }

    /** helper for writing SA roles**/
    private String writeServiceAccountRealmRoles(MappingsRepresentation mr) {
        String roles = "{\n";
        for(RoleRepresentation rr: mr.getRealmMappings()){
            roles += "\t\t\"" + rr.getName() + "\" = \"" + rr.getName() + "\",\n";
        }
        return roles + "\t}";
    }

    /** helper for writing SA roles**/
    private String writeServiceAccountClientRoles(MappingsRepresentation mr) {
        String roles = "{\n";
        for(String client : mr.getClientMappings().keySet()){
            String roleClientPath =currentModule;
            if(client.equalsIgnoreCase(clientName)){
                requiredClients.add(client);
                roleClientPath = "var." + client;
            }
            for(RoleRepresentation rr: mr.getClientMappings().get(client).getMappings()) {
                String role = """
                                "${roleClientName}/${name}" = {
                                    "client_id" = ${roleClientPath}.CLIENT.id,
                                    "role_id" = "${name}"
                                }
                        """;
                Map values = getRoleValues(rr);
                values.put("roleClientName",client);
                values.put("roleClientPath",roleClientPath);

                roles += new StringSubstitutor(values).replace(role);
            }
        }
        return  roles + "\t}";
    }


// IMPORT METHODS

    /** imports the client, mapper and role associated with a payara client module **/
    private void importPayaraClient(){
        String payaraModule =".module.payara_client";

        importClient(payaraModule);
        importAllRoles(payaraModule);
        importPayaraUserModelClientRoleMapper();
    }

    /** imports a client**/
    private void importClient(String extraModule){
        System.out.println(clientName +" import is being written");
        String resourcePath =terraformImport;
        resourcePath += "module." + environmentModule + ".module."+ realmName+ ".module." + clientName+ extraModule + ".keycloak_openid_client.CLIENT ";
        resourcePath += realmName+"/"+targetClientID+"\n";
        write(importFW,resourcePath);
    }
    /** imports all the mappers associated with the client**/
    private void importAllMapperResources() {
        if (realmResource.clients().get(targetClientID).toRepresentation().getProtocolMappers() == null) return;
        for (ProtocolMapperRepresentation pmr : realmResource.clients().get(targetClientID).toRepresentation().getProtocolMappers()) {
            String mapperType;
            String protocolMapper = pmr.getProtocolMapper();
            switch (protocolMapper){
                case "the one that payara style uses":
                    if(isPayaraClient) continue;
                    mapperType = "the one that the payara style uses";
                    break;
                case "oidc-audience-mapper":
                    mapperType = "keycloak_openid_audience_protocol_mapper";
                    break;
                case "oidc-usermodel-attribute-mapper":
                    mapperType = "keycloak_openid_user_attribute_protocol_mapper";
                    break;
                case "oidc-usersessionmodel-note-mapper":
                    mapperType = "keycloak_openid_user_session_note_protocol_mapper";
                    break;
                default:
                    continue;
            }

            String importStatement = "terraform import module.${environmentModule}.module.${realmName}.module.${clientName}.${mapperType}.${name-hyphenated} ";
            importStatement += "${realmName}/client/${targetClientID}/${id}\n";
            Map values = getMapperValues(pmr);
            values.put("mapperType", mapperType);

            write(importFW, new StringSubstitutor(values).replace(importStatement));
        }
    }


    /** imports a specific mapper for the payara client**/
    // todo: we have an import for all mappers. the importAll should call this?
    private void importPayaraUserModelClientRoleMapper() {
        for(ProtocolMapperRepresentation pmr : realmResource.clients().get(targetClientID).toRepresentation().getProtocolMappers()) {
            // it's safe to assume that there's only 1 client role mapper in each payara client.
            if(pmr.getProtocolMapper().equalsIgnoreCase("oidc-usermodel-client-role-mapper")){
                System.out.println(pmr.getName() +" import is being written");
                String resourcePath =terraformImport;
                resourcePath += "module." + environmentModule + ".module."+ realmName+ ".module." + clientName +currentModule+ ".keycloak_openid_user_client_role_protocol_mapper.client_role_mapper ";
                resourcePath += realmName+"/client/"+targetClientID+"/" + pmr.getId()+"\n";
                write(importFW,resourcePath);
            }
        }
    }


    /** imports the scope mapping roles (both client and realm level)**/
    private void importScopeMappingResources() {
        RoleMappingResource roleMappingResource = realmResource.clients().get(targetClientID).getScopeMappings();
        Map <String, ClientMappingsRepresentation> map = roleMappingResource.getAll().getClientMappings();

        if(!(map == null || map.isEmpty())){
            for (String client : roleMappingResource.getAll().getClientMappings().keySet()) {
                String roleClientID = realmResource.clients().findByClientId(client).get(0).getId();
                for(RoleRepresentation rr: map.get(client).getMappings()) {
                    String resourcePath =terraformImport;
                    resourcePath += "module." + environmentModule + ".module."+ realmName+ ".module." + clientName + ".module.scope-mappings.keycloak_generic_client_role_mapper.SCOPE-MAPPING[\\\""+client+"/"+rr.getName()+"\\\"] ";
                    resourcePath += realmName+"/client/"+targetClientID+"/scope-mappings/" + roleClientID+"/"+ rr.getId()+"\n";
                    write(importFW,resourcePath);
                }
            }
        }
        //todo: this hasn't been tested yet
        List <RoleRepresentation> realmList = roleMappingResource.getAll().getRealmMappings();
        if(!(realmList == null || realmList.isEmpty())){
            for (RoleRepresentation rr : realmList) {
                String resourcePath =terraformImport;
                resourcePath += "module." + environmentModule + ".module."+ realmName+ ".module." + clientName + ".module.scope-mappings.keycloak_generic_client_role_mapper.SCOPE-MAPPING[\\\"realm/"+ rr.getName()+"\\\"] ";
                resourcePath += realmName+"/client/"+targetClientID+"/scope-mappings/" + realmResource.toRepresentation().getId()+"/"+ rr.getId()+"\n";
                write(importFW,resourcePath);
            }
        }
    }

    /**
     * writes the import statements to an output file
     * defaults path is:
     *          module.ENVIRONMENT.module.REALM.module.CLIENT.keycloak_role.ROLES[ROLE_ID]
     * extraModule: if resource is located somewhere else:
     *          module.ENVIRONMENT.module.REALM.module.CLIENT.extraModule.keycloak_role.ROLES[ROLE_ID]
     */
    private void importAllRoles(String extraModule) {
            String clientID = targetClientID;
            RolesResource rolesResource = realmResource.clients().get(clientID).roles();
            for(RoleRepresentation roleRepresentation: rolesResource.list() ){
                    String roleID = roleRepresentation.getId();
                    String resourcePath =terraformImport +  "module." + environmentModule + ".module."+ realmName+ ".module." + clientName +extraModule+ ".keycloak_role.ROLES[\\\"" + roleRepresentation.getName() + "\\\"] " + realmResource.toRepresentation().getId() + "/" + roleID + "\n";
                    write(importFW,resourcePath);
            }
    }

    private void ImportServiceAccountRoles() {
        ClientResource clientResource = realmResource.clients().get(targetClientID);
        if (clientResource.toRepresentation().isServiceAccountsEnabled()) {
            String id = clientResource.getServiceAccountUser().getId();
            MappingsRepresentation mappingsRepresentation = realmResource.users().get(id).roles().getAll();

            for(RoleRepresentation rr: mappingsRepresentation.getRealmMappings()){
                String resourcePath =terraformImport +  "module." + environmentModule + ".module."+ realmName+ ".module." + clientName + ".module.service-account-roles.keycloak_openid_client_service_account_realm_role.ROLE[\\\"" + rr.getName() + "\\\"] ";
                resourcePath += realmName + "/" + id + "/" + rr.getId()+"\n";
                write(importFW,resourcePath);
            }

            for(String client : mappingsRepresentation.getClientMappings().keySet()){
                ClientMappingsRepresentation cmr= mappingsRepresentation.getClientMappings().get(client);
                String roleClientName = cmr.getClient();
                String roleClientID = cmr.getId();
                for(RoleRepresentation rr: cmr.getMappings()) {
                    String resourcePath =terraformImport +  "module." + environmentModule + ".module."+ realmName+ ".module." + clientName + ".module.service-account-roles.keycloak_openid_client_service_account_role.ROLE[\\\"" +roleClientName+"/"+rr.getName() + "\\\"] ";
                    resourcePath += realmName + "/" + id + "/" +roleClientID+"/" + rr.getId()+"\n";
                    write(importFW,resourcePath);
                }
            }
        }
    }


    //    GET MAP METHODS

    /** gets values from a client**/
    private Map getClientValues(ClientRepresentation cr){
        Map result = new HashMap<>();

        result.put("accessTokenLifeSpan",(cr.getAttributes().get("access.token.lifespan") == null)? "": cr.getAttributes().get("access.token.lifespan"));
        result.put("accessType",(cr.isPublicClient())? "PUBLIC": ((cr.isBearerOnly())? "BEARER-ONLY":"CONFIDENTIAL"));
        result.put("adminURL",(cr.getAdminUrl() == null)? "": cr.getAdminUrl());
        result.put("baseURL",(cr.getBaseUrl() == null)? "": cr.getBaseUrl());
        result.put("clientID",(cr.getClientId() == null)? "": cr.getClientId());
        result.put("clientName",(cr.getName() == null)? "": cr.getName());
        result.put("description",(cr.getDescription() == null)? "":cr.getDescription());
        result.put("realmName",realmName);
        result.put("serviceAccountsEnabled",cr.isServiceAccountsEnabled().toString());
        result.put("standardFlowEnabled", cr.isStandardFlowEnabled().toString());
        //todo: check if this should be default to true or false
        result.put("useRefreshToken",(cr.getAttributes().get("use.refresh.tokens") ==null)? "false":cr.getAttributes().get("use.refresh.tokens"));

        String validRedirectURIS = "\t[\n";
        for (String uri : cr.getRedirectUris()) {
            validRedirectURIS += String.format("\t\t\"%s\",\n",uri);
        }
        result.put("validRedirectURIS",validRedirectURIS +"\t]");

        String webOrigins = "[\n";
        for(String uri : cr.getWebOrigins()) {
            webOrigins += String.format("\t\t\"%s\",\n",uri);
        }
        result.put("webOrigins",webOrigins + "\t]");

        return result;
    }

    // get values from a role
//     todo: this is kinda useless right now... either expand on this or delete this
    private Map getRoleValues(RoleRepresentation rr){
        Map result = new HashMap<>();
        result.put("name",rr.getName());
        result.put("id",rr.getId());
        result.put("description",(rr.getDescription() == null)? "":rr.getDescription());

        return result;
    }

    //gets values the mapper needs. some configs in a mapper aren't present in another mapper, but it's ok
    private Map getMapperValues(ProtocolMapperRepresentation pmr) {
        Map values = new HashMap();
        values.put("name-hyphenated",pmr.getName().replaceAll(" ","-"));
        values.put("name",pmr.getName());
        values.put("addToIdToken",pmr.getConfig().get("id.token.claim"));
        values.put("claimName",pmr.getConfig().get("claim.name"));
        values.put("claimValueType",pmr.getConfig().get("jsonType.label"));
        values.put("id",pmr.getId());
        values.put("includedClientAudience",pmr.getConfig().get("included.client.audience"));
        values.put("sessionNote",pmr.getConfig().get("user.session.note"));
        values.put("userAttribute",pmr.getConfig().get("user.attribute"));

        values.putAll(globals);

        return  values;
    }





    // FILE METHODS
    public void closeFile() throws IOException{
        resourceFW.close();
        importFW.close();
    }

    private void write(FileWriter filewriter, String input){
        try{
            filewriter.write(input);
        }catch (Exception e){
            throw new RuntimeException();
        }
    }



    /**
     * gets the Client's UUID from a clientID
     *
     * @param clientID
     * @return String representation of the clientUUID
     * @throws Exception when multiple clients or no clients found
     */
    //todo: keep. might be useful if we want to import multiple clients at once
    private String getClientUUID(String clientID) throws Exception {
        ClientsResource clientsResource = realmResource.clients();
        List<ClientRepresentation> clientRepresentationList = clientsResource.findByClientId(clientID);
        if (clientRepresentationList.size() > 1) {
            throw new Exception("more than 1 client found");
        } else if (clientRepresentationList.size() < 1) {
            throw new Exception("client not found");
        } else {
            String clientUUID = clientRepresentationList.get(0).getId();
            return clientUUID;
        }
    }
}

