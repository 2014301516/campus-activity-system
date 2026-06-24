package com.cas.controller;

import com.cas.common.Result;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileOutputStream;
import java.util.*;

@RestController
@RequestMapping("/api")
public class FileController {

    /** 接收 base64 图片，存到 uploads 目录，返回 URL */
    @PostMapping("/upload-base64")
    public Result<Map<String, String>> uploadBase64(@RequestBody Map<String, String> body) throws Exception {
        String base64 = body.get("file");
        if (base64 == null || base64.isEmpty()) return Result.error("文件为空");

        // 去掉 data:image/png;base64, 前缀
        if (base64.contains(",")) {
            base64 = base64.substring(base64.indexOf(",") + 1);
        }

        byte[] bytes = Base64.getDecoder().decode(base64);
        String ext = ".jpg";
        String fileName = UUID.randomUUID().toString() + ext;

        File dir = new File(new File("uploads").getAbsolutePath());
        if (!dir.exists()) dir.mkdirs();

        try (FileOutputStream fos = new FileOutputStream(new File(dir, fileName))) {
            fos.write(bytes);
        }

        Map<String, String> result = new HashMap<>();
        result.put("url", "http://localhost:8080/uploads/" + fileName);
        return Result.success(result);
    }
}
