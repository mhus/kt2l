package de.mhus.kt2l.core;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import de.mhus.commons.net.MNet;
import de.mhus.kt2l.config.LoginConfiguration;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Route("login")
@PageTitle("Login")
@AnonymousAllowed
@Slf4j
public class LoginView extends VerticalLayout implements BeforeEnterObserver {

    private LoginForm login = new LoginForm();

    @Autowired
    private LoginConfiguration loginConfig;

    public LoginView() {
        addClassName("login-view");
        setSizeFull();

        setJustifyContentMode(JustifyContentMode.CENTER);
        setAlignItems(Alignment.CENTER);

        login.setForgotPasswordButtonVisible(false);
        login.setAction("login");

        add(new H1("KT2L"), login);



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

        if (loginConfig.isAutoLogin()) {
            if (!loginConfig.isAutoLoginLocalhostOnly() || MNet.isLocalhost(rhost)) {
                LOGGER.info("Do auto login for {}",loginConfig.getAutoLoginUser());
                try {
                    req.login(loginConfig.getAutoLoginUser(), loginConfig.getLocalAutoLoginPassword());
                } catch (ServletException e) {
                    LOGGER.warn("Autologin failed for {}",loginConfig.getAutoLoginUser(),e);
                }
                UI.getCurrent().navigate("/");
            }
        }

    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        if(beforeEnterEvent.getLocation()
                .getQueryParameters()
                .getParameters()
                .containsKey("error")) {
            login.setError(true);
        }
    }
}