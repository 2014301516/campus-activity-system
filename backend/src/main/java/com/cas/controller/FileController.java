package com.cas.controller;

import com.cas.common.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class FileController {

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    @PostMapping("/upload")
    public Result<Map<String, String>> upload(@RequestParam("file") MultipartFile file) throws Exception {
        if (file.isEmpty()) return Result.error("文件为空");

        String original = file.getOriginalFilename();
        String ext = original.substring(original.lastIndexOf("."));
        String fileName = UUID.randomUUID().toString() + ext;

        String absolutePath = new File(uploadDir).getAbsolutePath();
        File dir = new File(absolutePath);
        if (!dir.exists()) dir.mkdirs();

        file.transferTo(new File(dir, fileName));

        Map<String, String> result = new HashMap<>();
        result.put("url", "/" + uploadDir + "/" + fileName);
        return Result.success(result);
    }
}
