<#-- VeriVoto: base/login/error.ftl (26.8.1; keycloak.v2 has no error.ftl of its own) plus a way out (SR15).
     The parent's «Volver a la aplicación» link needs client.baseUrl, which the realm does not set for verivoto-admin, and
     url.loginRestartFlowUrl needs the authentication session that has just expired, so the button goes to the console
     (theme property vvConsoleUrl), which starts a new sign-in. Not shown to the voter SSO client: voters come back
     through the personal link of their invitation email. -->
<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=false; section>
    <#if section = "header">
        ${kcSanitize(msg("errorTitle"))?no_esc}
    <#elseif section = "form">
        <div id="kc-error-message">
            <p class="instruction">${kcSanitize(message.summary)?no_esc}</p>
            <#if traceId??>
                <p class="instruction" id="traceId">${msg("traceIdSupportMessage", traceId)}</p>
            </#if>
            <#if skipLink??>
            <#else>
                <#if client?? && client.baseUrl?has_content>
                    <p><a id="backToApplication" href="${client.baseUrl}">${msg("backToApplication")}</a></p>
                </#if>
            </#if>
            <#if properties.vvConsoleUrl?has_content && !(client?? && client.clientId == "verivoto-voter")>
                <a id="vv-restart-login" class="vv-restart ${properties.kcButtonClass!} ${properties.kcButtonSecondaryClass!} ${properties.kcButtonBlockClass!}" href="${properties.vvConsoleUrl}">${msg("backToLogin")}</a>
            </#if>
        </div>
    </#if>
</@layout.registrationLayout>
