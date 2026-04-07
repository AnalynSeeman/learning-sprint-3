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
    XSSFSheet srcSheet = createSheetFromClone(sheetNum, newName);
    String effectiveName = (newName == null) ? getUniqueSheetName(srcSheet.getSheetName()) : newName;
    
    XSSFSheet clonedSheet = createSheet(effectiveName);

    // Helper 1: Copy relations and identify drawings
    XSSFDrawing dg = copySheetRelations(srcSheet, clonedSheet);

    // Copy external relationships
    try{
        copyExternalRelationships(srcSheet, clonedSheet);
    }
    catch (InvalidFormatException e) {
        throw new POIXMLException("Failed to clone sheet external relationships", e);
    }

    // Copy sheet data
    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
        srcSheet.write(out);
        try (ByteArrayInputStream bis = new ByteArrayInputStream(out.toByteArray())) {
            clonedSheet.read(bis);
        }
    } catch (IOException e) {
        throw new POIXMLException("Failed to clone sheet data stream", e);
    }

    // Clean up unsupported elements
    CTWorksheet ct = clonedSheet.getCTWorksheet();
    if (ct.isSetLegacyDrawing()) {
        logger.log(POILogger.WARN, "Cloning sheets with comments is not yet supported.");
        ct.unsetLegacyDrawing();
    }
    if (ct.isSetPageSetup()) {
        logger.log(POILogger.WARN, "Cloning sheets with page setup is not yet supported.");
        ct.unsetPageSetup();
    }

    clonedSheet.setSelected(false);

    // Helper 2: Clone drawings
    if (dg != null) {
        cloneSheetDrawing(srcSheet, clonedSheet, dg);
    }

    return clonedSheet;
}

/**
 * Validates the source and name logic for the cloning process.
 */
private XSSFSheet createSheetFromClone(int sheetNum, String newName) {
    validateSheetIndex(sheetNum);
    XSSFSheet srcSheet = sheets.get(sheetNum);

    if (newName != null) {
        validateSheetName(newName);
    }
    
    return srcSheet;
}

/**
 * Copies relations from source to clone, skipping the drawing relation to avoid conflicts.
 */
private XSSFDrawing copySheetRelations(XSSFSheet srcSheet, XSSFSheet clonedSheet) {
    XSSFDrawing dg = null;
    List<RelationPart> rels = srcSheet.getRelationParts();
    
    for (RelationPart rp : rels) {
        POIXMLDocumentPart r = rp.getDocumentPart();
        if (r instanceof XSSFDrawing) {
            dg = (XSSFDrawing) r;
            continue;
        }
        addRelation(rp, clonedSheet);
    }
    return dg;
}

private void copyExternalRelationships(XSSFSheet srcSheet, XSSFSheet clonedSheet) throws InvalidFormatException {
    for (PackageRelationship pr : srcSheet.getPackagePart().getRelationships()) {
        if (pr.getTargetMode() == TargetMode.EXTERNAL) {
            clonedSheet.getPackagePart().addExternalRelationship(
                pr.getTargetURI().toASCIIString(),
                pr.getRelationshipType(),
                pr.getId()
            );
        }
    }
}

/**
 * Clones the sheet drawing and all associated drawing relations.
 */
private void cloneSheetDrawing(XSSFSheet srcSheet, XSSFSheet clonedSheet, XSSFDrawing srcDrawing) {
    CTWorksheet ct = clonedSheet.getCTWorksheet();
    if (ct.isSetDrawing()) {
        ct.unsetDrawing();
    }
    
    XSSFDrawing clonedDg = clonedSheet.createDrawingPatriarch();
    clonedDg.getCTDrawing().set(srcDrawing.getCTDrawing());

    // Re-fetch to ensure we are working with the initialized patriarch
    clonedDg = clonedSheet.createDrawingPatriarch();

    // Clone drawing relations
    List<RelationPart> srcDrawingRels = srcSheet.createDrawingPatriarch().getRelationParts();
    for (RelationPart rp : srcDrawingRels) {
        addRelation(rp, clonedDg);
    }
}