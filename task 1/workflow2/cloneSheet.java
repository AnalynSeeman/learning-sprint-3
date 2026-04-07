/**
     * Creates a complete clone of a worksheet with all its content, formatting, and relationships.
     * 
     * This method performs a deep copy of the specified source sheet, including:
     * <ul>
     *   <li>Sheet content and formatting</li>
     *   <li>Internal relationships (charts, images, etc.)</li>
     *   <li>External relationships and hyperlinks</li>
     *   <li>Drawing objects (with limitations - see warnings below)</li>
     * </ul>
     * 
     * <strong>Known Limitations:</strong>
     * <ul>
     *   <li>Comments/legacy drawing objects are not supported and will be removed</li>
     *   <li>Page setup configurations are not copied</li>
     * </ul>
     *
     * @param sheetIndex The 0-based index of the source sheet to clone
     * @param newSheetName The name for the cloned sheet, or null to auto-generate a unique name
     * @return A new XSSFSheet containing all cloned content from the source sheet
     * @throws IllegalArgumentException if the sheet index is invalid
     * @throws IllegalArgumentException if the sheet name is invalid or already exists
     * @throws POIXMLException if an I/O error occurs during the cloning process
     */
    public XSSFSheet cloneSheet(int sheetIndex, String newSheetName) {
        // Validate input parameters
        validateSheetIndex(sheetIndex);
        XSSFSheet sourceSheet = sheets.get(sheetIndex);
        
        // Determine the name for the cloned sheet
        String finalSheetName = determineClonedSheetName(sourceSheet, newSheetName);
        
        // Create a new sheet with the determined name
        XSSFSheet clonedSheet = createSheet(finalSheetName);
        
        // Preserve drawables before copying relations (needed for special drawing handling)
        XSSFDrawing sourceDrawing = extractSourceDrawing(sourceSheet);
        
        // Copy all internal relationships and content
        copyInternalRelationships(sourceSheet, clonedSheet);
        
        // Copy external relationships (hyperlinks, external data sources, etc.)
        copyExternalRelationships(sourceSheet, clonedSheet);
        
        // Clone the actual sheet content via serialization
        cloneSheetContent(sourceSheet, clonedSheet);
        
        // Handle unsupported features and remove incompatible configurations
        handleUnsupportedFeatures(clonedSheet);
        
        // Clone drawing objects and their relationships if present
        if (sourceDrawing != null) {
            cloneDrawingAndRelationships(sourceSheet, clonedSheet, sourceDrawing);
        }
        
        return clonedSheet;
    }

    /**
     * Determines the appropriate name for the cloned sheet.
     * Validates provided names or generates a unique name based on the source sheet.
     *
     * @param sourceSheet The source sheet being cloned
     * @param requestedName The requested name for the cloned sheet (may be null)
     * @return A valid sheet name for the clone
     * @throws IllegalArgumentException if the requested name is invalid
     */
    private String determineClonedSheetName(XSSFSheet sourceSheet, String requestedName) {
        if (requestedName == null) {
            // Auto-generate a unique name based on the source sheet name
            String sourceSheetName = sourceSheet.getSheetName();
            return getUniqueSheetName(sourceSheetName);
        } else {
            // Validate and use the provided name
            validateSheetName(requestedName);
            return requestedName;
        }
    }

    /**
     * Extracts the drawing object from the source sheet for later cloning.
     * The drawing relationship is handled specially and excluded from standard relationship copying.
     *
     * @param sourceSheet The source sheet to extract drawing objects from
     * @return The XSSFDrawing object if present, or null if the sheet has no drawing
     */
    private XSSFDrawing extractSourceDrawing(XSSFSheet sourceSheet) {
        XSSFDrawing sourceDrawing = null;
        List<RelationPart> relationships = sourceSheet.getRelationParts();
        
        for (RelationPart relationPart : relationships) {
            POIXMLDocumentPart documentPart = relationPart.getDocumentPart();
            if (documentPart instanceof XSSFDrawing) {
                sourceDrawing = (XSSFDrawing) documentPart;
                // Found the drawing; exit early to improve efficiency
                break;
            }
        }
        
        return sourceDrawing;
    }

    /**
     * Copies all internal relationships from the source sheet to the cloned sheet.
     * Internal relationships include charts, images, and other document parts,
     * excluding drawing relationships which are handled separately.
     *
     * @param sourceSheet The source sheet
     * @param clonedSheet The destination sheet
     */
    private void copyInternalRelationships(XSSFSheet sourceSheet, XSSFSheet clonedSheet) {
        List<RelationPart> sourceRelationships = sourceSheet.getRelationParts();
        
        for (RelationPart relationPart : sourceRelationships) {
            POIXMLDocumentPart documentPart = relationPart.getDocumentPart();
            
            // Skip drawing relationships; they are cloned separately with special handling
            if (documentPart instanceof XSSFDrawing) {
                continue;
            }
            
            // Copy all other relationships to the cloned sheet
            addRelation(relationPart, clonedSheet);
        }
    }

    /**
     * Copies external relationships from the source sheet to the cloned sheet.
     * External relationships include hyperlinks and external data sources.
     *
     * @param sourceSheet The source sheet
     * @param clonedSheet The destination sheet
     * @throws POIXMLException if an error occurs while adding external relationships
     */
    private void copyExternalRelationships(XSSFSheet sourceSheet, XSSFSheet clonedSheet) {
        try {
            for (PackageRelationship packageRelationship : sourceSheet.getPackagePart().getRelationships()) {
                // Only copy external relationships (hyperlinks, external references, etc.)
                if (packageRelationship.getTargetMode() == TargetMode.EXTERNAL) {
                    clonedSheet.getPackagePart().addExternalRelationship(
                            packageRelationship.getTargetURI().toASCIIString(),
                            packageRelationship.getRelationshipType(),
                            packageRelationship.getId()
                    );
                }
            }
        } catch (InvalidFormatException e) {
            String errorMessage = String.format(
                    "Failed to copy external relationships while cloning sheet '%s'",
                    sourceSheet.getSheetName()
            );
            throw new POIXMLException(errorMessage, e);
        }
    }

    /**
     * Clones the actual sheet content by serializing the source sheet and deserializing into the clone.
     * This approach preserves all formatting, formulas, and cell values.
     *
     * @param sourceSheet The source sheet
     * @param clonedSheet The destination sheet
     * @throws POIXMLException if an I/O error occurs during serialization/deserialization
     */
    private void cloneSheetContent(XSSFSheet sourceSheet, XSSFSheet clonedSheet) {
        try (ByteArrayOutputStream serializedContent = new ByteArrayOutputStream()) {
            // Serialize the source sheet to a byte stream
            sourceSheet.write(serializedContent);
            
            // Deserialize the content into the cloned sheet
            try (ByteArrayInputStream contentStream = new ByteArrayInputStream(serializedContent.toByteArray())) {
                clonedSheet.read(contentStream);
            }
        } catch (IOException e) {
            String errorMessage = String.format(
                    "Failed to serialize/deserialize sheet content while cloning sheet '%s'",
                    sourceSheet.getSheetName()
            );
            throw new POIXMLException(errorMessage, e);
        }
    }

    /**
     * Handles unsupported features by removing incompatible configurations.
     * Logs warnings for features that cannot be cloned properly.
     *
     * @param clonedSheet The cloned sheet to process
     */
    private void handleUnsupportedFeatures(XSSFSheet clonedSheet) {
        CTWorksheet ctWorksheet = clonedSheet.getCTWorksheet();
        
        // Remove legacy drawing (comments) - not yet supported in cloning
        if (ctWorksheet.isSetLegacyDrawing()) {
            logger.log(POILogger.WARN, 
                    "Sheet comments are not supported during cloning and will be removed.");
            ctWorksheet.unsetLegacyDrawing();
        }
        
        // Remove page setup - not yet supported in cloning
        if (ctWorksheet.isSetPageSetup()) {
            logger.log(POILogger.WARN,
                    "Page setup configurations are not supported during cloning and will be removed.");
            ctWorksheet.unsetPageSetup();
        }
        
        // Cloned sheets should not be selected by default
        clonedSheet.setSelected(false);
    }

    /**
     * Clones drawing objects and their relationships from the source sheet to the cloned sheet.
     * This includes charts, shapes, and images embedded in the drawing layer.
     *
     * @param sourceSheet The source sheet
     * @param clonedSheet The destination sheet
     * @param sourceDrawing The source drawing object to clone
     */
    private void cloneDrawingAndRelationships(XSSFSheet sourceSheet, XSSFSheet clonedSheet, XSSFDrawing sourceDrawing) {
        CTWorksheet ctClonedWorksheet = clonedSheet.getCTWorksheet();
        
        // Remove any existing drawing reference to allow clean recreation
        if (ctClonedWorksheet.isSetDrawing()) {
            ctClonedWorksheet.unsetDrawing();
        }
        
        // Create a new drawing patriarch in the cloned sheet
        XSSFDrawing clonedDrawing = clonedSheet.createDrawingPatriarch();
        
        // Copy drawing content from source to clone
        clonedDrawing.getCTDrawing().set(sourceDrawing.getCTDrawing());
        
        // Recreate the drawing to ensure proper initialization
        clonedDrawing = clonedSheet.createDrawingPatriarch();
        
        // Copy all drawing relationships (shapes, charts, images, etc.)
        XSSFDrawing sourceDrawingPatriarch = sourceSheet.createDrawingPatriarch();
        List<RelationPart> sourceDrawingRelationships = sourceDrawingPatriarch.getRelationParts();
        
        for (RelationPart drawingRelationship : sourceDrawingRelationships) {
            addRelation(drawingRelationship, clonedDrawing);
        }
    }
