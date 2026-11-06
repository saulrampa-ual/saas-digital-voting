<#-- VeriVoto (SR13, approved prototype): email code page of email-otp-authenticator 1.8.1, overriding its
     theme-resources/templates/login-email-otp.ftl. Same form as the original: POST to url.loginAction with the fields
     "email-otp" (the code), "login" (submit), "resend-email" (resend) and, when device trust is on, "trust-device";
     the authentication flow is not changed.
     The code is ONE real input (autocomplete one-time-code, inputmode numeric), so paste, Backspace and autofill are the
     browser's own. js/verivoto.js only draws it as six boxes, enables «Continuar» at six digits and shows the resend
     countdown; without JavaScript the plain field and both buttons still work. Six boxes = realm config code-length 6. -->
<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('email-otp'); section>
<!-- template: login-email-otp.ftl -->
    <#if section = "header">
        ${msg("vvOtpTitle")}
    <#elseif section = "form">
        <#assign otpError = messagesPerField.existsError('email-otp')>
        <p class="vv-otp-intro" id="vv-otp-intro">
            <#if maskedEmail?has_content>${kcSanitize(msg("vvOtpSentTo", maskedEmail))?no_esc}<#else>${msg("vvOtpSent")}</#if>
        </p>
        <form id="kc-otp-login-form" class="${properties.kcFormClass!} vv-otp-form" onsubmit="login.disabled = true; return true;" action="${url.loginAction}" method="post"
              data-resend-wait="${properties.vvOtpResendSeconds!'0'}" data-start-wait="${(!otpError && !(message?has_content && message.type = 'error'))?c}">
            <div class="${properties.kcFormGroupClass!}">
                <label for="email-otp" class="${properties.kcLabelClass!} vv-otp-label">${msg("vvOtpLabel")}</label>
                <div class="vv-otp-field">
                    <input id="email-otp" name="email-otp" class="${properties.kcInputClass!} vv-otp-input" type="text"
                           autocomplete="one-time-code" inputmode="numeric" pattern="[0-9]*" maxlength="6" spellcheck="false" autofocus
                           aria-describedby="vv-otp-intro<#if otpError> input-error-email-otp-code</#if>"
                           aria-invalid="<#if otpError>true<#else>false</#if>" dir="ltr" />
                </div>
                <#if otpError>
                    <span id="input-error-email-otp-code" class="${properties.kcInputErrorMessageClass!} vv-otp-error" aria-live="polite">
                        ${kcSanitize(messagesPerField.get('email-otp'))?no_esc}
                    </span>
                </#if>
            </div>

            <#if deviceTrustEnabled?? && deviceTrustEnabled>
            <div class="${properties.kcFormGroupClass!}">
                <div class="${properties.kcInputWrapperClass!}">
                    <div class="${properties.kcCheckboxInputClass!}">
                        <input type="checkbox" id="trust-device" name="trust-device" value="true" />
                        <label for="trust-device" class="${properties.kcCheckboxLabelClass!}">
                            <#if deviceTrustPermanent?? && deviceTrustPermanent>
                                ${msg("dontAskForCodePermanently")}
                            <#elseif trustDurationUnitKey??>
                                <#assign unitStr = msg(trustDurationUnitKey)>
                                <#if trustHideNumber?? && trustHideNumber>
                                    ${msg("dontAskForCodeFor", "", unitStr)?trim}
                                <#else>
                                    ${msg("dontAskForCodeFor", trustDurationValue!1, unitStr)}
                                </#if>
                            <#else>
                                ${msg("dontAskForCodePermanently")}
                            </#if>
                        </label>
                    </div>
                </div>
            </div>
            </#if>

            <div class="${properties.kcFormGroupClass!}">
                <div id="kc-form-buttons" class="${properties.kcFormButtonsClass!}">
                    <div class="${properties.kcFormButtonsWrapperClass!}">
                        <button class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}"
                                name="login" id="kc-login" type="submit">${kcSanitize(msg("vvOtpContinue"))?no_esc}</button>
                        <button class="${properties.kcButtonClass!} ${properties.kcButtonSecondaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}"
                                name="resend-email" id="kc-resend-email" type="submit" formnovalidate
                                aria-describedby="vv-otp-wait">${kcSanitize(msg("vvOtpResend"))?no_esc}</button>
                        <p class="vv-otp-wait" id="vv-otp-wait" aria-live="polite" data-template="${msg('vvOtpWait')}"></p>
                    </div>
                </div>
            </div>
        </form>
        <#if auth?has_content && auth.attemptedUsername?has_content>
            <div class="vv-otp-account">
                <span class="vv-otp-user">${auth.attemptedUsername}</span>
                <a id="vv-change-account" href="${url.loginRestartFlowUrl}">${msg("vvOtpChangeAccount")}</a>
            </div>
        </#if>
    </#if>
</@layout.registrationLayout>
