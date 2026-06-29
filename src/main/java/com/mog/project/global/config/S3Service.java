package com.mog.project.global.config;

import com.mog.project.global.exception.ErrorCode;
import com.mog.project.global.exception.GlobalException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3Service {
    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucket;


    @Value("${aws.s3.base-url}")
    private String baseUrl;

    public String upload(MultipartFile file, String folder) {

        // UUID를 통해 고유한 파일명을 생성하고, 같은 파일명의 중복방지를 함
        String fileName = folder + "/" + UUID.randomUUID() + "_" + file.getOriginalFilename();

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(fileName) // 저장 경로
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(request, RequestBody.fromBytes(file.getBytes())); // 파일 저장

        } catch (IOException e) {
            throw new GlobalException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        return baseUrl + "/" + fileName; // 저장된 주소 반환
    }

    public void delete(String s3Url) {

        // 파일 경로만 추출
        String key = s3Url.replace(baseUrl + "/", "");

        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        s3Client.deleteObject(request);
    }
}
