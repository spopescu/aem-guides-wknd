/*
 * Copyright 2024 Adobe. All rights reserved.
 */
package com.adobe.aem.guides.wknd.core.filters;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.WriteListener;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.wrappers.SlingHttpServletResponseWrapper;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.aem.guides.wknd.core.models.ContentFragmentFields;

/**
 * Sling Filter that replaces Content Fragment field placeholders in HTML output.
 *
 * Replaces markers like ([cfFields.fieldName]) with actual CF field values.
 * Only processes requests for resources with sling:resourceType = wknd/components/xfcfpage
 */
@Component(
    service = Filter.class,
    property = {
        "sling.filter.scope=REQUEST",
        "service.ranking:Integer=1000"
    }
)
public class ContentFragmentPlaceholderFilter implements Filter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContentFragmentPlaceholderFilter.class);

    // Pattern to match ([cfFields.fieldName])
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\(\\[cfFields\\.(\\w+)\\]\\)");

    private static final String RESOURCE_TYPE = "wknd/components/xfcfpage";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        LOGGER.info("ContentFragmentPlaceholderFilter initialized");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (!(request instanceof SlingHttpServletRequest) || !(response instanceof SlingHttpServletResponse)) {
            chain.doFilter(request, response);
            return;
        }

        SlingHttpServletRequest slingRequest = (SlingHttpServletRequest) request;
        SlingHttpServletResponse slingResponse = (SlingHttpServletResponse) response;

        // Check if this is a page with xfcfpage resource type
        Resource resource = slingRequest.getResource();
        if (resource == null || !shouldProcessResource(resource)) {
            chain.doFilter(request, response);
            return;
        }

        LOGGER.debug("Processing CF placeholder replacement for: {}", resource.getPath());

        // Wrap response to capture output
        BufferedResponseWrapper wrappedResponse = new BufferedResponseWrapper(slingResponse);

        // Continue filter chain
        chain.doFilter(request, wrappedResponse);

        // Get the captured output
        String output = wrappedResponse.getCapturedOutput();

        if (output != null && !output.isEmpty()) {
            // Get the correct resource to adapt (jcr:content for pages)
            Resource adaptResource = resource;
            if ("cq:Page".equals(resource.getResourceType())) {
                Resource contentResource = resource.getChild("jcr:content");
                if (contentResource != null) {
                    adaptResource = contentResource;
                }
            }

            // Get CF fields from Sling Model
            ContentFragmentFields cfFields = slingRequest.adaptTo(ContentFragmentFields.class);

            if (cfFields != null && cfFields.getFields() != null) {
                // Replace placeholders
                output = replacePlaceholders(output, cfFields.getFields());
                LOGGER.debug("Replaced CF placeholders in output for: {}", resource.getPath());
            } else {
                LOGGER.debug("No CF fields available for: {}", resource.getPath());
            }

            // Write modified output
            response.setContentLength(output.getBytes(StandardCharsets.UTF_8).length);
            response.getWriter().write(output);
        }
    }

    @Override
    public void destroy() {
        LOGGER.info("ContentFragmentPlaceholderFilter destroyed");
    }

    /**
     * Check if the resource should be processed by this filter.
     * For cq:Page resources, check the jcr:content node's sling:resourceType.
     * For other resources, check the resource type directly.
     */
    private boolean shouldProcessResource(Resource resource) {
        // Check if it's a cq:Page
        if ("cq:Page".equals(resource.getResourceType())) {
            // For pages, check the jcr:content node
            Resource contentResource = resource.getChild("jcr:content");
            if (contentResource != null) {
                String contentResourceType = contentResource.getResourceType();
                LOGGER.debug("Page content resource type: {}", contentResourceType);
                return RESOURCE_TYPE.equals(contentResourceType);
            }
            return false;
        }

        // For non-page resources, check directly
        return RESOURCE_TYPE.equals(resource.getResourceType());
    }

    /**
     * Replace all ([cfFields.fieldName]) placeholders with actual field values
     */
    private String replacePlaceholders(String content, Map<String, Object> fields) {
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(content);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String fieldName = matcher.group(1);
            Object fieldValue = fields.get(fieldName);

            if (fieldValue != null) {
                // Escape special regex characters in replacement
                String replacement = Matcher.quoteReplacement(fieldValue.toString());
                matcher.appendReplacement(result, replacement);
            } else {
                // Keep original placeholder if field not found
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group(0)));
            }
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * Response wrapper that captures the output for processing
     */
    private static class BufferedResponseWrapper extends SlingHttpServletResponseWrapper {

        private ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        private PrintWriter writer;
        private ServletOutputStream servletOutputStream;

        public BufferedResponseWrapper(SlingHttpServletResponse response) {
            super(response);
        }

        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            if (servletOutputStream == null) {
                servletOutputStream = new ServletOutputStream() {
                    @Override
                    public void write(int b) throws IOException {
                        outputStream.write(b);
                    }

                    @Override
                    public boolean isReady() {
                        return true;
                    }

                    @Override
                    public void setWriteListener(WriteListener writeListener) {
                        // Not implemented
                    }
                };
            }
            return servletOutputStream;
        }

        @Override
        public PrintWriter getWriter() throws IOException {
            if (writer == null) {
                writer = new PrintWriter(outputStream, true, StandardCharsets.UTF_8);
            }
            return writer;
        }

        public String getCapturedOutput() {
            if (writer != null) {
                writer.flush();
            }
            return outputStream.toString(StandardCharsets.UTF_8);
        }
    }
}

