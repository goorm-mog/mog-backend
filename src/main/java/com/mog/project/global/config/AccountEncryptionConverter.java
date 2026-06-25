package com.mog.project.global.config;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

@Component
@Converter
public class AccountEncryptionConverter implements AttributeConverter<String, String> {

    // AES: 암호화 알고리즘, GCM: 암호화 모드
    private static final String ALGORITHM = "AES/GCM/NoPadding";

    // IV(Initialization Vector): 같은 값을 저장하더라도 각각 다른 암호문이 생기게 하기 위한 임의의 값으로 생각하면 됩니다.
    // 매 암호화마다 앞에 붙여 사용하는 salt로 생각하시면 됩니다!
    private static final int GCM_IV_LENGTH = 12;

    // GCM 인증 태그
    private static final int GCM_TAG_LENGTH = 128;

    // 시크릿키
    private final SecretKeySpec secretKey;

    // 생성자 주입
    public AccountEncryptionConverter(@Value("${encryption.secret-key}") String base64Key) {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    // JPA에서 DB에 저장하기 전에 호출
    @Override
    public String convertToDatabaseColumn(String plainText) {

        // 계좌번호가 없으면 그대로 null 저장
        if (plainText == null) return null;
        try {
            // 매번 암호화를 할때마다 새로운 랜덤 IV를 생성
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            // AES-GCM 암호화를 시작
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            // 평문을 받아서 암호화를 진행
            byte[] encrypted = cipher.doFinal(plainText.getBytes());

            // [IV 12byte][암호문] 순서로 병합
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

            // Base64로 인코딩해서 DB에 문자열로 저장
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("계좌번호 암호화 실패", e);
        }
    }

    // JPA가 DB에서 읽은 후 호출
    @Override
    public String convertToEntityAttribute(String cipherText) {

        // null이면 그대로 반환
        if (cipherText == null) return null;


        try {

            // Base64 디코딩을 진행
            byte[] combined = Base64.getDecoder().decode(cipherText);

            // 앞 12byte를 IV로 분리
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, iv.length);

            // 나머지를 암호문으로 분리
            byte[] encrypted = new byte[combined.length - iv.length];
            System.arraycopy(combined, iv.length, encrypted, 0, encrypted.length);

            // 암호화 할 때 사용한 동일한 IV와 키로 복호화를 진행
            // GCM 태그를 통해 검증을 하기 때문에, 변조된 데이터의 경우 예외를 반환
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            // 복호화된 데이터를 반환
            return new String(cipher.doFinal(encrypted));
        } catch (Exception e) {
            throw new RuntimeException("계좌번호 복호화 실패", e);
        }
    }


}
