# Content Fragment Experience Fragment Implementation

## Overview

This implementation provides a dynamic Experience Fragment system that can display any Content Fragment based on a UUID selector in the URL. The system uses a generic marker replacement approach with `[[cfFields.*]]` syntax to populate component content dynamically with Content Fragment data.

## Architecture

### Components

1. **Sling Model Interface**: `ContentFragmentFields.java`
   - Defines the contract for accessing Content Fragment data
   - Located: `core/src/main/java/com/adobe/aem/guides/wknd/core/models/`

2. **Sling Model Implementation**: `ContentFragmentFieldsImpl.java`
   - Extracts CF UUID from URL selector
   - Finds Content Fragment using `Session.getNodeByIdentifier(uuid)`
   - Extracts all fields and serializes to JSON using Jackson ObjectMapper
   - Located: `core/src/main/java/com/adobe/aem/guides/wknd/core/models/impl/`

3. **HTL Template**: `body.html`
   - Outputs CF fields as JSON in a `<script>` tag
   - Includes JavaScript for generic marker replacement
   - Located: `ui.apps/src/main/content/jcr_root/apps/wknd/components/xfcfpage/`

4. **JavaScript Marker Replacement**:
   - Generic function that replaces `[[cfFields.*]]` markers with actual CF data
   - Works in text nodes and HTML attributes
   - Handles all CF fields automatically without hardcoding
   - Embedded in `body.html`

5. **Experience Fragment**:
   - Path: `/content/experience-fragments/wknd/us/en/adventures/CFCard/master`
   - Resource Type: `wknd/components/xfcfpage`
   - Contains components with `[[cfFields.*]]` markers

## Usage

### URL Format

Access the experience fragment with a selector containing the Content Fragment UUID:

```
/content/experience-fragments/wknd/us/en/adventures/CFCard/master.{uuid}.html
```

### Examples

To get the UUID of a Content Fragment:
1. Open the CF in AEM
2. Check the `jcr:uuid` property in CRXDE
3. Or use the CF's UUID from the DAM

Example URL with UUID:
```
/content/experience-fragments/wknd/us/en/adventures/CFCard/master.eb0175d3-b011-421b-b1be-0ef5b2b64518.html
```

### JavaScript Access

The Content Fragment fields are available in the `cfFields` JavaScript object:

```javascript
// Access CF fields
console.log(cfFields.activity);      // e.g., "Cycling"
console.log(cfFields.adventureType); // e.g., "Overnight Trip"
console.log(cfFields.tripLength);    // e.g., "5 Days"
console.log(cfFields.groupSize);     // e.g., "12-14"
console.log(cfFields.difficulty);    // e.g., "Advanced"
console.log(cfFields.price);         // e.g., "$3000"
```

## Marker Replacement System

### How It Works

The system uses a generic marker replacement approach:

1. **Authors add markers** in component content using `[[cfFields.fieldName]]` syntax
2. **JavaScript scans** all text nodes and attributes in the DOM
3. **Markers are replaced** with actual CF field values automatically
4. **No hardcoding** needed - works with any CF field

### Marker Syntax

Use `[[cfFields.fieldName]]` where `fieldName` is any Content Fragment field:

```
[[cfFields.activity]]
[[cfFields.price]]
[[cfFields.adventureTitle]]
[[cfFields.description]]
[[cfFields.tripLength]]
[[cfFields.difficulty]]
```

### Example Component Configurations

**Text Component:**
```xml
<text
    sling:resourceType="wknd/components/text"
    text="&lt;p&gt;&lt;strong&gt;Activity:&lt;/strong&gt; [[cfFields.activity]]&lt;/p&gt;"/>
```

**Title Component:**
```xml
<title
    jcr:title="[[cfFields.adventureTitle]]"
    sling:resourceType="wknd/components/title"
    type="h2"/>
```

**Image Component:**
```xml
<image
    alt="[[cfFields.adventureTitle]]"
    sling:resourceType="wknd/components/image"/>
```

### Special Formatting

The JavaScript automatically formats certain fields:
- **Price**: Adds `$` prefix (e.g., `250` → `$250`)
- **Images**: Automatically populates `src` from `cfFields.primaryImage`

### UUID Lookup

The Sling Model uses JCR's `Session.getNodeByIdentifier(uuid)` to find Content Fragments:
- Direct UUID lookup (no query needed)
- Works with any CF regardless of location
- More efficient than path-based searches

## Extending the Implementation

### Adding New Fields

To add new CF fields to your Experience Fragment:

1. **Add markers in component content**:
   ```xml
   <text text="&lt;p&gt;New Field: [[cfFields.newFieldName]]&lt;/p&gt;"/>
   ```

2. **No code changes needed** - the JavaScript automatically handles all fields!

### Adding Custom Formatting

To add custom formatting for specific fields, edit `body.html`:

```javascript
// In the replaceMarkers function
if (fieldName === 'price') {
    fieldValue = '$' + fieldValue;
} else if (fieldName === 'date') {
    fieldValue = formatDate(fieldValue); // Add your custom formatter
}
```

### Creating New Experience Fragments

1. **Create XF** with resource type `wknd/components/xfcfpage`
2. **Add components** (text, title, image, etc.)
3. **Use markers** like `[[cfFields.fieldName]]` in component content
4. **Access via URL** with UUID selector

## Development

### Building the Project

```bash
# Build all modules
mvn clean install -PautoInstallSinglePackage

# Build individual modules
cd core && mvn clean install -PautoInstallBundle
cd ui.apps && mvn clean install -PautoInstallPackage
cd ui.content.sample && mvn clean install -PautoInstallPackage
```

### Testing

1. **Deploy the code** to your AEM instance
2. **Get a CF UUID**:
   - Open any Content Fragment in AEM
   - In CRXDE, check the `jcr:uuid` property
3. **Access the XF** with UUID selector:
   ```
   http://localhost:4502/content/experience-fragments/wknd/us/en/adventures/CFCard/master.{uuid}.html
   ```
4. **Verify in browser console**:
   ```javascript
   console.log(cfFields); // Should show all CF fields
   ```
5. **Check marker replacement**:
   - View page source - should see markers like `[[cfFields.activity]]`
   - View rendered page - markers should be replaced with actual values

## Troubleshooting

### CF Not Found

If the Content Fragment is not loading:
- **Verify UUID**: Check the `jcr:uuid` property in CRXDE
- **Check logs**: Look for errors from `ContentFragmentFieldsImpl` in AEM logs
- **Test UUID lookup**: Try accessing the CF directly via `/jcr:uuid/{uuid}` in CRXDE

### Markers Not Replaced

If `[[cfFields.*]]` markers are still visible:
- **Check browser console**: Look for JavaScript errors
- **Verify cfFields object**: Run `console.log(cfFields)` in browser console
- **Check root container**: The JavaScript logs which container it's using
- **Verify field names**: Make sure marker field names match CF field names exactly

### Root Container Not Found

If you see "Root container not found" in console:
- The JavaScript tries multiple selectors: `.root.responsivegrid`, `.root`, `main`, `document.body`
- Check the console log to see which container is being used
- The fallback to `document.body` should always work

### JSON Serialization Issues

If CF fields aren't serializing correctly:
- Check AEM logs for Jackson serialization errors
- Verify the CF fields are valid data types
- The ObjectMapper handles most types automatically

## Technical Details

### UUID Lookup Implementation

The implementation uses JCR's `Session.getNodeByIdentifier(uuid)` for efficient CF lookup:

```java
Session session = resourceResolver.adaptTo(Session.class);
Node node = session.getNodeByIdentifier(uuid);
String nodePath = node.getPath();
Resource cfResource = resourceResolver.getResource(nodePath);
ContentFragment cf = cfResource.adaptTo(ContentFragment.class);
```

**Benefits:**
- Direct UUID lookup (no query needed)
- Works regardless of CF location in DAM
- More efficient than SQL queries
- Standard JCR API method

### JSON Serialization

Uses Jackson ObjectMapper for robust JSON serialization:

```java
private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

public String getFieldsAsJson() {
    return OBJECT_MAPPER.writeValueAsString(fields);
}
```

**Benefits:**
- Handles all data types automatically
- Proper JSON escaping
- Industry-standard library
- Better performance than manual string building

### Marker Replacement Algorithm

The JavaScript uses regex to find and replace markers:

```javascript
var regex = /\[\[cfFields\.(\w+)\]\]/g;
// Matches: [[cfFields.fieldName]]
// Captures: fieldName
```

**Process:**
1. Walks through all text nodes using `TreeWalker`
2. Scans all element attributes
3. Replaces markers with CF field values
4. Applies special formatting (e.g., price → $price)

## Files Modified/Created

### Core (Java)
- `core/src/main/java/com/adobe/aem/guides/wknd/core/models/ContentFragmentFields.java` (new)
- `core/src/main/java/com/adobe/aem/guides/wknd/core/models/impl/ContentFragmentFieldsImpl.java` (new)

### UI Apps (HTL & JavaScript)
- `ui.apps/src/main/content/jcr_root/apps/wknd/components/xfcfpage/.content.xml` (new)
- `ui.apps/src/main/content/jcr_root/apps/wknd/components/xfcfpage/_cq_dialog/.content.xml` (new)
- `ui.apps/src/main/content/jcr_root/apps/wknd/components/xfcfpage/body.html` (new)
  - Contains CF data output as JSON
  - Contains generic marker replacement JavaScript

### UI Content (Experience Fragment)
- `ui.content.sample/src/main/content/jcr_root/content/experience-fragments/wknd/us/en/adventures/CFCard/.content.xml` (new)
- `ui.content.sample/src/main/content/jcr_root/content/experience-fragments/wknd/us/en/adventures/CFCard/master/.content.xml` (new)
  - Contains components with `[[cfFields.*]]` markers

## Key Features

✅ **Generic Marker System** - Works with any CF field without code changes
✅ **UUID-based Lookup** - Efficient direct lookup using JCR API
✅ **Jackson Serialization** - Robust JSON handling
✅ **Automatic Formatting** - Special handling for price, images, etc.
✅ **Flexible Container Detection** - Works with various DOM structures
✅ **JCR-safe Markers** - `[[...]]` syntax avoids validation issues
✅ **No Hardcoding** - Add new fields by just using markers

