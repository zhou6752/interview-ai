package com.interviewai.util;

import com.interviewai.dto.InterviewReport;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;

import java.io.ByteArrayOutputStream;
import java.util.List;

public class PdfExporter {

    private PdfExporter() {
    }

    public static byte[] exportInterviewReport(InterviewReport report, String position) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(bos);
            PdfDocument pdf = new PdfDocument(writer);
            Document doc = new Document(pdf);

            PdfFont titleFont = PdfFontFactory.createFont("Helvetica-Bold");
            PdfFont bodyFont = PdfFontFactory.createFont("Helvetica");

            Paragraph title = new Paragraph("模拟面试评估报告")
                    .setFont(titleFont).setFontSize(20)
                    .setTextAlignment(TextAlignment.CENTER);
            doc.add(title);

            doc.add(new Paragraph("面试方向：" + position)
                    .setFont(bodyFont).setFontSize(12).setMarginTop(12));
            doc.add(new Paragraph("综合评分：" + report.getOverallScore() + " 分")
                    .setFont(titleFont).setFontSize(16).setMarginTop(8));

            doc.add(new Paragraph("维度评分：").setFont(bodyFont).setFontSize(12).setMarginTop(16));
            doc.add(new Paragraph("- 技术深度：" + report.getTechnicalScore()).setFont(bodyFont).setFontSize(11));
            doc.add(new Paragraph("- 逻辑表达：" + report.getLogicScore()).setFont(bodyFont).setFontSize(11));
            doc.add(new Paragraph("- 知识广度：" + report.getKnowledgeBreadth()).setFont(bodyFont).setFontSize(11));
            doc.add(new Paragraph("- 实践经验：" + report.getPracticeScore()).setFont(bodyFont).setFontSize(11));

            if (report.getOverallComment() != null) {
                doc.add(new Paragraph("综合评价：").setFont(bodyFont).setFontSize(12).setMarginTop(16));
                doc.add(new Paragraph(report.getOverallComment()).setFont(bodyFont).setFontSize(11));
            }

            if (report.getLearningPath() != null && !report.getLearningPath().isEmpty()) {
                doc.add(new Paragraph("学习建议：").setFont(bodyFont).setFontSize(12).setMarginTop(16));
                for (String path : report.getLearningPath()) {
                    doc.add(new Paragraph("- " + path).setFont(bodyFont).setFontSize(11));
                }
            }

            if (report.getQuestionDetails() != null) {
                doc.add(new Paragraph("答题详情：").setFont(bodyFont).setFontSize(12).setMarginTop(16));
                for (InterviewReport.QuestionDetail detail : report.getQuestionDetails()) {
                    doc.add(new Paragraph("Q: " + detail.getQuestion()).setFont(bodyFont).setFontSize(10).setMarginTop(8));
                    doc.add(new Paragraph("回答: " + detail.getUserAnswer()).setFont(bodyFont).setFontSize(10));
                    doc.add(new Paragraph("点评: " + detail.getComment()).setFont(bodyFont).setFontSize(10));
                }
            }

            doc.close();
            return bos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("PDF 生成失败：" + e.getMessage(), e);
        }
    }
}