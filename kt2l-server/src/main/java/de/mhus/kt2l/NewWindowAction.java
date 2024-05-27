package de.mhus.kt2l;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServletRequest;
import de.mhus.kt2l.core.Core;
import de.mhus.kt2l.core.CoreAction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class NewWindowAction implements CoreAction  {

    @Override
    public boolean canHandle(Core core) {
        return true;
    }

    @Override
    public String getTitle() {
        return "New Window";
    }

    @Override
    public void execute(Core core) {
        VaadinServletRequest req = (VaadinServletRequest) VaadinService.getCurrentRequest();
        StringBuffer uriString = req.getRequestURL();
        UI.getCurrent().getPage().open(uriString.toString(), "_blank");

    }

    @Override
    public Icon getIcon() {
        return VaadinIcon.PLUS.create();
    }

    @Override
    public int getPriority() {
        return 5000;
    }
}
