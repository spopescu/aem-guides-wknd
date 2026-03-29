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
package com.adobe.aem.guides.wknd.core.models;

import java.util.Map;

/**
 * Represents a Content Fragment's fields for use in Experience Fragments.
 * This model extracts the Content Fragment ID from the request selector
 * and provides access to all the fragment's fields as a Map.
 */
public interface ContentFragmentFields {

    /**
     * @return a Map containing all the Content Fragment fields and their values,
     *         or an empty Map if no Content Fragment is found or an error occurs.
     */
    Map<String, Object> getFields();

    /**
     * @return a JSON string representation of the Content Fragment fields,
     *         suitable for use in JavaScript.
     */
    String getFieldsAsJson();

    /**
     * @return the path to the Content Fragment, or null if not found.
     */
    String getFragmentPath();

    /**
     * @return true if the model has valid Content Fragment data, false otherwise.
     */
    boolean isEmpty();
}

