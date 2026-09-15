package com.alvexo.bookingapp.util;

import com.alvexo.bookingapp.model.Settlement;
import com.alvexo.bookingapp.model.User;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.List;

/**
 * Minimal, dependency-light PDF rendering for the Finance tab's two downloads
 * (WORKSHOP_FINANCE_API_SPEC.md §4). Plain text/table layout — no styling
 * requirements were specified, so this favours being correct and simple over
 * matching a specific visual design.
 */
public final class SettlementPdfGenerator {

    private static final PDType1Font FONT = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDType1Font FONT_BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final float MARGIN = 50;
    private static final float LINE_HEIGHT = 16;

    private SettlementPdfGenerator() {
    }

    public static byte[] generateReceipt(User mechanic, Settlement s) {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float y = page.getMediaBox().getHeight() - MARGIN;

                y = writeLine(cs, MARGIN, y, FONT_BOLD, 16, "Settlement Receipt");
                y -= LINE_HEIGHT / 2;
                y = writeLine(cs, MARGIN, y, FONT_BOLD, 11, mechanic.getWorkshopName());
                y = writeLine(cs, MARGIN, y, FONT, 10, "Workshop ID: WS-" + mechanic.getId());
                y -= LINE_HEIGHT;

                y = writeLine(cs, MARGIN, y, FONT, 11, "Settlement date: " + s.getSettlementDate());
                y = writeLine(cs, MARGIN, y, FONT, 11, "Net pay: " + money(s.getNetPay()));
                y = writeLine(cs, MARGIN, y, FONT, 11,
                        "Service Reliability Adjustment: -" + money(s.getServiceReliabilityAdjustment()));
                y = writeLine(cs, MARGIN, y, FONT_BOLD, 11, "Settlement released: " + money(s.getSettlementReleased()));
                y -= LINE_HEIGHT;

                y = writeLine(cs, MARGIN, y, FONT_BOLD, 11, "Payment transaction");
                y = writeLine(cs, MARGIN, y, FONT, 10, "Transaction number: " + s.getTransactionNumber());
                y = writeLine(cs, MARGIN, y, FONT, 10, "Gateway: " + s.getPaymentGateway());
                y = writeLine(cs, MARGIN, y, FONT, 10, "Gateway reference: " + s.getPaymentGatewayReference());
                y = writeLine(cs, MARGIN, y, FONT, 10,
                        "Paid on: " + s.getPaymentDate() + " " + s.getPaymentTime());
                writeLine(cs, MARGIN, y, FONT, 10, "Status: " + s.getPaymentStatus());
            }

            return toBytes(doc);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to generate settlement receipt PDF", e);
        }
    }

    public static byte[] generateStatement(User mechanic, List<Settlement> settlements,
                                            LocalDate from, LocalDate to) {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            PDPageContentStream cs = new PDPageContentStream(doc, page);
            try {
                float y = page.getMediaBox().getHeight() - MARGIN;

                y = writeLine(cs, MARGIN, y, FONT_BOLD, 16, "Settlement Statement");
                y -= LINE_HEIGHT / 2;
                y = writeLine(cs, MARGIN, y, FONT_BOLD, 11, mechanic.getWorkshopName());
                y = writeLine(cs, MARGIN, y, FONT, 10, "Workshop ID: WS-" + mechanic.getId());
                y = writeLine(cs, MARGIN, y, FONT, 10, "Period: " + from + " to " + to);
                y -= LINE_HEIGHT;

                float[] columnX = {MARGIN, MARGIN + 90, MARGIN + 180, MARGIN + 280, MARGIN + 380};
                y = writeRow(cs, columnX, y, FONT_BOLD,
                        "Date", "Net pay", "Adjustment", "Released", "Status");
                y -= 4;

                java.math.BigDecimal totalNet = java.math.BigDecimal.ZERO;
                java.math.BigDecimal totalAdjustment = java.math.BigDecimal.ZERO;
                java.math.BigDecimal totalReleased = java.math.BigDecimal.ZERO;

                for (Settlement s : settlements) {
                    if (y < MARGIN + LINE_HEIGHT) {
                        cs.close();
                        page = new PDPage(PDRectangle.A4);
                        doc.addPage(page);
                        cs = new PDPageContentStream(doc, page);
                        y = page.getMediaBox().getHeight() - MARGIN;
                    }
                    y = writeRow(cs, columnX, y, FONT,
                            String.valueOf(s.getSettlementDate()),
                            money(s.getNetPay()),
                            money(s.getServiceReliabilityAdjustment()),
                            money(s.getSettlementReleased()),
                            String.valueOf(s.getStatus()));
                    totalNet = totalNet.add(s.getNetPay());
                    totalAdjustment = totalAdjustment.add(s.getServiceReliabilityAdjustment());
                    totalReleased = totalReleased.add(s.getSettlementReleased());
                }

                y -= LINE_HEIGHT / 2;
                writeRow(cs, columnX, y, FONT_BOLD,
                        "Total", money(totalNet), money(totalAdjustment), money(totalReleased), "");
            } finally {
                cs.close();
            }

            return toBytes(doc);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to generate settlement statement PDF", e);
        }
    }

    private static float writeLine(PDPageContentStream cs, float x, float y, PDType1Font font,
                                    float size, String text) throws IOException {
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(text != null ? text : "");
        cs.endText();
        return y - LINE_HEIGHT;
    }

    private static float writeRow(PDPageContentStream cs, float[] columnX, float y, PDType1Font font,
                                   String... values) throws IOException {
        for (int i = 0; i < values.length && i < columnX.length; i++) {
            cs.beginText();
            cs.setFont(font, 9);
            cs.newLineAtOffset(columnX[i], y);
            cs.showText(values[i] != null ? values[i] : "");
            cs.endText();
        }
        return y - LINE_HEIGHT;
    }

    private static String money(java.math.BigDecimal amount) {
        return amount == null ? "0" : "Rs. " + amount.stripTrailingZeros().toPlainString();
    }

    private static byte[] toBytes(PDDocument doc) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        doc.save(out);
        return out.toByteArray();
    }
}
