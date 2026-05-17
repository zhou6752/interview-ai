package com.interviewai.util;

import com.interviewai.exception.InvalidFileTypeException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

public class DocumentParserUtilTest {

    @Test
    public void testExtractText_文件为null_抛出异常() {
        Assertions.assertThrows(InvalidFileTypeException.class, () -> {
            DocumentParserUtil.extractText(null);
        });
    }

    @Test
    public void testExtractText_空文件_抛出异常() {
        MultipartFile emptyFile = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", new byte[0]
        );
        Assertions.assertThrows(InvalidFileTypeException.class, () -> {
            DocumentParserUtil.extractText(emptyFile);
        });
    }

    @Test
    public void testExtractText_不支持的后缀_抛出异常() {
        MultipartFile exeFile = new MockMultipartFile(
                "file", "virus.exe", "application/octet-stream", new byte[10]
        );
        Assertions.assertThrows(InvalidFileTypeException.class, () -> {
            DocumentParserUtil.extractText(exeFile);
        });
    }

    @Test
    public void testExtractText_空文件名_抛出异常() {
        MultipartFile noNameFile = new MockMultipartFile(
                "file", null, "application/pdf", new byte[10]
        );
        Assertions.assertThrows(InvalidFileTypeException.class, () -> {
            DocumentParserUtil.extractText(noNameFile);
        });
    }

    @Test
    public void testExtractText_正常PDF_返回文本(@TempDir Path tempDir) throws Exception {
        Path tempFile = tempDir.resolve("test-resume.pdf");

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                cs.newLineAtOffset(100, 700);
                cs.showText("Name: Zhang San");
                cs.newLineAtOffset(0, -20);
                cs.showText("School: Guangxi University of Science and Technology");
                cs.newLineAtOffset(0, -20);
                cs.showText("Major: Software Engineering");
                cs.endText();
            }
            document.save(tempFile.toFile());
        }

        byte[] pdfBytes = java.nio.file.Files.readAllBytes(tempFile);
        MultipartFile pdfFile = new MockMultipartFile(
                "file", "简历.pdf", "application/pdf", pdfBytes
        );

        String text = DocumentParserUtil.extractText(pdfFile);

        Assertions.assertTrue(text.contains("Zhang San"));
        Assertions.assertTrue(text.contains("Guangxi University"));
        Assertions.assertTrue(text.contains("Software Engineering"));
    }

    @Test
    public void testExtractText_TXT文件_返回文本() {
        MultipartFile txtFile = new MockMultipartFile(
                "file", "简历.txt", "text/plain",
                "精通Java、Spring、MySQL".getBytes()
        );

        String text = DocumentParserUtil.extractText(txtFile);
        Assertions.assertTrue(text.contains("Java"));
        Assertions.assertTrue(text.contains("Spring"));
        Assertions.assertTrue(text.contains("MySQL"));
    }
}