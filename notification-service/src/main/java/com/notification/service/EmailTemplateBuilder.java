package com.notification.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * EmailTemplateBuilder — builds inline-styled HTML email bodies.
 *
 * All emails share a common branded layout:
 *  - Capsule Pharmacy header (blue banner)
 *  - Content body with relevant info
 *  - CTA button where applicable
 *  - Footer with support contact
 *
 * Uses inline CSS only (no external stylesheets) — maximum email client compatibility.
 */
@Component
public class EmailTemplateBuilder {

    private static final String PRIMARY_COLOR = "#1A73E8";
    private static final String SUCCESS_COLOR = "#34A853";
    private static final String DANGER_COLOR  = "#EA4335";
    private static final String WARNING_COLOR = "#FBBC04";
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    // ── Wrapper ───────────────────────────────────────────────────────────────

    private String wrap(String title, String content) {
        return """
            <!DOCTYPE html>
            <html>
            <body style="margin:0;padding:0;font-family:Arial,sans-serif;background:#f5f5f5;">
              <table width="100%%" cellpadding="0" cellspacing="0">
                <tr><td align="center" style="padding:20px 0;">
                  <table width="600" cellpadding="0" cellspacing="0"
                         style="background:#ffffff;border-radius:8px;overflow:hidden;
                                box-shadow:0 2px 8px rgba(0,0,0,0.1);">
                    <!-- Header -->
                    <tr>
                      <td style="background:%s;padding:24px 32px;">
                        <h1 style="margin:0;color:#ffffff;font-size:22px;font-weight:bold;">
                          💊 Capsule Pharmacy
                        </h1>
                        <p style="margin:4px 0 0;color:rgba(255,255,255,0.85);font-size:14px;">%s</p>
                      </td>
                    </tr>
                    <!-- Body -->
                    <tr>
                      <td style="padding:32px;">%s</td>
                    </tr>
                    <!-- Footer -->
                    <tr>
                      <td style="background:#f8f9fa;padding:16px 32px;border-top:1px solid #e9ecef;">
                        <p style="margin:0;color:#6c757d;font-size:12px;">
                          Need help? Contact us at
                          <a href="mailto:support@capsulepharmacy.com"
                             style="color:%s;">support@capsulepharmacy.com</a>
                          <br>© 2025 Capsule Pharmacy. All rights reserved.
                        </p>
                      </td>
                    </tr>
                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """.formatted(PRIMARY_COLOR, title, content, PRIMARY_COLOR);
    }

    private String button(String text, String href, String color) {
        return """
            <a href="%s"
               style="display:inline-block;margin-top:16px;padding:12px 28px;
                      background:%s;color:#ffffff;text-decoration:none;
                      border-radius:6px;font-weight:bold;font-size:14px;">
              %s
            </a>
            """.formatted(href, color, text);
    }

    private String infoRow(String label, String value) {
        return """
            <tr>
              <td style="padding:8px 12px;font-weight:bold;color:#495057;
                         background:#f8f9fa;border:1px solid #dee2e6;width:40%%;">%s</td>
              <td style="padding:8px 12px;color:#212529;border:1px solid #dee2e6;">%s</td>
            </tr>
            """.formatted(label, value);
    }

    // ── Order Placed ──────────────────────────────────────────────────────────

    public String buildOrderPlacedEmail(String patientName, String orderNumber,
                                         BigDecimal total, String address) {
        String content = """
            <h2 style="color:#212529;margin-top:0;">Hi %s,</h2>
            <p style="color:#495057;">Your order has been placed successfully and is awaiting payment confirmation.</p>
            <table width="100%%" cellpadding="0" cellspacing="0"
                   style="border-collapse:collapse;margin:16px 0;">
              %s%s%s
            </table>
            <p style="color:#6c757d;font-size:13px;">
              Once payment is confirmed, we'll start processing your order. 
              Estimated delivery: <strong>3 business days</strong>.
            </p>
            %s
            """.formatted(
                patientName,
                infoRow("Order Number", "<strong>" + orderNumber + "</strong>"),
                infoRow("Total Amount", "₹" + total),
                infoRow("Delivery Address", address),
                button("Track Your Order", "http://capsulepharmacy.com/orders", PRIMARY_COLOR)
        );
        return wrap("Order Placed Successfully", content);
    }

    // ── Payment Confirmed ─────────────────────────────────────────────────────

    public String buildPaymentConfirmedEmail(String patientName, String orderNumber,
                                              BigDecimal amount, String transactionId) {
        String badge = "<span style=\"background:%s;color:#fff;padding:4px 10px;" +
                       "border-radius:4px;font-size:12px;font-weight:bold;\">CONFIRMED</span>"
                       .formatted(SUCCESS_COLOR);
        String content = """
            <h2 style="color:#212529;margin-top:0;">Hi %s,</h2>
            <p style="color:#495057;">
              Great news! Your payment has been received and your order is now confirmed. %s
            </p>
            <table width="100%%" cellpadding="0" cellspacing="0"
                   style="border-collapse:collapse;margin:16px 0;">
              %s%s%s
            </table>
            <p style="color:#6c757d;font-size:13px;">
              Your medicines are being packed and will be shipped soon. 
              You'll receive a notification once your order is shipped.
            </p>
            %s
            """.formatted(
                patientName, badge,
                infoRow("Order Number", orderNumber),
                infoRow("Amount Paid", "₹" + amount),
                infoRow("Transaction ID", transactionId),
                button("View Order Details", "http://capsulepharmacy.com/orders", SUCCESS_COLOR)
        );
        return wrap("Payment Confirmed ✅", content);
    }

    // ── Order Cancelled ───────────────────────────────────────────────────────

    public String buildOrderCancelledEmail(String patientName, String orderNumber, String reason) {
        String content = """
            <h2 style="color:#212529;margin-top:0;">Hi %s,</h2>
            <p style="color:#495057;">
              Your order has been cancelled. We're sorry for any inconvenience.
            </p>
            <table width="100%%" cellpadding="0" cellspacing="0"
                   style="border-collapse:collapse;margin:16px 0;">
              %s%s
            </table>
            <p style="color:#6c757d;font-size:13px;">
              If you paid for this order, a refund will be initiated within 5–7 business days.
            </p>
            %s
            """.formatted(
                patientName,
                infoRow("Order Number", orderNumber),
                infoRow("Reason", reason != null ? reason : "Not specified"),
                button("Browse Medicines", "http://capsulepharmacy.com/medicines", PRIMARY_COLOR)
        );
        return wrap("Order Cancelled", content);
    }

    // ── Appointment Booked ────────────────────────────────────────────────────

    public String buildAppointmentBookedEmail(String patientName, String doctorName,
                                               String specialization, LocalDateTime apptTime,
                                               String apptType) {
        String content = """
            <h2 style="color:#212529;margin-top:0;">Hi %s,</h2>
            <p style="color:#495057;">
              Your appointment has been booked successfully. Here are the details:
            </p>
            <table width="100%%" cellpadding="0" cellspacing="0"
                   style="border-collapse:collapse;margin:16px 0;">
              %s%s%s%s
            </table>
            <p style="color:#6c757d;font-size:13px;">
              You'll receive a reminder 24 hours before your appointment.
              Please be available at the scheduled time.
            </p>
            %s
            """.formatted(
                patientName,
                infoRow("Doctor", "Dr. " + doctorName),
                infoRow("Specialization", specialization),
                infoRow("Date & Time", apptTime.format(FORMATTER)),
                infoRow("Type", apptType),
                button("View Appointment", "http://capsulepharmacy.com/appointments", PRIMARY_COLOR)
        );
        return wrap("Appointment Confirmed 📅", content);
    }

    // ── Appointment Reminder ──────────────────────────────────────────────────

    public String buildAppointmentReminderEmail(String patientName, String doctorName,
                                                 String specialization, LocalDateTime apptTime,
                                                 String apptType) {
        String content = """
            <h2 style="color:#212529;margin-top:0;">Hi %s 👋</h2>
            <p style="color:#495057;">
              This is a friendly reminder that you have an appointment scheduled 
              <strong>tomorrow</strong>. Please be prepared.
            </p>
            <table width="100%%" cellpadding="0" cellspacing="0"
                   style="border-collapse:collapse;margin:16px 0;">
              %s%s%s%s
            </table>
            <p style="color:#6c757d;font-size:13px;">
              Need to reschedule? Please cancel at least 2 hours before your appointment.
            </p>
            %s
            """.formatted(
                patientName,
                infoRow("Doctor", "Dr. " + doctorName),
                infoRow("Specialization", specialization),
                infoRow("Date & Time", apptTime.format(FORMATTER)),
                infoRow("Type", apptType),
                button("View Appointment", "http://capsulepharmacy.com/appointments", WARNING_COLOR)
        );
        return wrap("Appointment Reminder ⏰", content);
    }

    // ── Appointment Cancelled ─────────────────────────────────────────────────

    public String buildAppointmentCancelledEmail(String patientName, String doctorName,
                                                  LocalDateTime apptTime, String reason) {
        String content = """
            <h2 style="color:#212529;margin-top:0;">Hi %s,</h2>
            <p style="color:#495057;">
              Your appointment has been cancelled.
            </p>
            <table width="100%%" cellpadding="0" cellspacing="0"
                   style="border-collapse:collapse;margin:16px 0;">
              %s%s%s
            </table>
            %s
            """.formatted(
                patientName,
                infoRow("Doctor", "Dr. " + doctorName),
                infoRow("Scheduled Time", apptTime.format(FORMATTER)),
                infoRow("Reason", reason != null ? reason : "Not specified"),
                button("Book New Appointment", "http://capsulepharmacy.com/doctors", PRIMARY_COLOR)
        );
        return wrap("Appointment Cancelled", content);
    }
}
