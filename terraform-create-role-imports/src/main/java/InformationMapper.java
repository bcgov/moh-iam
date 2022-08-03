import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;

import java.util.*;


public class InformationMapper {
    private HashMap<String,String> client;
    private HashMap<String,Map> roles;
    private HashMap<String,Map> mappers;

    /**
     * constructor
     */
    public InformationMapper(RealmResource rr, String clientID, String envName) {
        client = new HashMap<>();
        roles = new HashMap<>();
        mappers = new HashMap<>();

        client.put("environment", envName);
        client.put("realmName", rr.toRepresentation().getId());
        client.put("clientNameLowerCase",clientID.toLowerCase(Locale.ROOT));
        client.put("clientUUID", getClientUUID(clientID, rr));

        ClientRepresentation cr = rr.clients().get(client.get("clientUUID")).toRepresentation();
        getClientValues(cr);
    }

    public void getClientValues(ClientRepresentation cr){
        client.put("directAccessGrantsEnabled",cr.isDirectAccessGrantsEnabled().toString());
        client.put("backChannelLogoutSessionRequired",(cr.getAttributes().get("backchannel.logout.session.required") == null)? "":String.format("backchannel_logout_session_required = %s", cr.getAttributes().get("backchannel.logout.session.required")));
        client.put("pkceCodeChallengeMethod",(cr.getAttributes().get("pkce.code.challenge.method") == null)? "":cr.getAttributes().get("pkce.code.challenge.method"));
        client.put("accessTokenLifeSpan",(cr.getAttributes().get("access.token.lifespan") == null)? "": cr.getAttributes().get("access.token.lifespan"));
        client.put("accessType",(cr.isPublicClient())? "PUBLIC": ((cr.isBearerOnly())? "BEARER-ONLY":"CONFIDENTIAL"));
        client.put("adminURL",(cr.getAdminUrl() == null)? "": cr.getAdminUrl());
        client.put("baseURL",(cr.getBaseUrl() == null)? "": cr.getBaseUrl());
        client.put("fullScopeAllowed",cr.isFullScopeAllowed().toString());
        client.put("clientID",cr.getClientId());
        client.put("clientName",(cr.getName() == null)? "": cr.getName());
        client.put("description",(cr.getDescription() == null)? "":cr.getDescription());
        client.put("serviceAccountsEnabled",cr.isServiceAccountsEnabled().toString());
        client.put("standardFlowEnabled", cr.isStandardFlowEnabled().toString());
        client.put("useRefreshToken",(cr.getAttributes().get("use.refresh.tokens") ==null)? "":String.format("use_refresh_tokens = %s",cr.getAttributes().get("use.refresh.tokens")));

        //todo: these two feel a bit out of place
        String validRedirectURIS = "[\n";
        for (String uri : cr.getRedirectUris()) {
            validRedirectURIS += String.format("\t\t\"%s\",\n",uri);
        }
        client.put("validRedirectURIS",validRedirectURIS +"\t]");

        String webOrigins = "[\n";
        for(String uri : cr.getWebOrigins()) {
            webOrigins += String.format("\t\t\"%s\",\n",uri);
        }
        client.put("webOrigins",webOrigins + "\t]");
    }

    public String getClientInfo(String key){
        return client.get(key);
    }

    public Map getClientMap(){
        return client;
    }

    public void includeInGlobal(String key,String value){
        client.put(key,value);
    }

    public Map getMapperInformation(ProtocolMapperRepresentation pmr){
        String id = pmr.getId();
        if(!mappers.containsKey(id)){
            Map values = new HashMap(client);
            values.put(("addToUserInfo"), (pmr.getConfig().get("userinfo.token.claim") == null)? "true":pmr.getConfig().get("userinfo.token.claim"));
            values.put(("addToAccessToken"), (pmr.getConfig().get("access.token.claim") == null)? "true":pmr.getConfig().get("access.token.claim"));
            values.put(("addToIdToken"), (pmr.getConfig().get("id.token.claim") == null)? "true":pmr.getConfig().get("id.token.claim"));

            values.put("name-hyphenated",pmr.getName().replaceAll(" ","-"));
            values.put("name",pmr.getName());
            values.put("addToIdToken",pmr.getConfig().get("id.token.claim"));
            values.put("claimName",pmr.getConfig().get("claim.name"));
            values.put("claimValue",pmr.getConfig().get("claim.value"));
            values.put("claimValueType",pmr.getConfig().get("jsonType.label"));
            values.put("id",id);
            values.put("sessionNote",pmr.getConfig().get("user.session.note"));
            values.put("userAttribute",(pmr.getConfig().get("user.attribute") == null)? "":pmr.getConfig().get("user.attribute"));

            if(pmr.getConfig().get("included.client.audience") != null) {
                values.put("includedClientOrCustomAudience","included_client_audience = \"" + (pmr.getConfig().get("included.client.audience")) + "\"");

            } else if(pmr.getConfig().get("included.custom.audience") != null) {
                values.put("includedClientOrCustomAudience","included_custom_audience = \"" + (pmr.getConfig().get("included.custom.audience")) + "\"");
            }else{
                values.put("includedClientOrCustomAudience","");

            }




            mappers.put(id,values);
        }
        return mappers.get(id);
    }

    public Map getRoleInformation(RoleRepresentation rr) {
        if(!roles.containsKey(rr.getId())){
            Map result = new HashMap<>(client);

            result.put("name",rr.getName());
            result.put("ID",rr.getId());
            result.put("description",(rr.getDescription() == null)? "":rr.getDescription());

            roles.put(rr.getId(),result);
        }
        return roles.get(rr.getId());
    }

    /**
     * gets the Client's UUID from a clientID
     *
     * @return String representation of the clientUUID
     * @throws Exception when multiple clients or no clients found
     */
    private String getClientUUID(String clientID, RealmResource rr) {
        ClientsResource clientsResource = rr.clients();
        List<ClientRepresentation> clientRepresentationList = clientsResource.findByClientId(clientID);
        if (clientRepresentationList.size() > 1) {
            throw new RuntimeException("more than 1 client found");
        } else if (clientRepresentationList.size() < 1) {
            System.out.println(clientID);
            throw new RuntimeException("client not found");
        } else {
            return clientRepresentationList.get(0).getId();
        }
    }
}





