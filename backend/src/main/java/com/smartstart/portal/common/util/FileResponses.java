package com.smartstart.portal.common.util;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;

public final class FileResponses {

    private FileResponses() {
    }

    public static ResponseEntity<byte[]> attachment(byte[] body, String filename, String contentType) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
                .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
                .contentType(MediaType.parseMediaType(contentType))
                .body(body);
    }

    public static ResponseEntity<byte[]> csv(String body, String filename) {
        // Prefix a BOM so Excel opens UTF-8 correctly.
        byte[] bytes = ("﻿" + body).getBytes(StandardCharsets.UTF_8);
        return attachment(bytes, filename, "text/csv;charset=UTF-8");
    }

    public static ResponseEntity<byte[]> excel(byte[] body, String filename) {
        return attachment(body, filename, ExcelSupport.CONTENT_TYPE);
    }
}
