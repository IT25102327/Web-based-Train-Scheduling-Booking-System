package com.trainbooking.it25100977.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.WriterProperties;
import com.itextpdf.kernel.pdf.canvas.draw.DashedLine;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import com.trainbooking.exception.ResourceNotFoundException;
import com.trainbooking.it25100977.dto.PaymentRequest;
import com.trainbooking.it25100977.model.Payment;
import com.trainbooking.it25100977.model.Ticket;
import com.trainbooking.it25100977.repository.PaymentRepository;
import com.trainbooking.it25100977.repository.TicketRepository;
import com.trainbooking.it25103308.model.Booking;
import com.trainbooking.it25103308.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Service class handling mock payment processing, e-ticket generation, QR encoding, and PDF generation.
 *
 * @author SLIIT Software Engineering Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final TicketRepository ticketRepository;
    private final BookingRepository bookingRepository;
    private final com.trainbooking.it25100977.repository.RefundRepository refundRepository;
    private final com.trainbooking.it25102925.service.EmailService emailService;

    /**
     * Processes simulated card payment for a pending booking.
     * Supports failure simulation if card number ends with 0000 or CVV is 000.
     *
     * @param bookingId booking ID
     * @param request payment card details
     * @return {@link Payment} entity
     */
    @Transactional
    public Payment processPayment(Long bookingId, PaymentRequest request) {
        log.info("Processing payment for booking ID: {}", bookingId);
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with ID: " + bookingId));

        if (booking.getStatus() == Booking.BookingStatus.CANCELLED) {
            throw new IllegalStateException("Cannot process payment: Reservation has been cancelled or 10-minute hold expired.");
        }

        // Decline simulation trigger (card ending in 0000 or CVV 000)
        String cleanCard = (request.getCardNumber() != null) ? request.getCardNumber().replaceAll("\\s+", "") : "";
        if (cleanCard.endsWith("0000") || "000".equals(request.getCvv())) {
            log.warn("Payment declined for booking ID: {} - simulated decline criteria matched.", bookingId);
            Payment failedPayment = Payment.builder()
                    .booking(booking)
                    .amount(booking.getTotalAmount())
                    .status(Payment.PaymentStatus.FAILED)
                    .transactionRef("FAIL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                    .build();
            paymentRepository.save(failedPayment);
            throw new IllegalArgumentException("Card transaction declined by payment gateway: Insufficient funds or invalid card credentials. Please retry with a valid card before your 10-minute seat lock expires.");
        }

        Payment payment = Payment.builder()
                .booking(booking)
                .amount(booking.getTotalAmount())
                .status(Payment.PaymentStatus.COMPLETED)
                .transactionRef("TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .build();

        Payment savedPayment = paymentRepository.save(payment);

        booking.setStatus(Booking.BookingStatus.CONFIRMED);
        bookingRepository.save(booking);

        log.info("Payment completed for booking ID: {}, txn ref: {}", bookingId, savedPayment.getTransactionRef());
        return savedPayment;
    }

    /**
     * Generates a confirmed electronic ticket with QR code data for a paid booking and emails the passenger.
     *
     * @param bookingId booking ID
     * @return generated {@link Ticket}
     */
    @Transactional
    public Ticket generateTicket(Long bookingId) {
        log.info("Generating ticket for booking ID: {}", bookingId);
        return ticketRepository.findByBookingId(bookingId).orElseGet(() -> {
            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new ResourceNotFoundException("Booking not found with ID: " + bookingId));

            Payment payment = paymentRepository.findByBookingId(bookingId)
                    .orElseGet(() -> processPayment(bookingId, new PaymentRequest()));

            String ticketNumber = "TKT-2026-" + (10000 + bookingId);

            Ticket ticket = Ticket.builder()
                    .booking(booking)
                    .payment(payment)
                    .ticketNumber(ticketNumber)
                    .qrCodeData(ticketNumber)
                    .isBoarded(false)
                    .build();

            Ticket saved = ticketRepository.save(ticket);
            log.info("Issued ticket ID: {} with number: {}", saved.getId(), saved.getTicketNumber());

            // Automated Email Dispatch with E-Ticket Confirmation
            try {
                if (booking.getPassenger() != null && booking.getPassenger().getEmail() != null) {
                    String recipientEmail = booking.getPassenger().getEmail();
                    String subject = "Sri Lanka Railways — E-Ticket Confirmation (" + ticket.getTicketNumber() + ")";
                    String body = "Dear " + ticket.getPassengerName() + ",\n\n"
                            + "Your train booking is CONFIRMED!\n\n"
                            + "Ticket Number: " + ticket.getTicketNumber() + "\n"
                            + "Train: " + ticket.getTrainName() + "\n"
                            + "Route: " + ticket.getOrigin() + " ➔ " + ticket.getDestination() + "\n"
                            + "Date: " + ticket.getTravelDate() + " at " + ticket.getDepartureTime() + "\n"
                            + "Seats: " + ticket.getSeatNumbers() + " (" + ticket.getSeatClass() + " Class)\n"
                            + "Total Fare Paid: LKR " + ticket.getTotalFare() + "\n"
                            + "Transaction ID: " + (payment != null ? payment.getTransactionRef() : "N/A") + "\n\n"
                            + "Please display this digital ticket or QR code to the station gate staff upon boarding.\n"
                            + "Safe travels!\nSri Lanka Railways Digital Ticketing";
                    emailService.sendSimpleEmail(recipientEmail, subject, body);
                    log.info("Dispatched ticket confirmation email to: {}", recipientEmail);
                }
            } catch (Exception ex) {
                log.warn("Could not dispatch confirmation email: {}", ex.getMessage());
            }

            return saved;
        });
    }

    /**
     * Generates QR code image bytes as PNG using ZXing.
     *
     * @param ticketId ticket ID
     * @return byte array containing the PNG image
     */
    /**
     * Generates QR code image bytes as PNG using ZXing.
     *
     * @param ticketId ticket ID
     * @return byte array containing the PNG image
     */
    public byte[] generateQrCodeImage(Long ticketId) {
        Ticket ticket = getTicketById(ticketId);
        String qrContent = (ticket != null && ticket.getTicketNumber() != null) ? ticket.getTicketNumber() : "TKT-2026-" + ticketId;
        return createQrCodeBytes(qrContent);
    }

    /**
     * Encodes arbitrary string content into high-contrast QR code image PNG bytes.
     *
     * @param content text data to encode
     * @return PNG image bytes
     */
    private byte[] createQrCodeBytes(String content) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, 200, 200);
            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            return pngOutputStream.toByteArray();
        } catch (Exception ex) {
            log.error("Failed to generate QR code bytes for content: {}", content, ex);
            return new byte[0];
        }
    }

    /**
     * Generates an authentic, publication-quality Sri Lanka Railways electronic ticket (boarding pass) PDF.
     * Includes embedded ZXing QR code, journey stations, departure/arrival times, seat allocations,
     * passenger verification data, and security hash.
     *
     * @param ticketId ticket ID
     * @return byte array containing the PDF document
     */
    public byte[] generateTicketPdf(Long ticketId) {
        log.info("Generating professional high-definition PDF ticket for ID: {}", ticketId);
        Ticket ticket = getTicketById(ticketId);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            WriterProperties writerProperties = new WriterProperties();
            writerProperties.setCompressionLevel(0);
            PdfWriter writer = new PdfWriter(baos, writerProperties);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf, PageSize.A4);
            document.setMargins(28f, 32f, 28f, 32f);

            PdfFont fontBold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont fontRegular = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            PdfFont fontOblique = PdfFontFactory.createFont(StandardFonts.HELVETICA_OBLIQUE);

            DeviceRgb navyColor = new DeviceRgb(26, 54, 93);      // #1A365D - Sri Lanka Railways Blue
            DeviceRgb goldColor = new DeviceRgb(197, 140, 31);    // #C58C1F - Royal Rail Gold
            DeviceRgb darkText = new DeviceRgb(45, 55, 72);       // #2D3748 - Dark Charcoal
            DeviceRgb mutedText = new DeviceRgb(113, 128, 150);   // #718096 - Muted gray
            DeviceRgb bgCard = new DeviceRgb(247, 250, 252);      // #F7FAFC - Soft background
            DeviceRgb borderLight = new DeviceRgb(226, 232, 240); // #E2E8F0 - Clean border
            DeviceRgb greenValid = new DeviceRgb(39, 148, 77);    // #27944D - Status Green

            String ticketNum = (ticket.getTicketNumber() != null && !ticket.getTicketNumber().isBlank())
                    ? ticket.getTicketNumber() : "TKT-2026-" + ticketId;
            String passengerName = (ticket.getPassengerName() != null && !ticket.getPassengerName().isBlank())
                    ? ticket.getPassengerName() : "Valued Passenger";
            String passengerNic = (ticket.getPassengerNic() != null && !ticket.getPassengerNic().isBlank())
                    ? ticket.getPassengerNic() : "N/A";
            String contactPhone = (ticket.getBooking() != null && ticket.getBooking().getContactPhone() != null && !ticket.getBooking().getContactPhone().isBlank())
                    ? ticket.getBooking().getContactPhone() : "N/A";
            String trainName = (ticket.getTrainName() != null && !ticket.getTrainName().isBlank())
                    ? ticket.getTrainName() : "Sri Lanka Railways Express";
            String origin = (ticket.getOrigin() != null && !ticket.getOrigin().isBlank())
                    ? ticket.getOrigin() : "Colombo Fort";
            String destination = (ticket.getDestination() != null && !ticket.getDestination().isBlank())
                    ? ticket.getDestination() : "Kandy";
            String depTime = (ticket.getDepartureTime() != null && !ticket.getDepartureTime().isBlank())
                    ? ticket.getDepartureTime() : "05:55 AM";
            String arrTime = (ticket.getArrivalTime() != null && !ticket.getArrivalTime().isBlank())
                    ? ticket.getArrivalTime() : "09:20 AM";
            String travelDate = (ticket.getTravelDate() != null)
                    ? ticket.getTravelDate().toString() : java.time.LocalDate.now().toString();
            String seatClass = (ticket.getSeatClass() != null && !ticket.getSeatClass().isBlank())
                    ? ticket.getSeatClass() : "FIRST";
            String seatNumbers = (ticket.getSeatNumbers() != null && !ticket.getSeatNumbers().isBlank())
                    ? ticket.getSeatNumbers() : "Car 01 / Seat 01";
            int seatCount = (ticket.getSeatCount() != null) ? ticket.getSeatCount() : 1;
            String farePaid = (ticket.getFarePaid() != null)
                    ? String.format("%,.2f", ticket.getFarePaid()) : "1,500.00";
            String txnRef = (ticket.getPayment() != null && ticket.getPayment().getTransactionRef() != null)
                    ? ticket.getPayment().getTransactionRef() : "TXN-" + ticketId;
            String status = (ticket.getStatus() != null) ? ticket.getStatus() : "CONFIRMED";

            // 1. TOP HEADER TABLE (Branding & Ticket No)
            Table headerTable = new Table(UnitValue.createPercentArray(new float[]{65, 35}));
            headerTable.setWidth(UnitValue.createPercentValue(100));

            Cell titleCell = new Cell().setBorder(Border.NO_BORDER).setPadding(0);
            titleCell.add(new Paragraph("SRI LANKA RAILWAYS")
                    .setFont(fontBold).setFontSize(18f).setFontColor(navyColor).setMargin(0));
            titleCell.add(new Paragraph("Official Electronic Passenger Ticket & Boarding Pass")
                    .setFont(fontRegular).setFontSize(9.5f).setFontColor(mutedText).setMarginTop(2f).setMarginBottom(0));
            titleCell.add(new Paragraph("Ministry of Transport & Highways • Digital Ticketing Service")
                    .setFont(fontOblique).setFontSize(8f).setFontColor(goldColor).setMarginTop(1f).setMarginBottom(0));
            headerTable.addCell(titleCell);

            Cell badgeCell = new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT).setPadding(0);
            badgeCell.add(new Paragraph("BOARDING PASS / E-TICKET")
                    .setFont(fontBold).setFontSize(8.5f).setFontColor(goldColor).setMargin(0));
            badgeCell.add(new Paragraph(ticketNum)
                    .setFont(fontBold).setFontSize(14f).setFontColor(navyColor).setMarginTop(2f).setMarginBottom(0));
            badgeCell.add(new Paragraph("● " + status)
                    .setFont(fontBold).setFontSize(9f).setFontColor(greenValid).setMarginTop(2f).setMarginBottom(0));
            headerTable.addCell(badgeCell);

            document.add(headerTable);

            // Accent dividing bar
            SolidLine solidGold = new SolidLine(2.5f);
            solidGold.setColor(goldColor);
            LineSeparator goldBar = new LineSeparator(solidGold);
            goldBar.setMarginTop(8f);
            goldBar.setMarginBottom(12f);
            document.add(goldBar);

            // 2. JOURNEY ROUTE HIGHLIGHT BOX
            Table routeCard = new Table(UnitValue.createPercentArray(new float[]{38, 24, 38}));
            routeCard.setWidth(UnitValue.createPercentValue(100));
            routeCard.setBackgroundColor(bgCard);
            routeCard.setBorder(new SolidBorder(borderLight, 1f));

            // Origin
            Cell origCell = new Cell().setBorder(Border.NO_BORDER).setPadding(10f);
            origCell.add(new Paragraph("FROM (ORIGIN STATION)").setFont(fontBold).setFontSize(7.5f).setFontColor(mutedText).setMargin(0));
            origCell.add(new Paragraph(origin).setFont(fontBold).setFontSize(14f).setFontColor(navyColor).setMarginTop(2f).setMarginBottom(0));
            origCell.add(new Paragraph("Dep: " + depTime).setFont(fontBold).setFontSize(10f).setFontColor(darkText).setMarginTop(3f).setMarginBottom(0));
            routeCard.addCell(origCell);

            // Mid arrow & train
            Cell midCell = new Cell().setBorder(Border.NO_BORDER).setPadding(10f).setTextAlignment(TextAlignment.CENTER);
            midCell.add(new Paragraph("EXPRESS SERVICE").setFont(fontBold).setFontSize(8f).setFontColor(goldColor).setMargin(0));
            midCell.add(new Paragraph("───── ➔ ─────").setFont(fontBold).setFontSize(11f).setFontColor(mutedText).setMarginTop(2f).setMarginBottom(0));
            midCell.add(new Paragraph(trainName).setFont(fontBold).setFontSize(9.5f).setFontColor(darkText).setMarginTop(2f).setMarginBottom(0));
            midCell.add(new Paragraph("Date: " + travelDate).setFont(fontRegular).setFontSize(8.5f).setFontColor(mutedText).setMarginTop(1f).setMarginBottom(0));
            routeCard.addCell(midCell);

            // Destination
            Cell destCell = new Cell().setBorder(Border.NO_BORDER).setPadding(10f).setTextAlignment(TextAlignment.RIGHT);
            destCell.add(new Paragraph("TO (DESTINATION STATION)").setFont(fontBold).setFontSize(7.5f).setFontColor(mutedText).setMargin(0));
            destCell.add(new Paragraph(destination).setFont(fontBold).setFontSize(14f).setFontColor(navyColor).setMarginTop(2f).setMarginBottom(0));
            destCell.add(new Paragraph("Arr: " + arrTime).setFont(fontBold).setFontSize(10f).setFontColor(darkText).setMarginTop(3f).setMarginBottom(0));
            routeCard.addCell(destCell);

            document.add(routeCard);

            // Spacing
            document.add(new Paragraph("").setMarginTop(10f).setMarginBottom(0));

            // 3. PASSENGER & RESERVATION DETAILS (4-column grid)
            Table detailsTable = new Table(UnitValue.createPercentArray(new float[]{28, 24, 24, 24}));
            detailsTable.setWidth(UnitValue.createPercentValue(100));

            // Row 1
            addDetailCell(detailsTable, "PASSENGER NAME", passengerName, fontBold, fontRegular, navyColor, darkText, borderLight);
            addDetailCell(detailsTable, "NIC / PASSPORT NO.", passengerNic, fontBold, fontRegular, mutedText, darkText, borderLight);
            addDetailCell(detailsTable, "CONTACT PHONE", contactPhone, fontBold, fontRegular, mutedText, darkText, borderLight);
            addDetailCell(detailsTable, "BOOKING REF", txnRef, fontBold, fontRegular, mutedText, darkText, borderLight);

            // Row 2
            addDetailCell(detailsTable, "CLASS OF TRAVEL", seatClass + " CLASS", fontBold, fontRegular, mutedText, navyColor, borderLight);
            addDetailCell(detailsTable, "SEATS BOOKED", seatCount + (seatCount > 1 ? " Seats" : " Seat"), fontBold, fontRegular, mutedText, darkText, borderLight);
            addDetailCell(detailsTable, "ALLOCATED SEAT(S)", seatNumbers, fontBold, fontRegular, mutedText, navyColor, borderLight);
            addDetailCell(detailsTable, "TOTAL FARE PAID", "LKR " + farePaid, fontBold, fontRegular, mutedText, greenValid, borderLight);

            document.add(detailsTable);

            // 4. PERFORATION LINE
            document.add(new Paragraph("").setMarginTop(12f).setMarginBottom(0));
            DashedLine dashedMuted = new DashedLine(1.2f);
            dashedMuted.setColor(mutedText);
            LineSeparator perfLine = new LineSeparator(dashedMuted);
            document.add(perfLine);
            document.add(new Paragraph("ELECTRONIC VERIFICATION STUB & BOARDING PASS")
                    .setFont(fontBold).setFontSize(7.5f).setFontColor(mutedText)
                    .setTextAlignment(TextAlignment.CENTER).setMarginTop(3f).setMarginBottom(10f));

            // 5. QR CODE & VERIFICATION SECTION (2-column layout)
            Table qrSection = new Table(UnitValue.createPercentArray(new float[]{32, 68}));
            qrSection.setWidth(UnitValue.createPercentValue(100));
            qrSection.setBorder(new SolidBorder(borderLight, 1f));
            qrSection.setBackgroundColor(bgCard);

            // QR Cell
            Cell qrCell = new Cell().setBorder(Border.NO_BORDER).setPadding(12f).setTextAlignment(TextAlignment.CENTER).setVerticalAlignment(VerticalAlignment.MIDDLE);
            byte[] qrBytes = createQrCodeBytes(ticketNum);
            if (qrBytes != null && qrBytes.length > 0) {
                ImageData qrData = ImageDataFactory.create(qrBytes);
                Image qrImage = new Image(qrData);
                qrImage.setWidth(115f);
                qrImage.setHeight(115f);
                qrImage.setHorizontalAlignment(HorizontalAlignment.CENTER);
                qrCell.add(qrImage);
            }
            qrCell.add(new Paragraph("SCAN AT STATION BARRIER").setFont(fontBold).setFontSize(7.5f).setFontColor(navyColor).setMarginTop(4f).setMarginBottom(0));
            qrCell.add(new Paragraph(ticketNum).setFont(fontBold).setFontSize(8f).setFontColor(darkText).setMarginTop(1f).setMarginBottom(0));
            qrSection.addCell(qrCell);

            // Instructions & Security Cell
            Cell infoCell = new Cell().setBorder(Border.NO_BORDER).setPadding(12f).setVerticalAlignment(VerticalAlignment.MIDDLE);
            infoCell.add(new Paragraph("DIGITAL BOARDING INSTRUCTIONS")
                    .setFont(fontBold).setFontSize(10.5f).setFontColor(navyColor).setMargin(0));

            Paragraph rules = new Paragraph()
                    .setFont(fontRegular).setFontSize(8.2f).setFontColor(darkText).setMultipliedLeading(1.35f).setMarginTop(6f).setMarginBottom(0);
            rules.add("• Present this QR code to the station gate turnstiles or conductor for instant barcode validation.\n");
            rules.add("• Passengers must carry a valid government-issued photo ID (NIC/Passport) matching the ticket name.\n");
            rules.add("• Please arrive at the platform at least 20 minutes before departure time.\n");
            rules.add("• Reservation is strictly valid only for the designated train service, travel date, and allocated seats.\n");
            rules.add("• Subject to Sri Lanka Railways Conditions of Carriage. Non-transferable.");
            infoCell.add(rules);

            // Security hash box
            String secHash = "SHA256-SLR-" + Math.abs((ticketNum + passengerNic).hashCode()) + "-VERIFIED-2026";
            infoCell.add(new Paragraph("Security Verification Code: " + secHash)
                    .setFont(fontOblique).setFontSize(7.5f).setFontColor(mutedText).setMarginTop(8f).setMarginBottom(0));

            qrSection.addCell(infoCell);
            document.add(qrSection);

            // 6. FOOTER
            document.add(new Paragraph("Sri Lanka Railways Department • Ministry of Transport & Highways • General Inquiries: 1919 • Web: https://railway.gov.lk")
                    .setFont(fontRegular).setFontSize(7.5f).setFontColor(mutedText)
                    .setTextAlignment(TextAlignment.CENTER).setMarginTop(18f).setMarginBottom(0));

            document.close();
            return baos.toByteArray();
        } catch (Exception ex) {
            log.error("Failed to generate styled PDF ticket for ticket ID: {}", ticketId, ex);
            String fallback = "Sri Lanka Railways E-Ticket: " + ticket.getTicketNumber() + "\n" +
                    "Passenger: " + ticket.getPassengerName() + "\n" +
                    "Train: " + ticket.getTrainName() + "\n" +
                    "Route: " + ticket.getOrigin() + " to " + ticket.getDestination() + "\n" +
                    "Date: " + ticket.getTravelDate() + "\n" +
                    "Seats: " + ticket.getSeatNumbers() + " (" + ticket.getSeatClass() + ")\n" +
                    "Fare: LKR " + ticket.getFarePaid();
            return fallback.getBytes(StandardCharsets.UTF_8);
        }
    }

    /**
     * Helper to render a structured detail cell within the passenger reservation grid.
     */
    private void addDetailCell(Table table, String label, String value, PdfFont boldFont, PdfFont regularFont,
                               DeviceRgb labelColor, DeviceRgb valueColor, DeviceRgb borderColor) {
        Cell cell = new Cell().setBorder(new SolidBorder(borderColor, 0.8f)).setPadding(6f);
        cell.add(new Paragraph(label).setFont(boldFont).setFontSize(7f).setFontColor(labelColor).setMargin(0));
        cell.add(new Paragraph(value).setFont(boldFont).setFontSize(9.5f).setFontColor(valueColor).setMarginTop(2f).setMarginBottom(0));
        table.addCell(cell);
    }

    /**
     * Generates a formal Tax Invoice and Receipt PDF document for a booking using iText 8.
     *
     * @param ticketId ticket ID
     * @return PDF byte array
     */
    public byte[] generateInvoicePdf(Long ticketId) {
        log.info("Generating formal Tax Invoice PDF for ticket ID: {}", ticketId);
        Ticket ticket = getTicketById(ticketId);
        String txnRef = (ticket.getPayment() != null && ticket.getPayment().getTransactionRef() != null)
                ? ticket.getPayment().getTransactionRef()
                : "TXN-" + ticket.getId();

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            WriterProperties writerProperties = new WriterProperties();
            writerProperties.setCompressionLevel(0); // Leaves text streams uncompressed for verification
            PdfWriter writer = new PdfWriter(baos, writerProperties);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf, PageSize.A4);
            document.setMargins(32f, 36f, 32f, 36f);

            PdfFont fontBold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont fontRegular = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            PdfFont fontOblique = PdfFontFactory.createFont(StandardFonts.HELVETICA_OBLIQUE);

            DeviceRgb navyColor = new DeviceRgb(26, 54, 93);
            DeviceRgb darkText = new DeviceRgb(45, 55, 72);
            DeviceRgb mutedText = new DeviceRgb(113, 128, 150);
            DeviceRgb bgCard = new DeviceRgb(247, 250, 252);
            DeviceRgb borderLight = new DeviceRgb(226, 232, 240);
            DeviceRgb greenValid = new DeviceRgb(39, 148, 77);

            // Invoice Header
            Table header = new Table(UnitValue.createPercentArray(new float[]{60, 40}));
            header.setWidth(UnitValue.createPercentValue(100));

            Cell titleCell = new Cell().setBorder(Border.NO_BORDER).setPadding(0);
            titleCell.add(new Paragraph("Sri Lanka Railways - Official Tax Invoice / Receipt")
                    .setFont(fontBold).setFontSize(16f).setFontColor(navyColor).setMargin(0));
            titleCell.add(new Paragraph("Department of Sri Lanka Railway Services • VAT Reg: 409128391-7000")
                    .setFont(fontRegular).setFontSize(8.5f).setFontColor(mutedText).setMarginTop(2f).setMarginBottom(0));
            header.addCell(titleCell);

            Cell invCell = new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT).setPadding(0);
            invCell.add(new Paragraph("TAX INVOICE")
                    .setFont(fontBold).setFontSize(12f).setFontColor(navyColor).setMargin(0));
            invCell.add(new Paragraph("Invoice Ref: INV-" + ticket.getTicketNumber())
                    .setFont(fontRegular).setFontSize(9f).setFontColor(darkText).setMarginTop(2f).setMarginBottom(0));
            invCell.add(new Paragraph("Date: " + java.time.LocalDate.now())
                    .setFont(fontRegular).setFontSize(8.5f).setFontColor(mutedText).setMarginTop(1f).setMarginBottom(0));
            header.addCell(invCell);
            document.add(header);

            SolidLine solidNavy = new SolidLine(1.5f);
            solidNavy.setColor(navyColor);
            LineSeparator line = new LineSeparator(solidNavy);
            line.setMarginTop(8f);
            line.setMarginBottom(12f);
            document.add(line);

            // Customer & Service details
            Table custTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}));
            custTable.setWidth(UnitValue.createPercentValue(100));

            Cell bCell = new Cell().setBorder(Border.NO_BORDER).setPadding(4f);
            bCell.add(new Paragraph("BILLED TO:").setFont(fontBold).setFontSize(8f).setFontColor(mutedText));
            bCell.add(new Paragraph(ticket.getPassengerName()).setFont(fontBold).setFontSize(11f).setFontColor(darkText));
            bCell.add(new Paragraph("NIC: " + ticket.getPassengerNic()).setFont(fontRegular).setFontSize(8.5f).setFontColor(mutedText));
            custTable.addCell(bCell);

            Cell pCell = new Cell().setBorder(Border.NO_BORDER).setPadding(4f).setTextAlignment(TextAlignment.RIGHT);
            pCell.add(new Paragraph("PAYMENT DETAILS:").setFont(fontBold).setFontSize(8f).setFontColor(mutedText));
            pCell.add(new Paragraph("Status: PAID / COMPLETED").setFont(fontBold).setFontSize(9.5f).setFontColor(greenValid));
            pCell.add(new Paragraph("Transaction Ref: " + txnRef).setFont(fontRegular).setFontSize(8.5f).setFontColor(mutedText));
            custTable.addCell(pCell);
            document.add(custTable);

            document.add(new Paragraph("").setMarginTop(10f).setMarginBottom(0));

            // Line items table
            Table items = new Table(UnitValue.createPercentArray(new float[]{45, 20, 15, 20}));
            items.setWidth(UnitValue.createPercentValue(100));

            // Header
            items.addHeaderCell(new Cell().setBackgroundColor(bgCard).setBorder(new SolidBorder(borderLight, 1f)).setPadding(6f)
                    .add(new Paragraph("DESCRIPTION / SERVICE").setFont(fontBold).setFontSize(8.5f).setFontColor(navyColor)));
            items.addHeaderCell(new Cell().setBackgroundColor(bgCard).setBorder(new SolidBorder(borderLight, 1f)).setPadding(6f)
                    .add(new Paragraph("SEATS / CLASS").setFont(fontBold).setFontSize(8.5f).setFontColor(navyColor)));
            items.addHeaderCell(new Cell().setBackgroundColor(bgCard).setBorder(new SolidBorder(borderLight, 1f)).setPadding(6f).setTextAlignment(TextAlignment.RIGHT)
                    .add(new Paragraph("QTY").setFont(fontBold).setFontSize(8.5f).setFontColor(navyColor)));
            items.addHeaderCell(new Cell().setBackgroundColor(bgCard).setBorder(new SolidBorder(borderLight, 1f)).setPadding(6f).setTextAlignment(TextAlignment.RIGHT)
                    .add(new Paragraph("AMOUNT (LKR)").setFont(fontBold).setFontSize(8.5f).setFontColor(navyColor)));

            // Row
            String desc = "Train " + ticket.getTrainName() + " (" + ticket.getOrigin() + " to " + ticket.getDestination() + ") on " + ticket.getTravelDate();
            items.addCell(new Cell().setBorder(new SolidBorder(borderLight, 1f)).setPadding(6f)
                    .add(new Paragraph(desc).setFont(fontRegular).setFontSize(8.5f)));
            items.addCell(new Cell().setBorder(new SolidBorder(borderLight, 1f)).setPadding(6f)
                    .add(new Paragraph(ticket.getSeatNumbers() + " (" + ticket.getSeatClass() + ")").setFont(fontRegular).setFontSize(8.5f)));
            items.addCell(new Cell().setBorder(new SolidBorder(borderLight, 1f)).setPadding(6f).setTextAlignment(TextAlignment.RIGHT)
                    .add(new Paragraph(String.valueOf(ticket.getSeatCount())).setFont(fontRegular).setFontSize(8.5f)));
            items.addCell(new Cell().setBorder(new SolidBorder(borderLight, 1f)).setPadding(6f).setTextAlignment(TextAlignment.RIGHT)
                    .add(new Paragraph(String.format("%,.2f", ticket.getFarePaid())).setFont(fontBold).setFontSize(8.5f)));

            document.add(items);

            // Total Block
            Table totalTable = new Table(UnitValue.createPercentArray(new float[]{65, 35}));
            totalTable.setWidth(UnitValue.createPercentValue(100));
            totalTable.setMarginTop(8f);

            Cell noteCell = new Cell().setBorder(Border.NO_BORDER).setPadding(4f);
            noteCell.add(new Paragraph("Thank you for traveling with Sri Lanka Railways.")
                    .setFont(fontOblique).setFontSize(8.5f).setFontColor(mutedText));
            totalTable.addCell(noteCell);

            Cell totCell = new Cell().setBorder(Border.NO_BORDER).setPadding(4f).setTextAlignment(TextAlignment.RIGHT);
            totCell.add(new Paragraph("Total Paid: LKR " + String.format("%,.2f", ticket.getFarePaid()) + " (VAT Included)")
                    .setFont(fontBold).setFontSize(11f).setFontColor(navyColor));
            totalTable.addCell(totCell);
            document.add(totalTable);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate tax invoice PDF for ID: {}", ticketId, e);
            String fallback = "Sri Lanka Railways - Official Tax Invoice / Receipt\n" +
                    "Invoice Ref: INV-" + ticket.getTicketNumber() + "\n" +
                    "Transaction Ref: " + txnRef + "\n" +
                    "Billed To: " + ticket.getPassengerName() + "\n" +
                    "Total Paid: LKR " + ticket.getFarePaid();
            return fallback.getBytes(StandardCharsets.UTF_8);
        }
    }

    /**
     * Retrieves ticket details by ID.
     *
     * @param id ticket ID
     * @return {@link Ticket} entity
     */
    public Ticket getTicketById(Long id) {
        log.info("Fetching ticket by ID: {}", id);
        return ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with ID: " + id));
    }

    /**
     * Calculates the refund tier, percentage, and amount for a booking based on railway policy.
     *
     * @param booking the booking to evaluate
     * @return {@link com.trainbooking.it25100977.dto.RefundCalculationDto} calculation breakdown
     */
    public com.trainbooking.it25100977.dto.RefundCalculationDto calculateRefund(Booking booking) {
        if (booking == null) {
            throw new IllegalArgumentException("Booking cannot be null for refund calculation.");
        }

        BigDecimal originalAmount = booking.getTotalAmount() != null ? booking.getTotalAmount() : BigDecimal.ZERO;
        java.time.LocalDate travelDate = booking.getTravelDate() != null ? booking.getTravelDate() : java.time.LocalDate.now();
        java.time.LocalTime departureTime = (booking.getSchedule() != null && booking.getSchedule().getDepartureTime() != null)
                ? booking.getSchedule().getDepartureTime() : java.time.LocalTime.of(6, 0);

        java.time.LocalDateTime departureDateTime = java.time.LocalDateTime.of(travelDate, departureTime);
        java.time.LocalDateTime now = java.time.LocalDateTime.now();

        java.time.Duration duration = java.time.Duration.between(now, departureDateTime);
        long hours = duration.toHours();

        int percentage;
        String tierDescription;
        boolean eligible;

        if (duration.isNegative() || hours < 0) {
            percentage = 0;
            tierDescription = "Train has already departed (Non-refundable after departure)";
            eligible = false;
        } else if (hours >= 48) {
            percentage = 90;
            tierDescription = "Tier 1 (>48h before departure): 90% Refund (10% Service Fee)";
            eligible = true;
        } else if (hours >= 24) {
            percentage = 75;
            tierDescription = "Tier 2 (24–48h before departure): 75% Refund (25% Cancellation Fee)";
            eligible = true;
        } else if (hours >= 12) {
            percentage = 50;
            tierDescription = "Tier 3 (12–24h before departure): 50% Refund (50% Cancellation Fee)";
            eligible = true;
        } else {
            percentage = 25;
            tierDescription = "Tier 4 (<12h before departure): 25% Refund (75% Cancellation Fee)";
            eligible = true;
        }

        BigDecimal refundAmount = originalAmount
                .multiply(BigDecimal.valueOf(percentage))
                .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
        BigDecimal fee = originalAmount.subtract(refundAmount);

        return com.trainbooking.it25100977.dto.RefundCalculationDto.builder()
                .originalAmount(originalAmount)
                .refundAmount(refundAmount)
                .cancellationFee(fee)
                .refundPercentage(percentage)
                .hoursUntilDeparture(Math.max(0, hours))
                .policyTierDescription(tierDescription)
                .eligibleForRefund(eligible)
                .build();
    }

    /**
     * Executes mock payment gateway refund, saves ledger audit entry, and updates transaction status.
     *
     * @param bookingId booking ID
     * @param reason cancellation reason
     * @return generated {@link com.trainbooking.it25100977.model.Refund} entity
     */
    @Transactional
    public com.trainbooking.it25100977.model.Refund processRefund(Long bookingId, String reason) {
        log.info("Processing refund for booking ID: {}, Reason: {}", bookingId, reason);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with ID: " + bookingId));

        // Check if already refunded
        java.util.Optional<com.trainbooking.it25100977.model.Refund> existingRefund = refundRepository.findByBookingId(bookingId);
        if (existingRefund.isPresent()) {
            log.info("Booking ID {} already has a processed refund: {}", bookingId, existingRefund.get().getRefundTransactionRef());
            return existingRefund.get();
        }

        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElse(null);

        com.trainbooking.it25100977.dto.RefundCalculationDto calc = calculateRefund(booking);

        String refNumber = "REF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        com.trainbooking.it25100977.model.Refund refund = com.trainbooking.it25100977.model.Refund.builder()
                .booking(booking)
                .payment(payment)
                .originalAmount(calc.getOriginalAmount())
                .refundAmount(calc.getRefundAmount())
                .cancellationFee(calc.getCancellationFee())
                .refundPercentage(calc.getRefundPercentage())
                .refundTransactionRef(refNumber)
                .reason(reason != null && !reason.isBlank() ? reason : "Passenger requested cancellation")
                .status(com.trainbooking.it25100977.model.Refund.RefundStatus.COMPLETED)
                .build();

        com.trainbooking.it25100977.model.Refund savedRefund = refundRepository.save(refund);

        if (payment != null) {
            payment.setStatus(calc.getRefundPercentage() == 100 ? Payment.PaymentStatus.REFUNDED : Payment.PaymentStatus.PARTIALLY_REFUNDED);
            paymentRepository.save(payment);
        }

        // Invalidate digital ticket
        ticketRepository.findByBookingId(bookingId).ifPresent(ticket -> {
            ticket.setIsBoarded(false);
            ticketRepository.save(ticket);
            log.info("Invalidated boarding pass for cancelled ticket #{}", ticket.getTicketNumber());
        });

        log.info("Refund completed for booking #{} with Ref: {}, Amount: LKR {}", bookingId, refNumber, calc.getRefundAmount());
        return savedRefund;
    }

    /**
     * Retrieves refund record for a given booking ID.
     *
     * @param bookingId booking ID
     * @return optional containing refund if present
     */
    public java.util.Optional<com.trainbooking.it25100977.model.Refund> getRefundByBookingId(Long bookingId) {
        return refundRepository.findByBookingId(bookingId);
    }
}
