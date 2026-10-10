package com.example.pravin_quotation;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@SpringBootTest
class PravinQuotationApplicationTests {

	@Test
	void contextLoads() {
	}

	@Test
	void sanitizeTemplateBrandingStrings() throws Exception {
		Path templatesDir = Paths.get("src/main/resources/templates");
		if (Files.exists(templatesDir)) {
			Files.walk(templatesDir)
					.filter(p -> p.toString().endsWith(".html"))
					.forEach(p -> {
						try {
							String content = Files.readString(p);
							String updated = content
									.replace("INTERIORSS", "INTERIORS")
									.replace("Pravin KITCHENS & INTERIORS & INTERIORS", "Pravin KITCHENS & INTERIORS")
									.replace("Pravin KITCHENS &amp; INTERIORS &amp; INTERIORS", "Pravin KITCHENS &amp; INTERIORS");
							if (!updated.equals(content)) {
								Files.writeString(p, updated);
							}
						} catch (Exception e) {
							throw new RuntimeException(e);
						}
					});
		}
	}

}
