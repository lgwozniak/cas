package org.apereo.cas.pm.web.flow.actions;

import org.apereo.cas.CentralAuthenticationService;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.pm.BasePasswordManagementService;
import org.apereo.cas.pm.PasswordManagementService;
import org.apereo.cas.pm.web.flow.PasswordManagementWebflowUtils;
import org.apereo.cas.ticket.TransientSessionTicket;
import org.apereo.cas.web.support.WebUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.webflow.action.AbstractAction;
import org.springframework.webflow.action.EventFactorySupport;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

/**
 * This is {@link VerifyPasswordResetRequestAction}.
 *
 * @author Misagh Moayyed
 * @since 5.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class VerifyPasswordResetRequestAction extends AbstractAction {
    private final CasConfigurationProperties casProperties;

    private final PasswordManagementService passwordManagementService;

    private final CentralAuthenticationService centralAuthenticationService;

    @Override
    protected Event doExecute(final RequestContext requestContext) {
        val request = WebUtils.getHttpServletRequestFromExternalWebflowContext(requestContext);
        val transientTicket = request.getParameter(PasswordManagementWebflowUtils.REQUEST_PARAMETER_NAME_PASSWORD_RESET_TOKEN);

        if (StringUtils.isBlank(transientTicket)) {
            LOGGER.error("Password reset token is missing");
            return error();
        }

        val tst = getTicket(transientTicket);
        if (tst == null) {
            LOGGER.error("Unable to locate token [{}] in the ticket registry", transientTicket);
            return error();
        }

        val token = tst.getProperties().get(PasswordManagementWebflowUtils.FLOWSCOPE_PARAMETER_NAME_TOKEN).toString();
        val username = passwordManagementService.parseToken(token);
        if (StringUtils.isBlank(username)) {
            LOGGER.error("Password reset token could not be verified");
            return error();
        }
        this.centralAuthenticationService.deleteTicket(tst.getId());

        PasswordManagementWebflowUtils.putPasswordResetToken(requestContext, token);
        val pm = casProperties.getAuthn().getPm();
        if (pm.getReset().isSecurityQuestionsEnabled()) {
            val questions = BasePasswordManagementService
                .canonicalizeSecurityQuestions(passwordManagementService.getSecurityQuestions(username));
            if (questions.isEmpty()) {
                LOGGER.warn("No security questions could be found for [{}]", username);
                return error();
            }
            PasswordManagementWebflowUtils.putPasswordResetSecurityQuestions(requestContext, questions);
        } else {
            LOGGER.debug("Security questions are not enabled");
        }

        PasswordManagementWebflowUtils.putPasswordResetUsername(requestContext, username);
        PasswordManagementWebflowUtils.putPasswordResetSecurityQuestionsEnabled(requestContext, pm.getReset().isSecurityQuestionsEnabled());

        if (pm.getReset().isSecurityQuestionsEnabled()) {
            return success();
        }
        return new EventFactorySupport().event(this, "questionsDisabled");
    }

    private TransientSessionTicket getTicket(final String transientTicket) {
        try {
            return this.centralAuthenticationService.getTicket(transientTicket, TransientSessionTicket.class);
        } catch(final InvalidTicketException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.error(e.getMessage(), e);
            } else {
                LOGGER.error(e.getMessage());
            }
        }

        return null;
    }
}
