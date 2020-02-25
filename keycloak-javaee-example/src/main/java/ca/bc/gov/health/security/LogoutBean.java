/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.bc.gov.health.security;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import javax.enterprise.context.RequestScoped;
import javax.faces.context.FacesContext;
import javax.inject.Named;

/**
 * Request scoped bean with a method to log the user out of their current Keycloak and application sessions
 */
@RequestScoped
@Named("LogoutBean")
public class LogoutBean implements Serializable {
    
    public void logout() throws UnsupportedEncodingException, IOException {
        
       //Call the keycloak realm end-session endpoint to delete the session from keycloak
       FacesContext.getCurrentInstance()
               .getExternalContext()
               .redirect("https://localhost:8543/auth/realms/moh-users-realm/protocol/openid-connect/logout?redirect_uri=" 
                       + URLEncoder.encode("https://localhost:8181/keycloak-javaee-example/callback", "UTF-8"));
       
       //Invalidate the user browser session
       FacesContext.getCurrentInstance().getExternalContext().invalidateSession(); 
    }
   
}
