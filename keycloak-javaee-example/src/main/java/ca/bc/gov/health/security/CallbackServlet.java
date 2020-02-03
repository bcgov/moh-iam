package ca.bc.gov.health.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;

import fish.payara.security.openid.api.OpenIdContext;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Principal;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.security.enterprise.SecurityContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/callback")
public class CallbackServlet extends HttpServlet {

    @Inject
    OpenIdContext context;
    
    @Inject
    SecurityContext securityContext;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        System.out.println("\nmade it here");
        
        Principal userPrincipal = request.getUserPrincipal();
        
        PrintWriter out = response.getWriter();
        
        out.println("\nprincipal: " + userPrincipal.toString());
        out.println("\nprincipal name: " + userPrincipal.getName());
        
        out.println("\ncaller name: " + context.getCallerName());
        out.println("\ncaller groups: " + context.getCallerGroups());
        out.println("\nsubject: " + context.getSubject());
        out.println("\naccess token: " + context.getAccessToken());
        out.println("\nidentify token: " + context.getIdentityToken());
        out.println("\nuser claims: " + context.getClaimsJson());
        
        String kcPublicKey =  "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA3ysn0Gh/xjUinILW9LXfeNPS2SjxH09M6WukNVrFHlOKnblLArjlarwwuKQ4Q5FRytMYpkHomyP7l8h2DlqsmqqEtDhI3zWyzm+NWNR/IJ13F1j4B0MWu7wREe1Ag8JaeKMM16WO2Ru5azVqe+dbAyyxLEWRTJ7qxxGWl4jhlM1L78Ea1f0yxQeLwL9qafIGM+nM/aYTR7JnAD55YjRvkpDsIOqok39/YI/n50hxi9PpS1mGIo7l3NvePTCVeSY5raL16ouh6EaiuXQHk/PwyCjFTYPPHng+y+Jfjshw4Iu6870i13FaHVtqXxdiWDeo0b843EB9pvXIdalNZ/YxwQIDAQAB";
        
        PublicKey publicKey;
        Jws<Claims> claimsJws;
        try {
            publicKey = decodePublicKey(pemToDer(kcPublicKey));
            Jws<Claims> identityJws = Jwts.parser()
                    .setSigningKey(publicKey)
                    .parseClaimsJws(context.getIdentityToken().toString());
            out.println("\nidentity token JWS: " + identityJws);
            claimsJws = Jwts.parser()
                .setSigningKey(publicKey)
                .parseClaimsJws(context.getAccessToken().toString());
            out.println("\nclaim JWS: " + claimsJws);
            String role = ((Map<String,Object>) ((Map<String,Object>) claimsJws.getBody().get("resource_access")).get("KeycloakIntegrationExample")).get("roles").toString();
            out.println("\nrole: " + role);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | NoSuchProviderException ex) {
            Logger.getLogger(CallbackServlet.class.getName()).log(Level.SEVERE, null, ex);
        }

        out.println("\ncaller principal name: " + securityContext.getCallerPrincipal().getName());
        out.println("\nisCallerInRole? PSDADMIN: " + securityContext.isCallerInRole("PSDADMIN"));
        out.println("\nisCallerInRole? FAKEROLE: " + securityContext.isCallerInRole("FAKEROLE"));
        out.println("\nisCallerInRole? view-profile: " + securityContext.isCallerInRole("view-profile"));
        out.println("\nisCallerInRole? manage-account-links: " + securityContext.isCallerInRole("manage-account-links"));
    }
    
    public static byte[] pemToDer(String pem) throws IOException {
        return Base64.getDecoder().decode(stripBeginEnd(pem));
    }

    public static String stripBeginEnd(String pem) {
        String stripped = pem.replaceAll("-----BEGIN (.*)-----", "");
        stripped = stripped.replaceAll("-----END (.*)----", "");
        stripped = stripped.replaceAll("\r\n", "");
        stripped = stripped.replaceAll("\n", "");
        return stripped.trim();
    }


    public static PublicKey decodePublicKey(byte[] der) throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException {
        X509EncodedKeySpec spec = new X509EncodedKeySpec(der);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePublic(spec);
    }    
}
