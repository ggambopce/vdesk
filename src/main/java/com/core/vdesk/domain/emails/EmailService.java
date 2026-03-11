package com.core.vdesk.domain.emails;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;

    public void sendVerificationCode(String toEmail, String code) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(toEmail);
        msg.setSubject("[OneDesk] 이메일 인증 코드");  // 메일 제목
        // 메일 본문 내용
        msg.setText("""                                 
                아래 인증코드를 입력해 인증을 완료해주세요.
                
                인증코드: %s
                """.formatted(code));
        mailSender.send(msg);
    }
}

