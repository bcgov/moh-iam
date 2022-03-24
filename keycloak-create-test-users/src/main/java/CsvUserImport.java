
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Load users from a CSV file
 * Expected format:
 * First row headers
 * Col 1 - LDAP username (org-userid)
 * Col 2 - Firstname Lastname
 * Col 3 - Role
 * Col 4 - email
 * Col 5 - last modified (ignored)
 * Col 6 - email domain (ignored)
 * Col 7 - org name (ignored)
 * Col 8 - Keycloak username
 * @author greg.perkins
 */
public class CsvUserImport {
    private static Map<String,String> orgMap;
    private static String configPath;
    private static UserService userService;
    private static final Logger LOG = Logger.getLogger(CsvUserImport.class.getName());
    
    /**
     * Main method - runs the import
     * @param args
     * @throws IOException
     * @throws URISyntaxException 
     */
    public static void main(String[] args) throws IOException, URISyntaxException{
        InputStream is = CsvUserImport.class.getResourceAsStream("/idir.csv");
        //InputStream is = CsvUserImport.class.getResourceAsStream("/test.csv");
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line = reader.readLine(); //ignore first line
        line = reader.readLine(); 
        List<User> users = new ArrayList<>();
        while (line!=null){
            String[] parts = line.split(",");
            if (parts.length>=8){
                User user = parseUserFromLine(parts, "PLR");
                users.add(user);
                System.out.println("Parsed User:"+(user.getUsername()));
            }else{
                System.out.println("Can't parse: "+line);
            }
            line = reader.readLine();
        }
        
        if (args != null && args.length != 0) {
            configPath = args[0];
        } else {
            URL defaultLocation = Main.class.getClassLoader().getResource("configuration.properties");
            configPath = new File(defaultLocation.toURI()).getAbsolutePath();
        }
        LOG.info(String.format("Configuration file expected at '%s'.", configPath));

        // initializes the userService
        userService = new UserService(configPath);
        for (User user: users){
            try{
                if (userService.userExists(user)){
                    System.out.println("User "+user.getUsername()+" exists");
                }else{
                    System.out.println("User "+user.getUsername()+" does not exist... to be created");
                    userService.createUserInKeycloak(user.getUserRepresentation());
                }
                userService.addClientRolesInKeyCloak(user);
            }catch(Exception e){
                System.out.println(e.getMessage());
            }
        }
    }
    
    /**
     * Creates a User object from a line in the CSV
     * @param line String[] - parsed line of CSV file
     * @param client String - Name of KeyCloak client to assign role to
     * @return User
     */
    public static User parseUserFromLine(String[] line, String client){
        String[] names = line[1].split(" ");
        String email = line[3].toLowerCase();
        String domain = line[5].toLowerCase();
        String role = line[2];
        String userName = line[7].toLowerCase();
        User user = new User(userName);
        user.getUserRepresentation().setFirstName(names[0]);
        user.getUserRepresentation().setLastName(names[1]);
        user.getUserRepresentation().setEmail(email);
        user.recordClientRoles(client, role);
        String org = line[0].substring(0,line[0].indexOf("-"));
        user.addAttribute("org_details", getOrgMapping(org));
       return user;
    }
    
    /**
     * Maps an organization id to the appropriate keycloak org attribute
     * @param org - String
     * @return String
     */
    public static String getOrgMapping(String org){
        if (orgMap==null){
            //Lazy init map
            orgMap = new HashMap<>();
            orgMap.put("1909","{\"id\":\"00001909\",\"name\":\"College of Physicians & Surgeons of BC\"}");
            orgMap.put("1762","{\"id\":\"00001762\",\"name\":\"Northern Health Authority\"}");
            orgMap.put("1763","{\"id\":\"00001763\",\"name\":\"Interior Health Authority\"}");
            orgMap.put("1764","{\"id\":\"00001764\",\"name\":\"Vancouver Island Health Authority\"}");
            orgMap.put("1765","{\"id\":\"00001765\",\"name\":\"Vancouver Coastal Health Authority / PHC\"}");
            orgMap.put("1766","{\"id\":\"00001766\",\"name\":\"Fraser Health Authority\"}");
            orgMap.put("1767","{\"id\":\"00001767\",\"name\":\"Provincial Health Services Authority\"}");
            orgMap.put("20808","{\"id\":\"00020808\",\"name\":\"Lower Mainland - HIM\"}");
            orgMap.put("19319","{\"id\":\"00019319\",\"name\":\"VPP Cerner - Production\"}");
            orgMap.put("4252","{\"id\":\"00004252\",\"name\":\"Provider Registry Administration\"}");
            orgMap.put("15895","{\"id\":\"00015895\",\"name\":\"PRS-MOH\"}");
        }
        return orgMap.get(org);
    }
}
