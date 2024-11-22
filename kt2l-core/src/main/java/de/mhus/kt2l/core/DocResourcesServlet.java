package de.mhus.kt2l.core;

import de.mhus.commons.tools.MFile;
import jakarta.servlet.Servlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/docs")
public class DocResourcesServlet {

    @GetMapping("/**")
    public void proxy(final HttpServletRequest request, final HttpServletResponse response) {
        final var path =  request.getServletPath().replaceFirst("/docs/*", "/");
        final var resource = Thread.currentThread().getContextClassLoader().getResourceAsStream("docs" + path);
        if (resource == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        try (final var outputStream = response.getOutputStream()) {
            response.setContentType("text/html");
            response.setStatus(HttpServletResponse.SC_OK);
            StreamUtils.copy(resource, outputStream);
        } catch (Exception e) {
            LOGGER.error("Error reading resource {}", path, e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}