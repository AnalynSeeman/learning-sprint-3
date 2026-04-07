/**
 * Create an XSSFSheet from an existing sheet in the XSSFWorkbook.
 *  The cloned sheet is a deep copy of the original but with a new given
 *  name.
 *
 * @param sheetNum The index of the sheet to clone
 * @param newName The name to set for the newly created sheet
 * @return XSSFSheet representing the cloned sheet.
 * @throws IllegalArgumentException if the sheet index or the sheet
 *         name is invalid
 * @throws POIXMLException if there were errors when cloning
 */
public XSSFSheet cloneSheet(int sheetNum, String newName) {
    // 1. Setup and Validation
    XSSFSheet srcSheet = getSourceSheetForCloning(sheetNum);
    String validatedName = resolveNewSheetName(srcSheet, newName);

    // 2. Initialize the New Sheet
    XSSFSheet clonedSheet = createSheet(validatedName);

    // 3. Transfer Data and Relationships
    copySheetRelationships(srcSheet, clonedSheet);
    copySheetExternalRelationships(srcSheet, clonedSheet);
    copySheetContent(srcSheet, clonedSheet);

    // 4. Post-Processing and Special Elements
    sanitizeClonedSheet(clonedSheet);
    cloneDrawingIfPresent(srcSheet, clonedSheet);

    clonedSheet.setSelected(false);
    return clonedSheet;
}

private XSSFSheet getSourceSheetForCloning(int sheetNum) {
    validateSheetIndex(sheetNum);
    return sheets.get(sheetNum);
}

private String resolveNewSheetName(XSSFSheet srcSheet, String providedName) {
    if (providedName == null) {
        return getUniqueSheetName(srcSheet.getSheetName());
    }
    validateSheetName(providedName);
    return providedName;
}

private void copySheetRelationships(XSSFSheet src, XSSFSheet dest) {
    for (RelationPart rp : src.getRelationParts()) {
        // We skip XSSFDrawing here because it requires a specialized 
        // patriarch-based recreation later to avoid XML corruption.
        if (!(rp.getDocumentPart() instanceof XSSFDrawing)) {
            addRelation(rp, dest);
        }
    }
}

private void copySheetExternalRelationships(XSSFSheet src, XSSFSheet dest) {
    try {
        for (PackageRelationship pr : src.getPackagePart().getRelationships()) {
            if (pr.getTargetMode() == TargetMode.EXTERNAL) {
                dest.getPackagePart().addExternalRelationship(
                    pr.getTargetURI().toASCIIString(),
                    pr.getRelationshipType(),
                    pr.getId()
                );
            }
        }
    } catch (InvalidFormatException e) {
        throw new POIXMLException("Failed to clone external relationships for sheet: " + src.getSheetName(), e);
    }
}

private void copySheetContent(XSSFSheet src, XSSFSheet dest) {
    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
        src.write(out);
        try (ByteArrayInputStream bis = new ByteArrayInputStream(out.toByteArray())) {
            dest.read(bis);
        }
    } catch (IOException e) {
        throw new POIXMLException("Critical error during binary copy of sheet: " + src.getSheetName(), e);
    }
}

private void sanitizeClonedSheet(XSSFSheet sheet) {
    CTWorksheet ct = sheet.getCTWorksheet();
    
    if (ct.isSetLegacyDrawing()) {
        logger.log(POILogger.WARN, "Comments/Legacy Drawings are not supported in cloning and have been removed.");
        ct.unsetLegacyDrawing();
    }
    if (ct.isSetPageSetup()) {
        logger.log(POILogger.WARN, "PageSetup properties are not supported in cloning and have been removed.");
        ct.unsetPageSetup();
    }
}

private void cloneDrawingIfPresent(XSSFSheet src, XSSFSheet dest) {
    XSSFDrawing srcDrawing = src.getDrawingPatriarch();
    if (srcDrawing == null) return;

    // Reset existing drawing reference in the XML bean to allow fresh initialization
    CTWorksheet ct = dest.getCTWorksheet();
    if (ct.isSetDrawing()) {
        ct.unsetDrawing();
    }

    XSSFDrawing clonedDrawing = dest.createDrawingPatriarch();
    clonedDrawing.getCTDrawing().set(srcDrawing.getCTDrawing());

    // Clone drawing-level relations (e.g., images inside the drawing)
    for (RelationPart rp : srcDrawing.getRelationParts()) {
        addRelation(rp, clonedDrawing);
    }
}