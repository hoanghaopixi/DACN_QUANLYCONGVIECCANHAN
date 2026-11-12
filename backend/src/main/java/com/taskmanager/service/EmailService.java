package com.taskmanager.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    private final org.springframework.beans.factory.ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    public boolean sendOtpEmail(String toEmail, String otpCode) {
        return sendOtpEmail(toEmail, otpCode, null);
    }

    /**
     * Gửi mã OTP xác thực qua email với chuẩn văn bản doanh nghiệp trang trọng,
     * thiết kế tinh tế, không lạm dụng icon hay phong cách AI.
     */
    public boolean sendOtpEmail(String toEmail, String otpCode, String recipientName) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null || fromEmail == null || fromEmail.isBlank()) {
            log.warn("=== [SMARTTASK EMAIL SERVICE - LOCAL MOCK] ===");
            log.warn("Chưa cấu hình tài khoản SMTP (spring.mail.username).");
            log.warn("Mã OTP gửi đến [{}]: >>> {} <<< (Hiệu lực: 5 phút)", toEmail, otpCode);
            log.warn("==============================================");
            return false;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, "SmartTask AI Security");
            helper.setTo(toEmail);
            helper.setSubject("[SmartTask AI] Mã xác thực bảo mật tài khoản (OTP): " + otpCode);

            String htmlContent = buildOtpHtmlContent(otpCode, recipientName);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Đã gửi thành công mã OTP đến email {}", toEmail);
            return true;
        } catch (Exception e) {
            log.warn("Gửi email qua SMTP thất bại: {}. Kích hoạt chế độ ghi nhận OTP cục bộ.", e.getMessage());
            log.warn("=== [SMARTTASK EMAIL SERVICE - LOCAL FALLBACK] ===");
            log.warn("Mã OTP gửi đến [{}]: >>> {} <<< (Hiệu lực: 5 phút)", toEmail, otpCode);
            log.warn("==================================================");
            return false;
        }
    }

    /**
     * Gửi mã OTP xác thực đăng ký tài khoản mới qua email.
     */
    public boolean sendRegisterOtpEmail(String toEmail, String otpCode, String recipientName) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null || fromEmail == null || fromEmail.isBlank()) {
            log.warn("=== [SMARTTASK EMAIL SERVICE - LOCAL MOCK (REGISTER)] ===");
            log.warn("Chưa cấu hình tài khoản SMTP (spring.mail.username).");
            log.warn("Mã OTP Đăng ký gửi đến [{}]: >>> {} <<< (Hiệu lực: 5 phút)", toEmail, otpCode);
            log.warn("=========================================================");
            return false;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, "SmartTask AI Security");
            helper.setTo(toEmail);
            helper.setSubject("[SmartTask AI] Mã xác thực đăng ký tài khoản (OTP): " + otpCode);

            String htmlContent = buildRegisterOtpHtmlContent(otpCode, recipientName);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Đã gửi thành công mã OTP đăng ký đến email {}", toEmail);
            return true;
        } catch (Exception e) {
            log.warn("Gửi email qua SMTP thất bại: {}. Kích hoạt chế độ ghi nhận OTP cục bộ.", e.getMessage());
            log.warn("=== [SMARTTASK EMAIL SERVICE - LOCAL FALLBACK (REGISTER)] ===");
            log.warn("Mã OTP Đăng ký gửi đến [{}]: >>> {} <<< (Hiệu lực: 5 phút)", toEmail, otpCode);
            log.warn("=============================================================");
            return false;
        }
    }

    private String buildOtpHtmlContent(String otpCode, String recipientName) {
        String greetingName = (recipientName != null && !recipientName.trim().isEmpty())
                ? recipientName.trim()
                : "Quý người dùng";

        return """
            <!DOCTYPE html>
            <html lang="vi">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>Mã xác thực bảo mật SmartTask AI</title>
              <style>
                body {
                  margin: 0;
                  padding: 24px;
                  background-color: #F8FAFC;
                  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
                  color: #0F172A;
                  -webkit-font-smoothing: antialiased;
                }
                .wrapper {
                  max-width: 560px;
                  margin: 0 auto;
                  background-color: #FFFFFF;
                  border: 1px solid #E2E8F0;
                  border-radius: 12px;
                  overflow: hidden;
                  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.04);
                }
                .header-bar {
                  background-color: #0F172A;
                  padding: 24px 32px;
                  border-bottom: 2px solid #2563EB;
                }
                .system-name {
                  font-size: 18px;
                  font-weight: 700;
                  color: #FFFFFF;
                  letter-spacing: -0.01em;
                  margin: 0;
                }
                .system-subtitle {
                  font-size: 12px;
                  color: #94A3B8;
                  margin: 4px 0 0 0;
                  text-transform: uppercase;
                  letter-spacing: 0.5px;
                }
                .content {
                  padding: 32px;
                }
                .salutation {
                  font-size: 15px;
                  font-weight: 600;
                  color: #0F172A;
                  margin: 0 0 16px 0;
                }
                .paragraph {
                  font-size: 14px;
                  line-height: 1.65;
                  color: #334155;
                  margin: 0 0 16px 0;
                }
                .otp-panel {
                  background-color: #F1F5F9;
                  border: 1px solid #CBD5E1;
                  border-radius: 8px;
                  padding: 20px;
                  text-align: center;
                  margin: 24px 0;
                }
                .otp-label {
                  font-size: 12px;
                  font-weight: 600;
                  text-transform: uppercase;
                  letter-spacing: 1px;
                  color: #64748B;
                  margin-bottom: 8px;
                }
                .otp-code {
                  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, Courier, monospace;
                  font-size: 36px;
                  font-weight: 800;
                  letter-spacing: 10px;
                  color: #1E293B;
                  line-height: 1;
                }
                .notice-box {
                  border-left: 3px solid #2563EB;
                  background-color: #F8FAFC;
                  padding: 14px 18px;
                  margin: 24px 0;
                }
                .notice-title {
                  font-size: 13px;
                  font-weight: 700;
                  color: #0F172A;
                  margin: 0 0 6px 0;
                }
                .notice-list {
                  margin: 0;
                  padding-left: 18px;
                  font-size: 13px;
                  line-height: 1.6;
                  color: #475569;
                }
                .sign-off {
                  margin-top: 28px;
                  padding-top: 16px;
                  font-size: 14px;
                  line-height: 1.6;
                  color: #334155;
                }
                .footer {
                  background-color: #F8FAFC;
                  border-top: 1px solid #E2E8F0;
                  padding: 18px 32px;
                  text-align: center;
                  font-size: 12px;
                  line-height: 1.6;
                  color: #94A3B8;
                }
              </style>
            </head>
            <body>
              <div class="wrapper">
                <div class="header-bar">
                  <div class="system-name">SmartTask AI</div>
                  <div class="system-subtitle">Hệ thống Quản lý và Tự động Lập lịch Công việc Cá nhân</div>
                </div>

                <div class="content">
                  <p class="salutation">Kính gửi %s,</p>

                  <p class="paragraph">
                    Hệ thống SmartTask AI vừa tiếp nhận yêu cầu xác thực bảo mật nhằm hỗ trợ đặt lại mật khẩu cho tài khoản liên kết với địa chỉ email này.
                  </p>

                  <p class="paragraph">
                    Để xác nhận danh tính và tiếp tục quy trình thiết lập mật khẩu mới, Quý người dùng vui lòng sử dụng mã xác thực (OTP) dưới đây:
                  </p>

                  <div class="otp-panel">
                    <div class="otp-label">Mã xác thực một lần (OTP)</div>
                    <div class="otp-code">%s</div>
                  </div>

                  <div class="notice-box">
                    <div class="notice-title">Quy định an toàn và bảo mật:</div>
                    <ul class="notice-list">
                      <li>Mã xác thực này có hiệu lực trong vòng <strong>05 phút</strong> kể từ thời điểm phát hành.</li>
                      <li>Để bảo vệ an toàn cho tài khoản, tuyệt đối không cung cấp mã OTP này cho bất kỳ cá nhân nào khác.</li>
                      <li>Nếu Quý người dùng không gửi yêu cầu này, vui lòng bỏ qua thư hoặc thông báo ngay cho ban quản trị để được hỗ trợ kiểm tra an toàn.</li>
                    </ul>
                  </div>

                  <div class="sign-off">
                    Trân trọng cảm ơn,<br>
                    <strong>Ban Quản trị & Bộ phận Bảo mật Hệ thống SmartTask AI</strong>
                  </div>
                </div>

                <div class="footer">
                  Thông báo này được gửi tự động từ máy chủ SmartTask AI. Vui lòng không trả lời trực tiếp email này.<br>
                  &copy; 2026 SmartTask AI System. Bản quyền thuộc về Đồ án Chuyên ngành Quản lý Công việc Cá nhân.
                </div>
              </div>
            </body>
            </html>
            """.formatted(greetingName, otpCode);
    }

    private String buildRegisterOtpHtmlContent(String otpCode, String recipientName) {
        String greetingName = (recipientName != null && !recipientName.trim().isEmpty())
                ? recipientName.trim()
                : "Quý người dùng mới";

        return """
            <!DOCTYPE html>
            <html lang="vi">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>Mã xác thực đăng ký tài khoản SmartTask AI</title>
              <style>
                body {
                  margin: 0;
                  padding: 24px;
                  background-color: #F8FAFC;
                  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
                  color: #0F172A;
                  -webkit-font-smoothing: antialiased;
                }
                .wrapper {
                  max-width: 560px;
                  margin: 0 auto;
                  background-color: #FFFFFF;
                  border: 1px solid #E2E8F0;
                  border-radius: 12px;
                  overflow: hidden;
                  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.04);
                }
                .header-bar {
                  background-color: #0F172A;
                  padding: 24px 32px;
                  border-bottom: 2px solid #10B981;
                }
                .system-name {
                  font-size: 18px;
                  font-weight: 700;
                  color: #FFFFFF;
                  letter-spacing: -0.01em;
                  margin: 0;
                }
                .system-subtitle {
                  font-size: 12px;
                  color: #94A3B8;
                  margin: 4px 0 0 0;
                  text-transform: uppercase;
                  letter-spacing: 0.5px;
                }
                .content {
                  padding: 32px;
                }
                .salutation {
                  font-size: 15px;
                  font-weight: 600;
                  color: #0F172A;
                  margin: 0 0 16px 0;
                }
                .paragraph {
                  font-size: 14px;
                  line-height: 1.65;
                  color: #334155;
                  margin: 0 0 16px 0;
                }
                .otp-panel {
                  background-color: #ECFDF5;
                  border: 1px solid #A7F3D0;
                  border-radius: 8px;
                  padding: 20px;
                  text-align: center;
                  margin: 24px 0;
                }
                .otp-label {
                  font-size: 12px;
                  font-weight: 600;
                  text-transform: uppercase;
                  letter-spacing: 1px;
                  color: #059669;
                  margin-bottom: 8px;
                }
                .otp-code {
                  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, Courier, monospace;
                  font-size: 36px;
                  font-weight: 800;
                  letter-spacing: 10px;
                  color: #065F46;
                  line-height: 1;
                }
                .notice-box {
                  border-left: 3px solid #10B981;
                  background-color: #F8FAFC;
                  padding: 14px 18px;
                  margin: 24px 0;
                }
                .notice-title {
                  font-size: 13px;
                  font-weight: 700;
                  color: #0F172A;
                  margin: 0 0 6px 0;
                }
                .notice-list {
                  margin: 0;
                  padding-left: 18px;
                  font-size: 13px;
                  line-height: 1.6;
                  color: #475569;
                }
                .sign-off {
                  margin-top: 28px;
                  padding-top: 16px;
                  font-size: 14px;
                  line-height: 1.6;
                  color: #334155;
                }
                .footer {
                  background-color: #F8FAFC;
                  border-top: 1px solid #E2E8F0;
                  padding: 18px 32px;
                  text-align: center;
                  font-size: 12px;
                  line-height: 1.6;
                  color: #94A3B8;
                }
              </style>
            </head>
            <body>
              <div class="wrapper">
                <div class="header-bar">
                  <div class="system-name">SmartTask AI</div>
                  <div class="system-subtitle">Chào mừng bạn gia nhập hệ thống SmartTask AI</div>
                </div>

                <div class="content">
                  <p class="salutation">Xin chào %s,</p>

                  <p class="paragraph">
                    Cảm ơn bạn đã đăng ký tài khoản trên hệ thống Quản lý và Tự động Lập lịch Công việc Cá nhân <strong>SmartTask AI</strong>.
                  </p>

                  <p class="paragraph">
                    Để kích hoạt tài khoản và xác minh quyền sở hữu hòm thư email này, vui lòng nhập mã xác thực (OTP) 6 chữ số dưới đây vào biểu mẫu đăng ký:
                  </p>

                  <div class="otp-panel">
                    <div class="otp-label">Mã xác thực đăng ký (OTP)</div>
                    <div class="otp-code">%s</div>
                  </div>

                  <div class="notice-box">
                    <div class="notice-title">Lưu ý bảo mật:</div>
                    <ul class="notice-list">
                      <li>Mã OTP này có hiệu lực trong vòng <strong>05 phút</strong> kể từ khi phát hành.</li>
                      <li>Tuyệt đối không chia sẻ mã này cho bất kỳ ai để đảm bảo an toàn tài khoản.</li>
                      <li>Nếu bạn không thực hiện yêu cầu đăng ký này, vui lòng bỏ qua thư này.</li>
                    </ul>
                  </div>

                  <div class="sign-off">
                    Trân trọng,<br>
                    <strong>Đội ngũ Phát triển SmartTask AI</strong>
                  </div>
                </div>

                <div class="footer">
                  Email này được gửi tự động. Vui lòng không trả lời trực tiếp.<br>
                  &copy; 2026 SmartTask AI System. Bản quyền thuộc về Đồ án Chuyên ngành Quản lý Công việc Cá nhân.
                </div>
              </div>
            </body>
            </html>
            """.formatted(greetingName, otpCode);
    }
}
