package com.lafarge.services;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class CertificateGenerator {

    private static final DeviceRgb ORANGE = new DeviceRgb(217, 119, 87);
    private static final DeviceRgb DARK_BLUE = new DeviceRgb(31, 56, 100);
    private static final DeviceRgb GRAY = new DeviceRgb(120, 120, 120);

    public static String generateCertificate(
            String employeeName,
            String trainingTitle,
            String level,
            String duration,
            Long enrollmentId,
            String certificateNumber,
            String trainerName) throws IOException, Exception {

        // 1. Dossier de stockage
        String directoryPath = System.getProperty("jboss.server.data.dir") + File.separator + "certificates";
        File dir = new File(directoryPath);
        if (!dir.exists()) dir.mkdirs();

        String fileName = "certificate_" + enrollmentId + ".pdf";
        String filePath = directoryPath + File.separator + fileName;

        // 2. Créer le PDF
        PdfWriter writer = new PdfWriter(filePath);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc, PageSize.A4);
        document.setMargins(70, 60, 60, 60);

        // 3. Créer une page vide
        pdfDoc.addNewPage();

        // 4. Contenu du certificat
        document.add(new Paragraph("HOLCIM ACADEMY")
                .setFontSize(14)
                .setBold()
                .setFontColor(DARK_BLUE)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(15));

        document.add(new Paragraph("Certificat de réussite")
                .setFontSize(32)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(35));

        document.add(new Paragraph("Ce certificat atteste que")
                .setFontSize(11)
                .setFontColor(GRAY)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(12));

        document.add(new Paragraph(employeeName)
                .setFontSize(28)
                .setBold()
                .setFontColor(DARK_BLUE)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(20));

        document.add(new Paragraph("a suivi avec succès la formation")
                .setFontSize(11)
                .setFontColor(GRAY)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(12));

        document.add(new Paragraph("« " + trainingTitle + " »")
                .setFontSize(20)
                .setItalic()
                .setFontColor(DARK_BLUE)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(15)
                .setMarginBottom(35));

        // Tableau des métadonnées
        Table table = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1, 1}))
                .useAllAvailableWidth()
                .setMarginTop(15);

        table.addCell(createMetaCell("Niveau", level));
        table.addCell(createMetaCell("Durée", duration));
        table.addCell(createMetaCell("Date d'obtention",
                LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));
        table.addCell(createMetaCell("N° certificat", certificateNumber));

        document.add(table);

        // Bloc du bas : Signature + QR Code
        Table bottomTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                .useAllAvailableWidth()
                .setMarginTop(80);

        // Signature (nom du formateur)
        Cell sigCell = new Cell().setBorder(Border.NO_BORDER);
        sigCell.add(new Paragraph(trainerName != null ? trainerName : "Direction Pédagogique")
                .setFontSize(22)
                .setItalic()
                .setFontColor(DARK_BLUE)
                .setTextAlignment(TextAlignment.CENTER));
        sigCell.add(new Paragraph("______________________")
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(GRAY));
        sigCell.add(new Paragraph("Formateur certifié")
                .setFontSize(10)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(GRAY));
        bottomTable.addCell(sigCell);

        // QR Code
        Cell qrCell = new Cell().setBorder(Border.NO_BORDER);
        String qrContent = "http://localhost:5173/verify-certificate/" + enrollmentId;
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(qrContent, BarcodeFormat.QR_CODE, 200, 200);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", baos);
        Image qrImage = new Image(ImageDataFactory.create(baos.toByteArray()));
        qrImage.setWidth(110).setHeight(110)
                .setHorizontalAlignment(HorizontalAlignment.CENTER);
        qrCell.add(qrImage);
        qrCell.add(new Paragraph("Scanner pour vérifier l'authenticité")
                .setFontSize(8)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(GRAY));
        bottomTable.addCell(qrCell);

        document.add(bottomTable);

        // Bordure orange
        PdfCanvas canvas = new PdfCanvas(pdfDoc.getFirstPage());
        canvas.setStrokeColor(ORANGE)
              .setLineWidth(2.5f)
              .rectangle(20, 20, PageSize.A4.getWidth() - 40, PageSize.A4.getHeight() - 40)
              .stroke();

        document.close();
        System.out.println(">>> Certificat généré avec succès : " + filePath);

        return filePath;
    }

    private static Cell createMetaCell(String label, String value) {
        Cell cell = new Cell().setBorder(Border.NO_BORDER).setPadding(5);
        cell.add(new Paragraph(label)
                .setFontSize(9)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(ColorConstants.BLACK));
        cell.add(new Paragraph(value)
                .setFontSize(11)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(DARK_BLUE));
        return cell;
    }
}