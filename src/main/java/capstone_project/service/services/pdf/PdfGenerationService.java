package capstone_project.service.services.pdf;

import capstone_project.entity.order.order.OrderEntity;
import capstone_project.entity.user.address.AddressEntity;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
@RequiredArgsConstructor
public class PdfGenerationService {

    private final Font TITLE_FONT;
    private final Font SUBTITLE_FONT;
    private final Font NORMAL_FONT;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private Font loadFont(float size, int style) throws DocumentException, IOException {
        BaseFont baseFont = BaseFont.createFont("src/main/resources/fonts/arial.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
        return new Font(baseFont, size, style);
    }

    public PdfGenerationService() throws DocumentException, IOException {
        this.TITLE_FONT = loadFont(18, Font.BOLD);
        this.SUBTITLE_FONT = loadFont(14, Font.BOLD);
        this.NORMAL_FONT = loadFont(12, Font.NORMAL);
    }

    public byte[] generateOrderPdf(OrderEntity order) throws DocumentException, IOException {
        Document document = new Document();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, outputStream);
            document.open();

            // Add title
            Paragraph title = new Paragraph("ORDER INVOICE", TITLE_FONT);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            // Add order details
            addOrderDetails(document, order);

            document.close();
            return outputStream.toByteArray();

        } catch (DocumentException e) {
            log.error("Error generating PDF: {}", e.getMessage(), e);
            throw e;
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }
    }

    private String formatAddress(AddressEntity address) {
        if (address == null) {
            return "N/A";
        }

        // Build the address parts manually to ensure proper formatting
        StringBuilder sb = new StringBuilder();
        if (address.getStreet() != null && !address.getStreet().trim().isEmpty()) {
            sb.append(address.getStreet().trim());
        }
        if (address.getWard() != null && !address.getWard().trim().isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(address.getWard().trim());
        }
        if (address.getProvince() != null && !address.getProvince().trim().isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(address.getProvince().trim());
        }

        return sb.toString();
    }

    private void addOrderDetails(Document document, OrderEntity order) throws DocumentException {
        // Order Information
        Paragraph orderInfo = new Paragraph("Order Information", SUBTITLE_FONT);
        orderInfo.setSpacingBefore(20);
        orderInfo.setSpacingAfter(10);
        document.add(orderInfo);

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);
        table.setSpacingAfter(10);

//        addTableRow(table, "Order ID:", order.getId().toString());
        addTableRow(table, "Order Code:", order.getOrderCode());
        addTableRow(table, "Order Date:", order.getCreatedAt().format(DATE_FORMATTER));
        addTableRow(table, "Total Price:", order.getTotalPrice() + " VND");

        document.add(table);

        // Add pickup and delivery addresses
        if (order.getPickupAddress() != null) {
            addAddressSection(document, "Pickup Address", formatAddress(order.getPickupAddress()));
        }
        if (order.getDeliveryAddress() != null) {
            addAddressSection(document, "Delivery Address", formatAddress(order.getDeliveryAddress()));
        }
    }
    private void addAddressSection(Document document, String title, String address) throws DocumentException {
        if (address != null && !address.isEmpty()) {
            Paragraph sectionTitle = new Paragraph(title, SUBTITLE_FONT);
            sectionTitle.setSpacingBefore(15);
            sectionTitle.setSpacingAfter(5);
            document.add(sectionTitle);

            document.add(new Paragraph(address, NORMAL_FONT));
        }
    }

    private void addTableRow(PdfPTable table, String header, String value) {
        PdfPCell cell1 = new PdfPCell(new Phrase(header, NORMAL_FONT));
        PdfPCell cell2 = new PdfPCell(new Phrase(value != null ? value : "N/A", NORMAL_FONT));

        cell1.setBorder(Rectangle.NO_BORDER);
        cell2.setBorder(Rectangle.NO_BORDER);

        table.addCell(cell1);
        table.addCell(cell2);
    }
}