import org.apache.commons.text.StringSubstitutor;
import org.checkerframework.checker.regex.qual.Regex;
import org.keycloak.admin.client.resource.*;
import org.keycloak.representations.idm.*;

import java.io.*;
import java.util.*;
//split into classes: writing, importing, deleting

public class ClientService {
    // todo: idk if enum is proper for this
    enum ClientType {
        BASIC,
        PAYARA,
        DATA_SOURCE
    }
    private RealmResource realmResource;

    // todo: variables and outputs depend on: type of client, clients that it requires.
    // we get the required clients from writing mainFW


    //todo: why is this not being used?
//    private String environment;
//    private String realmName;
    private String clientID;
    private String clientUUID;
    private ClientType  clientType;

    //todo: fix the Maps
    // note: since global is just a map of <String,String>, it's ok to make a shallow copy
    // AS LONG AS YOU DON'T DO THIS: Map localMap = globals
    private Map globals;
    private Set<String> requiredClients;

    private FileWriter mainFW;
    private FileWriter outputsFW;
    private FileWriter variablesFW;
    private FileWriter versionsFW;

    //string constants
    private String terraformImport;
    private String clientResourceModule; //todo: this works for client resources only. fix it
    //todo: add this into a map? make it include data types.


    /**
     * sets up keycloak. Also sets up the RealmResource
     //     * @throws IOException
     */
    public ClientService(RealmResource rr,String clientID,String envName,String clientType, String path) {
        this.realmResource = rr;

        this.clientID = clientID;
        try{
            this.clientUUID = getClientUUID(clientID,rr);
        }catch (Exception e) {
            e.printStackTrace();
        }
        switch (clientType){
            case "payara" -> this.clientType = ClientType.PAYARA;
            case "basic" -> this.clientType = ClientType.BASIC;
            case "data source"-> this.clientType = ClientType.DATA_SOURCE;
            default -> throw new RuntimeException("invalid client type");
        }
        //todo: why are you doing this again?
        switch (this.clientType){
            case BASIC -> clientResourceModule = "keycloak_openid_client.CLIENT";
            case PAYARA -> clientResourceModule = "module.payara-client.CLIENT";
            case DATA_SOURCE -> clientResourceModule = "data.keycloak_openid_client.CLIENT";
        }

        //todo: makes sense if you want to split this into another class
        globals = new HashMap<>();
        globals.put("environment", envName);
        globals.put("realmName",rr.toRepresentation().getId());
        globals.put("clientID", clientID);
        globals.put("clientResourceModule", clientResourceModule);
        globals.put("clientUUID",clientUUID);

        requiredClients = new HashSet<>();

        this.terraformImport = "terraform import module." + envName + ".module."+rr.toRepresentation().getId() +".module." + clientID;


        configureFileWriters(path);
    }


    // initializes the file writers.
    private void configureFileWriters(String path) {
        String folderPath = path + clientID.toLowerCase(Locale.ROOT);
        System.out.println("\t" + folderPath);
        if(!(new File(folderPath).mkdir()) && !(new File(folderPath).exists())) {
            throw new RuntimeException("Folder can't be created and doesn't exists");
        }
        try{
             mainFW = createFileWriter(folderPath + "\\main.tf");
             outputsFW = createFileWriter(folderPath + "\\outputs.tf");
             variablesFW = createFileWriter(folderPath + "\\variables.tf");
             versionsFW = createFileWriter(folderPath + "\\versions.tf");
         } catch (IOException e){
             throw new RuntimeException();
         }
    }
    // creates a single file writer. helper for configureFileWriters
    private FileWriter createFileWriter(String path) throws IOException {
        System.out.println("\t\t" + path);
        if (!(new File(path).createNewFile()) && !(new File(path).exists())) {
            throw new RuntimeException("File can't be created");
        }
        return new FileWriter(path);
    }

    public String createAllNonDependentResources() {
        writeVersionsFile();
        if(clientType == ClientType.DATA_SOURCE) {
            writeClientDataSource();
            writeClientRolesDataSource();
        }else {
            if (clientType == ClientType.PAYARA) {
                writePayaraModule();
            } else if (clientType == ClientType.BASIC) {
                writeClientResource();
                writeAllRoles();
            }
            writeAllMapperResources();
        }
        writeVariables();
        writeOutputs();
        return writeModuleInRealm();
    }

    public String createAllNonDependentImports(){
        String result = "";
        if(clientType == ClientType.DATA_SOURCE) return result;

        if(clientType == ClientType.PAYARA) {
            result += importPayaraClient();
        } else if(clientType == ClientType.BASIC) {
            result +=  importClient("");
            result +=  importAllRoles(".module.client-roles");
        }

        result +=  importAllMapperResources();

        return result;
    }

    public String createAllDependentImports() {
        String result = "";
        if(clientType == ClientType.DATA_SOURCE) return result;

        result +=  importScopeMappingResources();
        result +=  ImportServiceAccountRoles();

        return result;
    }

    public String createResources(){
        writeVersionsFile();

        if(clientType == ClientType.DATA_SOURCE) {
            writeClientDataSource();
            writeClientRolesDataSource();
        }else{
            if(clientType == ClientType.PAYARA) {
                writePayaraModule();
            }else if(clientType == ClientType.BASIC){
                writeClientResource();
                writeAllRoles();
            }
            writeAllMapperResources();
            writeScopeMappingResources(); //todo: only this and writeServiceAccountRoles depend on other clients
            writeServiceAccountRoles();
        }
        writeVariables();
        writeOutputs();
        return writeModuleInRealm();
    }

    public String createImports(){
        String result = "";
        if(clientType == ClientType.DATA_SOURCE) return result;

        if(clientType == ClientType.PAYARA) {
            result += importPayaraClient();
        } else if(clientType == ClientType.BASIC) {
            result +=  importClient("");
            result +=  importAllRoles(".module.client-roles");
        }

        result +=  importAllMapperResources();
        result +=  importScopeMappingResources();
        result +=  ImportServiceAccountRoles();

        return result;
    }




// WRITE METHODS

    // "writes" the module "client_name" found on the realm tf file. it just prints it out in the console.
    private String writeModuleInRealm() {
        String module = """
            module "${clientName}" {
                source = "./${clientNameLowerCase}"
            ${clientDependencies}
            }
            """;

        Map variables = new HashMap();
        variables.put("clientName", clientID);
        variables.put("clientNameLowerCase", clientID.toLowerCase(Locale.ROOT));
        String clientDependencies = "";
        for (String clientID: requiredClients){
            clientDependencies += String.format("""
                            %s= "${module.%s}"
                        """,clientID,clientID);
        }
        variables.put("clientDependencies",clientDependencies);
        module = new StringSubstitutor(variables).replace(module);

        return removeBlankLine(module);
    }

//    writes output. todo: check if clientType fits will make the thing work with the 3 types.
    private void writeOutputs() {
        //todo: this is weird. change this.
        String moduleName = (clientType ==ClientType.PAYARA)? "payara-client": (clientType == ClientType.BASIC)? "client-roles" :"client-roles-data-source";
        String outputs = String.format("""
                        output "CLIENT" {
                            value = ${clientType}.CLIENT
                        }
                        output "ROLES" {
                            value = module.%s.ROLES
                        }
                        """,moduleName);

        Map values = new HashMap();
        values.put("clientType",(clientType == ClientType.PAYARA)? "module.payara-client": (clientType == ClientType.DATA_SOURCE)? "data.keycloak_openid_client":"keycloak_openid_client" );

        String variables = "";
        for (String client: requiredClients){
            variables +="variable \""+client+"\" {\n" + "}\n";
        }
        values.put("variables",variables);
        write(outputsFW,new  StringSubstitutor(values).replace(outputs));
    }

//    writes variables
    private void writeVariables() {
        for (String client: requiredClients){
            String variable = String.format("""
                    variable "%s" {}
                    """,client);
            write(variablesFW,(variable));
        }
    }



    // DATA SOURCE SPECIFIC FUNCTIONS:
    private void writeClientDataSource() {
        String dataSource = """
                data "keycloak_openid_client" "CLIENT" {
                    realm_id = "${realmName}"
                    client_id = "${clientID}"
                }
                """;
        write(mainFW,new StringSubstitutor(globals).replace(dataSource));
    }

    // writes the roles as a data source
    private void writeClientRolesDataSource() {
        ClientResource clientResource = realmResource.clients().get(clientUUID);
        String resource = """
                module "client-roles-data-source" {
                    source = "../../../../modules/client-roles-data-source"
                    client_id = data.keycloak_openid_client.CLIENT.id
                    realm_id = data.keycloak_openid_client.CLIENT.realm_id
                    roles = ${roles}
                }
                """;

        Map valuesMap = new HashMap(globals);
        String roles = "{\n";
        for (RoleRepresentation roleRepresentation : clientResource.roles().list()) {
            String roleName = roleRepresentation.getName();
            roles += String.format("\t\t\"%s\" = \"%s\",\n",roleName,roleName);
        }
        valuesMap.put("roles",roles +"\t}");

        write(mainFW,new StringSubstitutor(valuesMap).replace(resource));
    }

    private void writeOutputRole() {
        String value =(clientType == ClientType.PAYARA)? "payara-client":"client-roles";
        write(mainFW,String.format("""
                output "ROLES" {
                    value = module.%s.ROLES
                }
                """,value));
    }

    private void writeVersionsFile() {
        String keycloakProvider = """
                terraform {
                  required_providers {
                    keycloak = {
                      source  = "mrparkers/keycloak"
                      version = ">= 3.0.0"
                    }
                  }
                }
                """;
        write(versionsFW,keycloakProvider);
    }

    // writes client as payara client
    private void writePayaraModule() {
        ClientResource clientResource = realmResource.clients().get(clientUUID);
        String module = """
                module "payara-client" {
                	source = "../../../../modules/payara-client"
                	claim_name  = "${claimName}"
                	client_id   = "${clientID}"
                	base_url    = "${baseURL}"
                	description = "${description}"
                	valid_redirect_uris = ${validRedirectURIS}
                	roles = ${roles}
                }
                """;

        Map valuesMap = (getClientValues(clientResource.toRepresentation()));
        valuesMap.put("claimName",(clientID.toLowerCase() + "_role"));
        valuesMap.put("roles",writePayaraModuleRoles(clientResource));

        write(mainFW,new StringSubstitutor(valuesMap).replace(module));
    }

    // helper for writePayaraModule. writes the roles in the payara module
    private String writePayaraModuleRoles(ClientResource cr) {
        String roles = "{\t\n";
        for (RoleRepresentation roleRepresentation : cr.roles().list()) {
            String role = """
                            "${name}" = {
                                "name" = "${name}"
                                "description" = "${description}" 
                            },
                    """;
            roles  += new StringSubstitutor(getRoleValues(roleRepresentation)).replace(role);
        }
        return roles +"\t}";
    }


    // write and import a normal client
    private void writeClientResource() {
        ClientRepresentation clientRepresentation = realmResource.clients().get(clientUUID).toRepresentation();

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
        write(mainFW,new StringSubstitutor(getClientValues(clientRepresentation)).replace(resource));
    }

    // write and import all mappers. if payara, ignore the usermodel.
    //todo: do other types of resources
    private void writeAllMapperResources() {
        if(realmResource.clients().get(clientUUID).toRepresentation().getProtocolMappers() == null) return;
        writeAudienceMapperResources();
        writeUserAttributeMapperResources();
        if(clientType != ClientType.PAYARA){
            writeUserModelClientRoleMapperResources();
        }
        writeUserSessionNoteMapperResources();
    }


    /**writes the audience mapper resource**/
    private void writeAudienceMapperResources() {
        for(ProtocolMapperRepresentation pmr : realmResource.clients().get(clientUUID).toRepresentation().getProtocolMappers()) {
            if(pmr.getProtocolMapper().equalsIgnoreCase("oidc-audience-mapper")){
                String resource = """
                            resource "keycloak_openid_audience_protocol_mapper" "${name-hyphenated}" {
                                add_to_id_token = ${addToIdToken}
                                client_id = ${clientResourceModule}.id
                                included_client_audience = "${includedClientAudience}"
                                name = "${name}"
                                realm_id = ${clientResourceModule}.realm_id
                            }
                            """;
                write(mainFW,new StringSubstitutor(getMapperValues(pmr)).replace(resource));
            }
        }
    }

    /**writes the user attribute mapper resources**/
    private void writeUserAttributeMapperResources() {
        for(ProtocolMapperRepresentation pmr : realmResource.clients().get(clientUUID).toRepresentation().getProtocolMappers()) {
            if(pmr.getProtocolMapper().equalsIgnoreCase("oidc-usermodel-attribute-mapper")){
                String resource = """
                            resource "keycloak_openid_user_attribute_protocol_mapper" "${name-hyphenated}" {
                                add_to_id_token = ${addToIdToken}
                                claim_name = "${claimName}"
                                client_id = ${clientResourceModule}.id
                                name = "${name}"
                                user_attribute = "${userAttribute}"
                                realm_id = ${clientResourceModule}.realm_id
                            }
                            """;
                write(mainFW,new StringSubstitutor(getMapperValues(pmr)).replace(resource));
            }
        }
    }

    //todo: do this
    /**writes the user model client role mappers**/
    private void writeUserModelClientRoleMapperResources() {
        try{
            for(ProtocolMapperRepresentation pmr : realmResource.clients().get(clientUUID).toRepresentation().getProtocolMappers()) {
                if(pmr.getProtocolMapper().equalsIgnoreCase("oidc-usermodel-client-role-mapper")){
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    /** writes the user session note mappers**/
    private void writeUserSessionNoteMapperResources() {
        for(ProtocolMapperRepresentation pmr : realmResource.clients().get(clientUUID).toRepresentation().getProtocolMappers()) {
            if(pmr.getProtocolMapper().equalsIgnoreCase("oidc-usersessionmodel-note-mapper")){
                String resource = """
                        resource "keycloak_openid_user_session_note_protocol_mapper" "${name-hyphenated}" {
                            add_to_id_token = ${addToIdToken}
                            claim_name = "${claimName}"
                            claim_value_type = "${claimValueType}"
                            client_id = ${clientResourceModule}.id
                            name = "${name}"
                            realm_id = ${clientResourceModule}.realm_id
                            session_note = "${sessionNote}"
                        }
                        """;
                write(mainFW, new StringSubstitutor(getMapperValues(pmr)).replace(resource));
            }
        }
    }


    /** writes both realm and client level scope mapping resources **/
    private void writeScopeMappingResources(){
            String module = """
                    module "scope-mappings" {
                        source = "../../../../modules/scope-mappings"
                        realm_id = ${clientResourceModule}.realm_id
                        client_id = ${clientResourceModule}.id
                        roles = ${roles}
                    }
                    """;
            Map values = new HashMap<>(globals);
            try{
                values.put("roles", writeScopeMappingRoles());
            } catch (Exception e){
                System.out.println("\t\t\tno scope mappings found");
                return;
            }
            write(mainFW,new StringSubstitutor(values).replace(module));
    }

    /** helper for writeScopeMappingResources. gets the roles**/
    private String writeScopeMappingRoles() throws Exception{
        String roles = "{";
        RoleMappingResource roleMappingResource = realmResource.clients().get(clientUUID).getScopeMappings();
        Map <String, ClientMappingsRepresentation> map = roleMappingResource.getAll().getClientMappings();
        List <RoleRepresentation> realmList = roleMappingResource.getAll().getRealmMappings();

        if(map == null && realmList == null) throw new Exception();

        if(!(map == null || map.isEmpty())){
            for (String client : roleMappingResource.getAll().getClientMappings().keySet()) {
                for(RoleRepresentation rr: map.get(client).getMappings()) {
                    if(client.equalsIgnoreCase(clientID)){
                        roles += String.format("\n\t\t\"$%s/${name}\" = %s.ROLES[\"${name}\"].id,",
                                client, clientResourceModule);
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
        if(!(realmList == null || realmList.isEmpty())){
            for (RoleRepresentation rr : realmList) {
                roles += "\n\t\t\"realm/"+rr.getName()+"\" = \""+rr.getId()+"\","; // todo: change this into a terraform env
            }
        }
        return roles + "\n\t}";
    }

    /**writes the client-roles module**/
    private void writeAllRoles() {
            String module = """
                    module "client-roles" {
                        source = "../../../../modules/client-roles"
                        client_id = ${clientResourceModule}.id
                        realm_id = ${clientResourceModule}.realm_id
                        roles = ${roles}
                    }
                    """;

            String roles = "{\n";
            for (RoleRepresentation rr : realmResource.clients().get(clientUUID).roles().list()) {
                roles += new StringSubstitutor(getRoleValues(rr)).replace("""
                                "${name}" = {
                                    "name" = "${name}"
                                    "description" = "${description}"
                                },
                        """);
            }
            roles += "\t}";
            Map values = new HashMap<>(globals);
            values.put("roles",roles);

            write(mainFW,new StringSubstitutor(values).replace(module));
            importAllRoles(".module.client-roles");
    }

    /**writes the service-account-roles module**/
    private void writeServiceAccountRoles() {
            ClientResource clientResource = realmResource.clients().get(clientUUID);
            if (clientResource.toRepresentation().isServiceAccountsEnabled()) {
                String id = clientResource.getServiceAccountUser().getId();
                MappingsRepresentation mappingsRepresentation = realmResource.users().get(id).roles().getAll();

                String module = """                   
                        module "service-account-roles" {
                        	source = "../../../../modules/service-account-roles"
                        	realm_id = ${clientResourceModule}.realm_id
                        	client_id = ${clientResourceModule}.id
                        	service_account_user_id = ${clientResourceModule}.service_account_user_id
                        	realm_roles = ${realm_roles}
                        	client_roles = ${client_roles}
                        }
                        """;
                Map values = new HashMap();
                values.put("clientResourceModule", clientResourceModule);
                values.put("realm_roles",writeServiceAccountRealmRoles(mappingsRepresentation));
                values.put("client_roles",writeServiceAccountClientRoles(mappingsRepresentation));

                write(mainFW, new StringSubstitutor(values).replace(module));

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
            String roleClientPath = clientResourceModule;
            if(!client.equalsIgnoreCase(clientID)){
                requiredClients.add(client);
                roleClientPath = "var." + client +".CLIENT";
            }
            for(RoleRepresentation rr: mr.getClientMappings().get(client).getMappings()) {
                String role = """
                                "${roleClientName}/${name}" = {
                                    "client_id" = ${roleClientPath}.id,
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
    private String importPayaraClient(){
        String result = "";
        String payaraModule =".module.payara-client";

        result += importClient(payaraModule);
        result += importAllRoles(payaraModule);
        result += importPayaraUserModelClientRoleMapper();

        return result;
    }

    /** imports a client**/
    //todo: fix this. why does still need an extra module? should be taken care of somewhere else
    // it has an extra module because both payara and basic clients use this.
    private String importClient(String extraModule){
        String importStr =terraformImport + extraModule + ".keycloak_openid_client.CLIENT ${realmName}/${clientUUID}\n";
        return new StringSubstitutor(globals).replace(importStr);
    }
    /** imports all the mappers associated with the client**/
    private String importAllMapperResources() {
        String result = "";
        if (realmResource.clients().get(clientUUID).toRepresentation().getProtocolMappers() == null) return "";
        for (ProtocolMapperRepresentation pmr : realmResource.clients().get(clientUUID).toRepresentation().getProtocolMappers()) {
            String mapperType;
            String protocolMapper = pmr.getProtocolMapper();
            switch (protocolMapper){
                case "the one that payara style uses":
                    if(clientType == ClientType.PAYARA) continue;
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

            String importStatement = terraformImport + ".${mapperType}.${name-hyphenated} ";
            importStatement += "${realmName}/client/${clientUUID}/${id}\n";
            Map values = getMapperValues(pmr);
            values.put("mapperType", mapperType);

            result += new StringSubstitutor(values).replace(importStatement);
        }
        return result;
    }


    /** imports a specific mapper for the payara client**/
    // todo: we have an import for all mappers. the importAll should call this? No. it's not technically "part" of the mapper,
//     and also there's too many differences with the generic mapper.
    private String importPayaraUserModelClientRoleMapper() {
        String result = "";
        for(ProtocolMapperRepresentation pmr : realmResource.clients().get(clientUUID).toRepresentation().getProtocolMappers()) {
            if(pmr.getProtocolMapper().equalsIgnoreCase("oidc-usermodel-client-role-mapper")){
                String importStatement =terraformImport + ".module.payara-client.keycloak_openid_user_client_role_protocol_mapper.client_role_mapper ";
                importStatement += "${realmName}/client/${clientUUID}/${id}\n";
                result += new StringSubstitutor(getMapperValues(pmr)).replace(importStatement);
            }
        }
        return result;
    }


    /** imports the scope mapping roles (both client and realm level)**/
    private String importScopeMappingResources() {
        String result = "";
        RoleMappingResource roleMappingResource = realmResource.clients().get(clientUUID).getScopeMappings();
        Map <String, ClientMappingsRepresentation> map = roleMappingResource.getAll().getClientMappings();

        if(!(map == null || map.isEmpty())){
            for (String client : roleMappingResource.getAll().getClientMappings().keySet()) {
                String roleClientID = realmResource.clients().findByClientId(client).get(0).getId();
                for(RoleRepresentation rr: map.get(client).getMappings()) {
                    String resourcePath =terraformImport;
                    resourcePath += ".module.scope-mappings.keycloak_generic_client_role_mapper.SCOPE-MAPPING[\\\""+client+"/${name}\\\"] ";
                    resourcePath += "${realmName}/client/${clientUUID}/scope-mappings/"+roleClientID+"/${ID}\n";
                    result += new StringSubstitutor(getRoleValues(rr)).replace(resourcePath);
                }
            }
        }
        //todo: this hasn't been tested yet
        List <RoleRepresentation> realmList = roleMappingResource.getAll().getRealmMappings();
        if(!(realmList == null || realmList.isEmpty())){
            for (RoleRepresentation rr : realmList) {
                String resourcePath =terraformImport;
                resourcePath += ".module.scope-mappings.keycloak_generic_client_role_mapper.SCOPE-MAPPING[\\\"realm/${name}\\\"] ";
                resourcePath += "${realmName}/client/${clientUUID}/scope-mappings/${ID}\n";

                result += resourcePath;
            }
        }
        return result;
    }

    /**
     * writes the import statements to an output file
     * defaults path is:
     *          module.ENVIRONMENT.module.REALM.module.CLIENT.keycloak_role.ROLES[ROLE_ID]
     * extraModule: if resource is located somewhere else:
     *          module.ENVIRONMENT.module.REALM.module.CLIENT.extraModule.keycloak_role.ROLES[ROLE_ID]
     */
    private String importAllRoles(String extraModule) {
        String result = "";
        RolesResource rolesResource = realmResource.clients().get(clientUUID).roles();
        for(RoleRepresentation roleRepresentation: rolesResource.list() ){
            String resource = terraformImport + extraModule + """
                    .keycloak_role.ROLES[\\\"${name}\\\"] ${realmName}/${ID}
                    """;
            result += new StringSubstitutor(getRoleValues(roleRepresentation)).replace(resource);
        }
        return result;
    }

    private String ImportServiceAccountRoles() {
        String result = "";
        ClientResource clientResource = realmResource.clients().get(clientUUID);
        if (clientResource.toRepresentation().isServiceAccountsEnabled()) {
            String userID = clientResource.getServiceAccountUser().getId();
            MappingsRepresentation mappingsRepresentation = realmResource.users().get(userID).roles().getAll();

            for(RoleRepresentation rr: mappingsRepresentation.getRealmMappings()){
                String resourcePath =terraformImport + ".module.service-account-roles.keycloak_openid_client_service_account_realm_role.ROLE[\\\"" + rr.getName() + "\\\"] ";
                resourcePath += String.format("${realmName}/%s/${ID}\n",userID);
                result += new StringSubstitutor(getRoleValues(rr)).replace(resourcePath);
            }

            for(String client : mappingsRepresentation.getClientMappings().keySet()){
                ClientMappingsRepresentation cmr= mappingsRepresentation.getClientMappings().get(client);
                String roleClientName = cmr.getClient();
                String roleClientID = cmr.getId();
                for(RoleRepresentation rr: cmr.getMappings()) {
                    String resourcePath =terraformImport + ".module.service-account-roles.keycloak_openid_client_service_account_role.ROLE[\\\"" +roleClientName+"/"+rr.getName() + "\\\"] ";
                    resourcePath +=  String.format("${realmName}/%s/%s/${ID}\n",userID,roleClientID);
                    result += new StringSubstitutor(getRoleValues(rr)).replace(resourcePath);
                }
            }
        }
        return result;
    }


    //    GET MAP METHODS

    /** gets values from a client**/
    private Map getClientValues(ClientRepresentation cr){
        Map result = new HashMap<>(globals);

        result.put("accessTokenLifeSpan",(cr.getAttributes().get("access.token.lifespan") == null)? "": cr.getAttributes().get("access.token.lifespan"));
        result.put("accessType",(cr.isPublicClient())? "PUBLIC": ((cr.isBearerOnly())? "BEARER-ONLY":"CONFIDENTIAL"));
        result.put("adminURL",(cr.getAdminUrl() == null)? "": cr.getAdminUrl());
        result.put("baseURL",(cr.getBaseUrl() == null)? "": cr.getBaseUrl());
        result.put("clientID",(cr.getClientId() == null)? "": cr.getClientId());
        result.put("clientName",(cr.getName() == null)? "": cr.getName());
        result.put("description",(cr.getDescription() == null)? "":cr.getDescription());
        result.put("serviceAccountsEnabled",cr.isServiceAccountsEnabled().toString());
        result.put("standardFlowEnabled", cr.isStandardFlowEnabled().toString());
        result.put("useRefreshToken",(cr.getAttributes().get("use.refresh.tokens") ==null)? "false":cr.getAttributes().get("use.refresh.tokens"));

        //todo: these two feel a bit out of place
        String validRedirectURIS = "[\n";
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
        Map result = new HashMap<>(globals);
        result.put("name",rr.getName());
        result.put("ID",rr.getId());
        result.put("description",(rr.getDescription() == null)? "":rr.getDescription());

        return result;
    }

    //gets values the mapper needs. some configs in a mapper aren't present in another mapper, but it's ok
    private Map getMapperValues(ProtocolMapperRepresentation pmr) {
        Map values = new HashMap(globals);
        values.put("name-hyphenated",pmr.getName().replaceAll(" ","-"));
        values.put("name",pmr.getName());
        values.put("addToIdToken",pmr.getConfig().get("id.token.claim"));
        values.put("claimName",pmr.getConfig().get("claim.name"));
        values.put("claimValueType",pmr.getConfig().get("jsonType.label"));
        values.put("id",pmr.getId());
        values.put("includedClientAudience",pmr.getConfig().get("included.client.audience"));
        values.put("sessionNote",pmr.getConfig().get("user.session.note"));
        values.put("userAttribute",pmr.getConfig().get("user.attribute"));

        return  values;
    }





    // FILE METHODS
    public void closeFile() throws IOException{
        mainFW.close();
        outputsFW.close();
        variablesFW.close();
        versionsFW.close();
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
     * @return String representation of the clientUUID
     * @throws Exception when multiple clients or no clients found
     */
    private String getClientUUID(String clientID, RealmResource rr) throws Exception {
        ClientsResource clientsResource = rr.clients();
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

    private String removeBlankLine(String string){
         return string.replaceAll("(?m)^[ \t]*\r?\n", "");
    }
}

