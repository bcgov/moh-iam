
import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;
import org.apache.commons.io.FileUtils;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ClientRepresentation;

import java.io.*;

import java.net.URL;
import java.util.*;

import java.io.File;

//Import Scanner class


public class Main {
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

    public static void main(String[] args) throws Exception {
        Properties configProperties = getProperties();

        JsonObject inputJSON = readJsonFile(configProperties.getProperty("inputFile"));

        RealmResource realmResource = getRealmResource(configProperties);
        String environmentName = configProperties.getProperty("environment");
        String realmName = configProperties.getProperty("realm");
        String path = configProperties.getProperty("outputPath");
        String phase = configProperties.getProperty("phase");

        String realmFolderPath = path + realmName.toLowerCase(Locale.ROOT);
        recursiveDelete(new File(realmFolderPath));
        if(!(new File(realmFolderPath).mkdir()) && !(new File(realmFolderPath).exists())) {
            throw new RuntimeException("Folder can't be created and doesn't exists");
        }

        FileWriter realmFW = createFileWriter(realmFolderPath+"\\main.tf");
        FileWriter importFW = createFileWriter(path + "imports.bash");
        FileWriter realmVersions = createFileWriter(realmFolderPath+"\\versions.tf");

        Map<String, String> env = System.getenv();
        for(String sthing: env.keySet()) System.out.println(sthing);
        JsonArray clients =(JsonArray) inputJSON.get("clients");
        clients.forEach(client -> {
            JsonObject JSONclient = (JsonObject) client;
            try{
                ClientService clientService = new ClientService(realmResource,
                        (String) JSONclient.get("clientID"),
                        environmentName,
                        (String) JSONclient.get("clientType"),
                        realmFolderPath+"\\");

                if (phase.equalsIgnoreCase("1")){
                    //create all non-dependent
                write(realmFW, clientService.createAllNonDependentResources());
                write(importFW,clientService.createAllNonDependentImports());
                }else if(phase.equalsIgnoreCase("2")){
                    //create all resources, but import only dependent
                write(realmFW, clientService.createResources());
                write(importFW,clientService.createAllDependentImports());
                }
                clientService.closeFile();
            }catch (Exception e){
                e.printStackTrace();
            }
        });

        realmVersions.write(keycloakProvider);

        realmFW.close();
        importFW.close();
        realmVersions.close();
    }

    private static Properties getProperties() throws Exception{
        URL defaultLocation = Main.class.getClassLoader().getResource("configuration.properties");
        String configPath = new File(defaultLocation.toURI()).getAbsolutePath();

        Properties configProperties = new Properties();
        File file = new File(configPath);

        InputStream inputStream = (file.exists())? new FileInputStream(file) : Main.class.getResourceAsStream(configPath);
        Objects.requireNonNull(inputStream, String.format("Configuration file not found at '%s'.", configPath));
        configProperties.load(inputStream);
        return configProperties;
    }
    private static RealmResource getRealmResource(Properties configProperties) {
        Keycloak keycloak = KeycloakBuilder.builder() //
                .serverUrl(getServerUrl(configProperties)) //
                .realm(configProperties.getProperty("realm")) //
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS) //
                .clientId(configProperties.getProperty("clientID")) //
                .clientSecret(getClientSecret(configProperties)) //
                .build();
        RealmResource realmResource = keycloak.realm(configProperties.getProperty("realm"));

        return  realmResource;
    }
    private static JsonObject readJsonFile(String inputFile) {
        JsonObject result = null;
        try {
            InputStream JSONStream;
            File jsonFile = new File(inputFile);
            if (jsonFile.exists()) {
                JSONStream = new FileInputStream(jsonFile);
            } else {
                JSONStream = Main.class.getClassLoader().getResourceAsStream(inputFile);
            }
            Reader reader = new BufferedReader(new InputStreamReader(JSONStream));
            result = (JsonObject) Jsoner.deserialize(reader);
            reader.close();
        } catch (Exception e){
            e.printStackTrace();
        }
        return result;
    }
    private static FileWriter createFileWriter(String path) throws IOException {
        System.out.println(path);
        if (!(new File(path).createNewFile()) && !(new File(path).exists())) {
            throw new RuntimeException("File can't be created");
        }
        return new FileWriter(path);
    }


    private static void recursiveDelete(File file)
    {
        try {
            FileUtils.deleteDirectory(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (!file.exists()) return;
        if (file.isDirectory())
        {
            for (File f : file.listFiles())  recursiveDelete(f);
        }
        file.delete();
    }

    private static void write(FileWriter filewriter, String input){
        try{
            filewriter.write(input);
        }catch (Exception e){
            throw new RuntimeException();
        }
    }

    private static String getClientSecret(Properties configProperties) {
        Map<String, String> env = System.getenv();
        switch (configProperties.getProperty("environment")){
            case "KEYCLOAK_DEV": return env.get("TF_VAR_dev_client_secret");
            case "KEYCLOAK_TEST": return env.get("TF_VAR_test_client_secret");
            case "KEYCLOAK_PROD": return env.get("TF_VAR_prod_client_secret");
            default: throw new RuntimeException();
        }
    }

    private static String getServerUrl(Properties configProperties) {
        switch (configProperties.getProperty("environment")){
            case "KEYCLOAK_DEV": return "https://common-logon-dev.hlth.gov.bc.ca/auth";
            case "KEYCLOAK_TEST": return "https://common-logon-test.hlth.gov.bc.ca/auth";
            case "KEYCLOAK_PROD": return "https://common-logon.hlth.gov.bc.ca/auth";
            default: throw new RuntimeException();
        }
    }
}