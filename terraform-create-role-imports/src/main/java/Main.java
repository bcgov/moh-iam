
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

    public static void main(String[] args) throws Exception {
        Properties configProperties = getProperties();
        String outputPath = configProperties.getProperty("outputPath");
        String environment = configProperties.getProperty("environment");
        System.out.println(outputPath +"input_"+environment + ".json");
        JsonObject inputJSON = readJsonFile(outputPath +"input_"+environment + ".json");

        File realm = new File(outputPath + configProperties.getProperty("realm").toLowerCase(Locale.ROOT));

        recursiveDelete(realm);
        RealmService rs = new RealmService(configProperties,(JsonArray) inputJSON.get("clients"));

        rs.writeClients();
        rs.closeFileWriters();
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

}