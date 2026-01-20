/*
 *  Copyright 2026 Adobe Systems Incorporated
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.adobe.aem.guides.wknd.core.models.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.jcr.Node;
import javax.jcr.Session;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.aem.guides.wknd.core.models.ContentFragmentFields;
import com.adobe.cq.dam.cfm.ContentElement;
import com.adobe.cq.dam.cfm.ContentFragment;
import com.adobe.cq.dam.cfm.FragmentData;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Implementation of ContentFragmentFields that extracts Content Fragment data
 * based on a selector in the request URL.
 *
 * Expected URL format: /path/to/xf/master.{uuid}.html
 * where {uuid} is the UUID of the Content Fragment.
 */
@Model(
        adaptables = {SlingHttpServletRequest.class},
        adapters = {ContentFragmentFields.class},
        resourceType = {ContentFragmentFieldsImpl.RESOURCE_TYPE},
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class ContentFragmentFieldsImpl implements ContentFragmentFields {

    protected static final String RESOURCE_TYPE = "wknd/components/xfcfpage";

    private static final Logger LOGGER = LoggerFactory.getLogger(ContentFragmentFieldsImpl.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Self
    private SlingHttpServletRequest request;

    @SlingObject
    private ResourceResolver resourceResolver;

    private Map<String, Object> fields;
    private String fragmentPath;

    @PostConstruct
    protected void init() {
        fields = new HashMap<>();

        try {
            // Extract the CF UUID from the selector
            String cfUuid = extractContentFragmentId();

            if (StringUtils.isNotBlank(cfUuid)) {
                // Try to find the Content Fragment by UUID
                ContentFragment contentFragment = findContentFragment(cfUuid);

                if (contentFragment != null) {
                    fragmentPath = contentFragment.adaptTo(Resource.class).getPath();
                    extractFields(contentFragment);
                } else {
                    LOGGER.warn("Content Fragment not found for UUID: {}", cfUuid);
                }
            } else {
                LOGGER.debug("No Content Fragment UUID found in selectors");
            }
        } catch (Exception e) {
            LOGGER.error("Error initializing ContentFragmentFields", e);
        }
    }

    /**
     * Extracts the Content Fragment UUID from the request selectors.
     * Expects the CF UUID to be in the first selector position.
     */
    private String extractContentFragmentId() {
        RequestPathInfo pathInfo = request.getRequestPathInfo();
        String[] selectors = pathInfo.getSelectors();

        if (selectors != null && selectors.length > 0) {
            // Return the first selector as the CF UUID
            return selectors[0];
        }

        return null;
    }

    /**
     * Attempts to find a Content Fragment by UUID using JCR Session.getNodeByIdentifier().
     * The uuid parameter is expected to be a UUID string.
     */
    private ContentFragment findContentFragment(String uuid) {
        try {
            Session session = resourceResolver.adaptTo(Session.class);
            if (session == null) {
                LOGGER.error("Unable to get JCR Session from ResourceResolver");
                return null;
            }

            LOGGER.debug("Looking up Content Fragment by UUID: {}", uuid);

            // Use JCR's getNodeByIdentifier to get the node directly by UUID
            Node node = session.getNodeByIdentifier(uuid);
            String nodePath = node.getPath();

            LOGGER.debug("Found node at path: {}", nodePath);

            // Get the resource from the path
            Resource cfResource = resourceResolver.getResource(nodePath);

            if (cfResource != null) {
                ContentFragment cf = cfResource.adaptTo(ContentFragment.class);
                if (cf != null) {
                    return cf;
                } else {
                    LOGGER.warn("Resource at {} is not a Content Fragment", nodePath);
                }
            } else {
                LOGGER.warn("Could not get resource for path: {}", nodePath);
            }
        } catch (javax.jcr.ItemNotFoundException e) {
            LOGGER.warn("No node found with UUID: {}", uuid);
        } catch (Exception e) {
            LOGGER.error("Error finding Content Fragment by UUID: {}", uuid, e);
        }

        return null;
    }

    /**
     * Extracts all fields from the Content Fragment.
     */
    private void extractFields(ContentFragment contentFragment) {
        try {
            Iterator<ContentElement> elements = contentFragment.getElements();

            while (elements.hasNext()) {
                ContentElement element = elements.next();
                String elementName = element.getName();
                FragmentData fragmentData = element.getValue();

                if (fragmentData != null) {
                    Object value = fragmentData.getValue();
                    fields.put(elementName, value);
                }
            }

            LOGGER.debug("Extracted {} fields from Content Fragment", fields.size());
        } catch (Exception e) {
            LOGGER.error("Error extracting fields from Content Fragment", e);
        }
    }

    @Override
    public Map<String, Object> getFields() {
        return Collections.unmodifiableMap(fields);
    }

    @Override
    public String getFieldsAsJson() {
        if (fields.isEmpty()) {
            return "{}";
        }

        try {
            return OBJECT_MAPPER.writeValueAsString(fields);
        } catch (Exception e) {
            LOGGER.error("Error serializing fields to JSON using Jackson", e);
            return "{}";
        }
    }

    @Override
    public String getFragmentPath() {
        return fragmentPath;
    }

    @Override
    public boolean isEmpty() {
        return fields.isEmpty();
    }
}

