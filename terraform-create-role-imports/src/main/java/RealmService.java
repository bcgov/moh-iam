import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonObject;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ClientRepresentation;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

public class RealmService {
    RealmResource rr;
    String environmentName;
    String realmName;

    String outputPath;
    String realmFolderPath;
    String phase;
    JsonArray clients;


    FileWriter realmFW;
    FileWriter importFW;
    FileWriter realmVersions;

    private static String keycloakProvider = """
                terraform {
                  required_providers {
                    keycloak = {
                      source  = "mrparkers/keycloak"
                      version = ">= 3.0.0"
                    }
                  }
                }
                """;

    public  RealmService(Properties configProperties,JsonArray clients) throws IOException {
        rr = getRealmResource(configProperties);

        this.environmentName = configProperties.getProperty("environment");
        this.realmName = configProperties.getProperty("realm");
        this.phase = configProperties.getProperty("phase");

        String path = configProperties.getProperty("outputPath");
        realmFolderPath = path + realmName.toLowerCase(Locale.ROOT);

        if(!(new File(realmFolderPath).mkdir()) && !(new File(realmFolderPath).exists())) {
            throw new RuntimeException("Folder can't be created and doesn't exists");
        }
        realmFW = createFileWriter(realmFolderPath+"\\main.tf");
        importFW = createFileWriter(path + "imports.bash");
        realmVersions = createFileWriter(realmFolderPath+"\\versions.tf");

        this.clients = clients;

        realmVersions.write(keycloakProvider);

    }

    public void writeClients(){
        clients.forEach(client -> {
            JsonObject JSONclient = (JsonObject) client;
            try{
                System.out.println((String) JSONclient.get("clientID") + JSONclient.get("clientType"));

                List<ClientRepresentation> clientRepresentationList = rr.clients().findAll((String) JSONclient.get("clientID"),true,true,0,((boolean) JSONclient.get("isList"))? -1:1);

                for (ClientRepresentation cr: clientRepresentationList) {
                    System.out.println("\t"+cr.getClientId());
                    ClientService clientService = new ClientService(rr,
                            cr.getClientId(),
                            environmentName,
                            (String) JSONclient.get("clientType"),
                            realmFolderPath + "\\");
                    if (phase.equalsIgnoreCase("1") && !(boolean)JSONclient.get("isMaintained")) {
                        //create all non-dependent
                        write(realmFW, clientService.createAllNonDependentResources());
                        write(importFW, clientService.createAllNonDependentImports());
                    } else {
                        //create all resources, but import only dependent
                        write(realmFW, clientService.createResources());
                        if(!(boolean)JSONclient.get("isMaintained")) {
                            System.out.println("writing imports for : " + cr.getClientId());
                            write(importFW, clientService.createAllDependentImports());
                        }
                    }
                    clientService.closeFile();
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        });
    }

    public void closeFileWriters() throws IOException {
        realmFW.close();
        importFW.close();
        realmVersions.close();
    }

    private static void write(FileWriter filewriter, String input){
        try{
            filewriter.write(input);
        }catch (Exception e){
            throw new RuntimeException();
        }
    }

    private static RealmResource getRealmResource(Properties configProperties) {
        Keycloak keycloak = KeycloakBuilder.builder() //
                .serverUrl(getServerUrl(configProperties.getProperty("environment"))) //
                .realm(configProperties.getProperty("realm")) //
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS) //
                .clientId(configProperties.getProperty("clientID")) //
                .clientSecret(getClientSecret(configProperties.getProperty("environment"))) //
                .build();
        RealmResource realmResource = keycloak.realm(configProperties.getProperty("realm"));

        return  realmResource;
    }

    private static FileWriter createFileWriter(String path) throws IOException {
//        System.out.println(path);
        if (!(new File(path).createNewFile()) && !(new File(path).exists())) {
            throw new RuntimeException("File can't be created");
        }
        return new FileWriter(path);
    }

    private static String getClientSecret(String environmentName) {
        Map<String, String> env = System.getenv();
        switch (environmentName){
            case "KEYCLOAK_DEV": return env.get("TF_VAR_dev_client_secret");
            case "KEYCLOAK_TEST": return env.get("TF_VAR_test_client_secret");
            case "KEYCLOAK_PROD": return env.get("TF_VAR_prod_client_secret");
            default: throw new RuntimeException();
        }
    }

    private static String getServerUrl(String environmentName) {
        switch (environmentName){
            case "KEYCLOAK_DEV": return "https://common-logon-dev.hlth.gov.bc.ca/auth";
            case "KEYCLOAK_TEST": return "https://common-logon-test.hlth.gov.bc.ca/auth";
            case "KEYCLOAK_PROD": return "https://common-logon.hlth.gov.bc.ca/auth";
            default: throw new RuntimeException();
        }
    }
}
