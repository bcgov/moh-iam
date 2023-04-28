<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=social.displayInfo displayWide=(realm.password && social.providers??); section>
    <#if section = "header">
        <#if client.getName()?has_content>
            Welcome to ${client.getName()}
        <#else>
            Welcome to 
        </#if> 
    <#elseif section = "form">
    <div id="kc-form" <#if realm.password && social.providers??>class="${properties.kcContentWrapperClass!}"</#if>>
	  <#if !social.providers?? ||  social.providers?size == 0>
      <div id="kc-form-wrapper" <#if realm.password && social.providers??>class="${properties.kcFormSocialAccountContentClass!} ${properties.kcFormSocialAccountClass!}"</#if>>
        <#if realm.password>
            <form id="kc-form-login" onsubmit="login.disabled = true; return true;" action="${url.loginAction}" method="post">
                <div class="${properties.kcFormGroupClass!}">
                    <label for="username" class="${properties.kcLabelClass!}"><#if !realm.loginWithEmailAllowed>${msg("username")}<#elseif !realm.registrationEmailAsUsername>${msg("usernameOrEmail")}<#else>${msg("email")}</#if></label>

                    <#if usernameEditDisabled??>
                        <input tabindex="1" id="username" class="${properties.kcInputClass!}" name="username" value="${(login.username!'')}" type="text" disabled />
                    <#else>
                        <input tabindex="1" id="username" class="${properties.kcInputClass!}" name="username" value="${(login.username!'')}"  type="text" autofocus autocomplete="off" />
                    </#if>
                </div>

                <div class="${properties.kcFormGroupClass!}">
                    <label for="password" class="${properties.kcLabelClass!}">${msg("password")}</label>
                    <input tabindex="2" id="password" class="${properties.kcInputClass!}" name="password" type="password" autocomplete="off" />
                </div>

                <div class="${properties.kcFormGroupClass!} ${properties.kcFormSettingClass!}">
                    <div id="kc-form-options">
                        <#if realm.rememberMe && !usernameEditDisabled??>
                            <div class="checkbox">
                                <label>
                                    <#if login.rememberMe??>
                                        <input tabindex="3" id="rememberMe" name="rememberMe" type="checkbox" checked> ${msg("rememberMe")}
                                    <#else>
                                        <input tabindex="3" id="rememberMe" name="rememberMe" type="checkbox"> ${msg("rememberMe")}
                                    </#if>
                                </label>
                            </div>
                        </#if>
                        </div>
                        <div class="${properties.kcFormOptionsWrapperClass!}">
                            <#if realm.resetPasswordAllowed>
                                <span><a tabindex="5" href="${url.loginResetCredentialsUrl}">${msg("doForgotPassword")}</a></span>
                            </#if>
                        </div>

                  </div>

                  <div id="kc-form-buttons" class="${properties.kcFormGroupClass!}">
                      <input type="hidden" id="id-hidden-input" name="credentialId" <#if auth.selectedCredential?has_content>value="${auth.selectedCredential}"</#if>/>
                      <input tabindex="4" class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}" name="login" id="kc-login" type="submit" value="${msg("doLogInButton")}"/>
                  </div>
            </form>
        </#if>
      </div>
	  </#if>
        <#if realm.password && social.providers??>
            <div id="kc-social-providers" class="${properties.kcFormSocialAccountContentClass!} ${properties.kcFormSocialAccountClass!}">
                <ul class="${properties.kcFormSocialAccountListClass!}">
                    <#-- by default all identity providers are shown, different message is displayed in case of IDP outage -->
                    <#list social.providers as p>	
                        <#if p.displayName?contains("outage")>
                            <li class="${properties.kcFormSocialAccountListLinkClass!} idp-outage"><a style="display: block;" href="${p.loginUrl}" id="zocial-${p.alias}" class="zocial ${p.providerId}"> <span>Login with ${p.displayName?remove_ending("outage")} is currently unavailable. The team is working to restore the service.</span></a></li>
                        <#else>
						    <li class="${properties.kcFormSocialAccountListLinkClass!}"><a style="display: block" href="${p.loginUrl}" id="zocial-${p.alias}" class="zocial ${p.providerId}"> <span>Login with ${p.displayName}</span></a></li>
                        </#if>
                    </#list>
                </ul>
            </div>
        </#if>
		
		<#-- this script checks the 'idps_to_show' query parameter and hides any IDP's that are not listed -->
		<script>
		   <#if social.providers??>
				var idProviders=[<#list social.providers as p>'${p.alias?string}',</#list>]
				hideIdProviders(idProviders);
		   </#if>
		</script>
    </div>
	
	<#if client.clientId != "account">
		<div class="link-account-info centered">
		  <a id="show-link-instructions" href="#" onclick="showInstructionsModal();return false;" >Need help? Contact us</a>
		</div>
	</#if>
	  
	<div id="instructions-modal" class="modal">
	  <div class="modal-content" >
		<span class="close" onclick="hideInstructionsModal();return false;">&times;</span>
        <p>
            <b>Email</b>
            <br>
            <a href="mailto:hlth.helpdesk@gov.bc.ca">hlth.helpdesk@gov.bc.ca</a>
            <br>
            <b>Hours</b>
            <br>
            Monday–Friday (excluding statutory holidays)
            <br>
            8:00AM–4:30PM (Pacific Time)
            <br><br>
            For Health Authority ID account help, please consult your local IT help desk.
            <br><br>
            For all other account specific help (e.g. IDIR, BCeID, etc.), please consult the applicable help desk on the login page where you enter your account credentials.
        </p>
	  </div>
	</div>

    <#elseif section = "info" >
        <#if realm.password && realm.registrationAllowed && !usernameEditDisabled??>
            <div id="kc-registration">
                <span>${msg("noAccount")} <a tabindex="6" href="${url.registrationUrl}">${msg("doRegister")}</a></span>
            </div>
        </#if>
    </#if>

</@layout.registrationLayout>
