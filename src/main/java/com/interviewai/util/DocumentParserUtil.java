package com.interviewai.util;

import com.interviewai.exception.InvalidFileTypeException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;

public class DocumentParserUtil {

    private static final Logger log = LoggerFactory.getLogger(DocumentParserUtil.class);
    private static final Tika tika = new Tika();
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".pdf", ".docx", ".doc", ".txt", ".md");
    private static final long MAX_SIZE = 5 * 1024 * 1024;

    private DocumentParserUtil() {
    }

    public static String extractText(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileTypeException("上传的文件为空");
        }

        if (file.getSize() > MAX_SIZE) {
            throw new InvalidFileTypeException("文件大小超过限制（最大 5MB）");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank()) {
            throw new InvalidFileTypeException("文件名为空");
        }

        String ext = "";
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex == -1) {
            throw new InvalidFileTypeException("无法识别文件类型，请上传 PDF/DOCX/DOC/TXT 文件");
        }
        ext = filename.substring(dotIndex).toLowerCase();

        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new InvalidFileTypeException("不支持的文件类型：" + ext + "，仅支持 PDF / DOCX / DOC / TXT / MD");
        }

        try {
            String text = tika.parseToString(file.getInputStream()).trim();

            // Tika 对中文 PDF 可能返回空（PDFBox 3.x 字体编码兼容问题），
            // 回退到 PDFBox 直读，它对 CJK 字体支持更可靠
            if (text.isEmpty() && ".pdf".equals(ext)) {
                log.info("Tika 解析 PDF 返回空，回退 PDFBox 直读: {}", filename);
                text = extractPdfWithPdfBox(file);
            }

            if (text.isEmpty()) {
                throw new InvalidFileTypeException(
                        "未从文件中提取到文字内容。"
                        + (".pdf".equals(ext) ? " 该 PDF 可能是扫描件（图片 PDF），本项目暂不支持 OCR 识别。" : ""));
            }

            return text;
        } catch (InvalidFileTypeException e) {
            throw e;
        } catch (IOException | TikaException e) {
            throw new InvalidFileTypeException("文档解析失败：" + e.getMessage());
        }
    }

    private static String extractPdfWithPdfBox(MultipartFile file) throws IOException {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(document).trim();
        }
    }
}