/*
 * kt2l-core - kt2l core implementation
 * Copyright © 2024 Mike Hummel (mh@mhus.de)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.mhus.kt2l.aaa;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.icon.SvgIcon;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.dom.ElementAttachEvent;
import com.vaadin.flow.dom.ElementAttachListener;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.Theme;
import de.mhus.commons.net.MNet;
import de.mhus.kt2l.aaa.oauth2.AuthProvider;
import de.mhus.kt2l.aaa.oauth2.OAuth2AuthProvider;
import de.mhus.kt2l.ui.UiUtil;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static de.mhus.commons.tools.MString.isSet;

@Route("login")
@PageTitle("Login")
@AnonymousAllowed
@Slf4j
public class LoginView extends VerticalLayout implements BeforeEnterObserver {

    private LoginForm login = new LoginForm();

    @Autowired
    private LoginConfiguration loginConfig;
    @Autowired
    private AuthProvider authProvider;

    @PostConstruct
    public void init() {
        addClassName("login-view");
        setSizeFull();

        setJustifyContentMode(JustifyContentMode.CENTER);
        setAlignItems(Alignment.CENTER);

        if (loginConfig.isShowLoginHeader()) {
            var icon = new SvgIcon(new StreamResource("logo.svg", () -> LoginView.class.getResourceAsStream("/images/kt2l-logo.svg")));
            add(new H1(icon, new Div("KT2L")));
        }
        var loginText = loginConfig.getLoginText();
        if (isSet(loginText)) {
            add(new Html("<div class='login-text'>" + loginText + "</div>"));
        }
        if (loginConfig.isLocalAuthEnabled()) {
            login.setForgotPasswordButtonVisible(false);
            login.setAction("login");
            add(login);
        }
//        if ("false".equals(UI.getCurrent().getSession().getAttribute("autologin"))) {
//            UI.getCurrent().getSession().close();
//            UI.getCurrent().getPage().setLocation("/reset");
//            VaadinServletRequest request = VaadinServletRequest.getCurrent();
//            for (var cookie : request.getCookies()) {
//                cookie.setMaxAge(0);
//                cookie.setValue(null);
//                cookie.setPath("/");
//                VaadinService.getCurrentResponse().addCookie(cookie);
//            }
//        }

        if (loginConfig.isOAuth2Enabled()) {
            for (var provider : authProvider.getAuthProviders()) {
                var logo = getLogo(provider);
                var text = new Div("Login with " + provider.getTitle());
                Anchor loginLink = new Anchor(authProvider.getProividerLoginUrl(provider), logo, text);
                loginLink.addClassName("login-link");
                // Instruct Vaadin Router to ignore doing SPA handling
                loginLink.setRouterIgnore(true);
                var loginDiv = new Div(loginLink);
                loginDiv.addClassName("login-div");
                add(loginDiv);
            }
        }
    }

    private SvgIcon getLogo(OAuth2AuthProvider provider) {
        var path = provider.getImageResourcePath() == null ? "/images/saml-logo.svg" : provider.getImageResourcePath();
        StreamResource iconResource = new StreamResource(provider.getRegistrationId() + "-logo.svg",
                () -> LoginView.class.getResourceAsStream(path));
        SvgIcon icon = new SvgIcon(iconResource);
        return icon;
    }

    public static HttpServletRequest getCurrentHttpRequest(){
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes) {
            HttpServletRequest request = ((ServletRequestAttributes)requestAttributes).getRequest();
            return request;
        }
        return null;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        var req = getCurrentHttpRequest();
        var rhost = req.getRemoteHost();

        var sessionAutoLogin = UI.getCurrent().getSession().getAttribute("autologin");
        if (loginConfig.isAutoLogin() /* && !"false".equals(sessionAutoLogin ) */) {
            if (!loginConfig.isAutoLoginLocalhostOnly() || MNet.isLocalhost(rhost)) {
                LOGGER.info("Do auto login for {}",loginConfig.getAutoLoginUser());
                try {
                    req.login(loginConfig.getAutoLoginUser(), loginConfig.getLocalAutoLoginPassword());
                } catch (ServletException e) {
                    if (e.getMessage().contains("already authenticated")) {
                        LOGGER.info("Already authenticated");
                    } else {
                        LOGGER.warn("Autologin failed for {}", loginConfig.getAutoLoginUser(), e);
                    }
                }
                UI.getCurrent().navigate("/");
            }
        }

    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        if(UI.getCurrent() != null){
            LOGGER.debug("!!!!!!!!!!!!!!! onAttach");
//            UI.getCurrent().getElement().getStyle().set("background", "url(./frontend/images/login-background.jpg) no-repeat center center fixed");
            UI.getCurrent().getElement().getClassList().add("login-page");
        }
        if(beforeEnterEvent.getLocation()
                .getQueryParameters()
                .getParameters()
                .containsKey("error")) {
            login.setError(true);
        }
    }

}