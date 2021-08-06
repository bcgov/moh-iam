<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=social.displayInfo displayWide=(realm.password && social.providers??); section>
    <#if section = "header">
        Welcome to ${client.getName()}
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
                    <#list social.providers as p>	
						<#-- by default all identity providers are shown -->
						<li class="${properties.kcFormSocialAccountListLinkClass!}"><a style="display: block" href="${p.loginUrl}" id="zocial-${p.alias}" class="zocial ${p.providerId}"> <span>Login with ${p.displayName}</span></a></li>
                    </#list>
                </ul>
            </div>
        </#if>
		
		<#-- this script checks the 'idps_to_show' query parameter and hides any IDP's that are not listed-->
		<script>
		   <#if social.providers??>
				var idProviders=[<#list social.providers as p>'${p.alias?string}',</#list>]
				hideIdProviders(idProviders);
		   </#if>
		</script>
    </div>
	
	<#if client.clientId != "account">
		<div class="link-account-info centered">
		  <a id="show-link-instructions" href="#" onclick="showInstructionsModal();return false;" >Click for instructions to link a new ID type to your account.</a>
		</div>
	</#if>
	  
	<div id="instructions-modal" class="modal">
	  <div class="modal-content" >
		<span class="close" onclick="hideInstructionsModal();return false;">&times;</span>
		Follow these steps to link a new Identity Provider to your account:
		<ol>
		  <li><a id="account-management-link" target="_blank" >Go to the Ministry of Health Account Management page.</a></li>
		  <li>Login with the ID currently linked to your account.</li>
		  <li>Navigate to the <strong>ID Types</strong> tab.</li>
		  <li>Click the <strong>Add</strong> button next to the ID Type you wish to link.</li>
		  <li>Login with your credentials for that identity type.</li>
		  <li>Click <strong>Sign Out</strong> to end your session.</li>
		</ol>
		You will now be able to use these credentials for signing in to your applications. 
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
