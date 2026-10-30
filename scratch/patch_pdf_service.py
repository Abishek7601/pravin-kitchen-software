path = r'src/main/java/com/example/pravin_quotation/service/QuotationPdfService.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

target1 = '''        Image itemImage = loadItemImage(imageUrl);
        boolean hasImage = (itemImage != null);
        String safeName = safe(materialName).trim();
        boolean isPhotoPlaceholder = safeName.equalsIgnoreCase("PHOTO") || safeName.equalsIgnoreCase("PHOTO - PHOTO");

        // If an actual image is available and the material name is "PHOTO", remove the PHOTO text
        boolean showName = !safeName.isBlank() && !safeName.equals("-") && !(hasImage && isPhotoPlaceholder);'''

replacement1 = '''        Image itemImage = loadItemImage(imageUrl);
        boolean hasImage = (itemImage != null);
        String safeName = safe(materialName).trim();
        boolean isPhotoPlaceholder = safeName.equalsIgnoreCase("PHOTO")
                || safeName.equalsIgnoreCase("PHOTO - PHOTO")
                || safeName.toUpperCase().matches("^PHOTO([\\\\s\\\\-_/]*[\\\\[\\\\(]?(FILE|FILES)[\\\\]\\\\)]?)?$");

        // If an actual image is available and the material name is "PHOTO", remove the PHOTO text
        boolean showName = !safeName.isBlank() && !safeName.equals("-") && !(hasImage && isPhotoPlaceholder);'''

target2 = '''        // 2. Local file system lookup
        try {
            String rel = cleanUrl.startsWith("/") ? cleanUrl.substring(1) : cleanUrl;
            Path path = Paths.get(rel);
            if (!Files.isRegularFile(path)) {
                path = Paths.get("uploads").resolve(rel.replaceFirst("^uploads[/\\\\\\\\]", ""));
            }
            if (!Files.isRegularFile(path)) {
                path = Paths.get(cleanUrl);
            }'''

replacement2 = '''        // 2. Local file system lookup
        try {
            String rel = (cleanUrl.startsWith("/") || cleanUrl.startsWith("\\\\")) ? cleanUrl.substring(1) : cleanUrl;
            Path path = Paths.get(rel);
            if (!Files.isRegularFile(path)) {
                path = Paths.get("uploads").resolve(rel.replaceFirst("^uploads[/\\\\\\\\]", ""));
            }
            if (!Files.isRegularFile(path)) {
                path = Paths.get(cleanUrl);
            }
            if (!Files.isRegularFile(path)) {
                path = Paths.get(System.getProperty("user.dir", ".")).resolve(rel);
            }
            if (!Files.isRegularFile(path)) {
                path = Paths.get(System.getProperty("user.dir", ".")).resolve("uploads").resolve(rel.replaceFirst("^uploads[/\\\\\\\\]", ""));
            }'''

assert target1 in content, 'target1 not found'
assert target2 in content, 'target2 not found'
new_content = content.replace(target1, replacement1).replace(target2, replacement2)
with open(path, 'w', encoding='utf-8') as f:
    f.write(new_content)
print('Successfully patched QuotationPdfService.java')

