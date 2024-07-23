<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('username','password') displayInfo=realm.password && realm.registrationAllowed && !registrationDisabled??; section>
    <#if section = "header">
        <#if client.getName()?has_content>
            Welcome to ${client.getName()}
        <#else>
            Welcome to
        </#if>
    <#elseif section = "form">
        <div id="kc-form" <#if realm.password && social.providers??>class="${properties.kcContentWrapperClass!}"</#if>>
            <#if !social.providers?? ||  social.providers?size == 0>
                <div id="kc-form-wrapper">
                    <#if realm.password>
                        <form id="kc-form-login" onsubmit="login.disabled = true; return true;" action="${url.loginAction}" method="post">
                            <#if !usernameHidden??>
                                <div class="${properties.kcFormGroupClass!}">
                                    <label for="username" class="${properties.kcLabelClass!}"><#if !realm.loginWithEmailAllowed>${msg("username")}<#elseif !realm.registrationEmailAsUsername>${msg("usernameOrEmail")}<#else>${msg("email")}</#if></label>

                                    <input tabindex="1" id="username" class="${properties.kcInputClass!}" name="username" value="${(login.username!'')}"  type="text" autofocus autocomplete="off"
                                           aria-invalid="<#if messagesPerField.existsError('username','password')>true</#if>"
                                    />

                                    <#if messagesPerField.existsError('username','password')>
                                        <span id="input-error" class="${properties.kcInputErrorMessageClass!}" aria-live="polite">
                                                ${kcSanitize(messagesPerField.getFirstError('username','password'))?no_esc}
                                        </span>
                                    </#if>

                                </div>
                            </#if>

                            <div class="${properties.kcFormGroupClass!}">
                                <label for="password" class="${properties.kcLabelClass!}">${msg("password")}</label>

                                <div class="${properties.kcInputGroup!}">
                                    <input tabindex="2" id="password" class="${properties.kcInputClass!}" name="password" type="password" autocomplete="off"
                                           aria-invalid="<#if messagesPerField.existsError('username','password')>true</#if>"
                                    />
                                    <button class="pf-c-button pf-m-control" type="button" aria-label="${msg("showPassword")}"
                                            aria-controls="password"  data-password-toggle
                                            data-label-show="${msg('showPassword')}" data-label-hide="${msg('hidePassword')}">
                                        <i class="fa fa-eye" aria-hidden="true"></i>
                                    </button>
                                </div>

                                <#if usernameHidden?? && messagesPerField.existsError('username','password')>
                                    <span id="input-error" class="${properties.kcInputErrorMessageClass!}" aria-live="polite">
                                            ${kcSanitize(messagesPerField.getFirstError('username','password'))?no_esc}
                                    </span>
                                </#if>

                            </div>

                            <div class="${properties.kcFormGroupClass!} ${properties.kcFormSettingClass!}">
                                <div id="kc-form-options">
                                    <#if realm.rememberMe && !usernameHidden??>
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
                            <#if p.displayName?c_lower_case?contains("outage")>
                                <li class="${properties.kcFormSocialAccountListLinkClass!} idp-outage"><a style="display: block;" href="${p.loginUrl}" id="zocial-${p.alias}" class="zocial ${p.providerId}"> <span>Login with ${p.displayName?keep_before_last(" ")} is currently unavailable. Teams supporting this identity provider are working to restore it.</span></a></li>
                            <#else>
                                <li class="${properties.kcFormSocialAccountListLinkClass!}"><a style="display: block" href="${p.loginUrl}" id="zocial-${p.alias}" class="zocial ${p.providerId}"> <span>Login with ${p.displayName}</span></a></li>
                            </#if>
                        </#list>
                    </ul>
                </div>

                <script>
                    <#if social.providers??>
                    var idProviders=[<#list social.providers as p>'${p.alias?string}',</#list>]
                    hideIdentityProviders(idProviders);
                    </#if>
                </script>
            </#if>
        </div>

    <#elseif section = "socialProviders" >


    <#elseif section = "contact" >
        <#if client.clientId != "account" && client.clientId != "account-console">
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
                    <a href="mailto:HLTH.ServiceDesk@gov.bc.ca">HLTH.ServiceDesk@gov.bc.ca</a>
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
        <#if realm.password && realm.registrationAllowed && !registrationDisabled??>
            <div id="kc-registration-container">
                <div id="kc-registration">
                    <span>${msg("noAccount")} <a tabindex="6" href="${url.registrationUrl}">${msg("doRegister")}</a></span>
                </div>
            </div>
        </#if>
    </#if>

</@layout.registrationLayout>
