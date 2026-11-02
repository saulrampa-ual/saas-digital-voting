<#-- VeriVoto: keycloak.v2 login-update-password.ftl (26.8.1) plus the password requirements of the realm policy.
     Used to set the first password, to change it and at the end of "forgot password". Only rules the policy defines
     are listed (passwordPolicies); the parent's change-time validator is replaced by the live list in js/verivoto.js. -->
<#import "template.ftl" as layout>
<#import "password-commons.ftl" as passwordCommons>
<#import "field.ftl" as field>
<#import "buttons.ftl" as buttons>
<#macro liveRule name value text>
    <li class="vv-rule" data-rule="${name}" data-value="${value?c}" data-state="pending"><span class="vv-rule-text">${text}</span><span class="vv-rule-state vv-sr"> (${msg("vvPasswordRulePending")})</span></li>
</#macro>
<#macro serverRule text>
    <li class="vv-rule" data-state="server"><span class="vv-rule-text">${text}</span><span class="vv-rule-state"> (${msg("vvPasswordRuleOnSave")})</span></li>
</#macro>

<@layout.registrationLayout displayMessage=!messagesPerField.existsError('password','password-confirm'); section>
<!-- template: login-update-password.ftl -->
    <#if section = "header">
        ${msg("updatePasswordTitle")}
    <#elseif section = "form">
        <form id="kc-passwd-update-form" class="${properties.kcFormClass!}" onsubmit="login.disabled = true; return true;" action="${url.loginAction}" method="post" novalidate="novalidate">
            <@field.password name="password-new" label=msg("passwordNew") fieldName="password" autocomplete="new-password" autofocus=true />

            <div class="vv-rules" id="vv-pw-rules" data-met="${msg('vvPasswordRuleMet')}" data-pending="${msg('vvPasswordRulePending')}">
                <p class="vv-rules-title" id="vv-pw-rules-title">${msg("vvPasswordRulesTitle")}</p>
                <ul aria-labelledby="vv-pw-rules-title">
                    <#if passwordPolicies.length??><@liveRule name="length" value=passwordPolicies.length text=msg("vvPasswordRuleLength", passwordPolicies.length?c) /></#if>
                    <#if passwordPolicies.maxLength??><@liveRule name="maxLength" value=passwordPolicies.maxLength text=msg("vvPasswordRuleMaxLength", passwordPolicies.maxLength?c) /></#if>
                    <#if passwordPolicies.lowerCase??><@liveRule name="lowerCase" value=passwordPolicies.lowerCase text=msg("vvPasswordRuleLowerCase", passwordPolicies.lowerCase?c) /></#if>
                    <#if passwordPolicies.upperCase??><@liveRule name="upperCase" value=passwordPolicies.upperCase text=msg("vvPasswordRuleUpperCase", passwordPolicies.upperCase?c) /></#if>
                    <#if passwordPolicies.digits??><@liveRule name="digits" value=passwordPolicies.digits text=msg("vvPasswordRuleDigits", passwordPolicies.digits?c) /></#if>
                    <#if passwordPolicies.specialChars??><@liveRule name="specialChars" value=passwordPolicies.specialChars text=msg("vvPasswordRuleSpecialChars", passwordPolicies.specialChars?c) /></#if>
                    <#if passwordPolicies.notUsername><@serverRule text=msg("vvPasswordRuleNotUsername") /></#if>
                    <#if passwordPolicies.notEmail><@serverRule text=msg("vvPasswordRuleNotEmail") /></#if>
                    <#if passwordPolicies.passwordHistory??><@serverRule text=msg("vvPasswordRuleHistory", passwordPolicies.passwordHistory?c) /></#if>
                </ul>
                <p class="vv-sr" id="vv-pw-live" aria-live="polite"></p>
            </div>

            <@field.password name="password-confirm" label=msg("passwordConfirm") autocomplete="new-password" />
            <p class="vv-match" id="vv-pw-match" aria-live="polite" data-yes="${msg('vvPasswordMatch')}" data-no="${msg('vvPasswordNoMatch')}"></p>

            <div class="${properties.kcFormGroupClass!}">
                <@passwordCommons.logoutOtherSessions/>
            </div>

            <@buttons.actionGroup horizontal=true>
                <#if isAppInitiatedAction??>
                    <@buttons.button id="kc-submit" name="login" label="doSubmit"/>
                    <@buttons.button id="kc-cancel" label="doCancel" name="cancel-aia" type="secondary"/>
                <#else>
                    <@buttons.button id="kc-submit" name="login" label="doSubmit"/>
                </#if>
            </@buttons.actionGroup>
        </form>
    </#if>
</@layout.registrationLayout>
