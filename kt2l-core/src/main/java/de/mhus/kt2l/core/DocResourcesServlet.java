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
package de.mhus.kt2l.core;

import de.mhus.commons.tools.MFile;
import de.mhus.commons.tools.MLang;
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
            response.setContentType("text/plain");
            MLang.tryThis(() -> response.getWriter().write("Resource not found"));
            return;
        }
        try (final var outputStream = response.getOutputStream()) {
            response.setContentType("text/html");
            response.setStatus(HttpServletResponse.SC_OK);
            StreamUtils.copy(resource, outputStream);
        } catch (Exception e) {
            LOGGER.error("Error reading resource {}", path, e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("text/plain");
            MLang.tryThis(() -> response.getWriter().write("Resource error"));
        }
    }
}