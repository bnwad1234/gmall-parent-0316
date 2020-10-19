package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.product.test.TestFdfs;
import org.csource.fastdfs.ClientGlobal;
import org.csource.fastdfs.StorageClient;
import org.csource.fastdfs.TrackerClient;
import org.csource.fastdfs.TrackerServer;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLDecoder;

@RestController
@RequestMapping("admin/product")

public class FileUploadApiController {

    @RequestMapping("fileUpload")
    public Result<String> fileUpload(MultipartFile file) throws Exception {
        // 获取fdfs的全局配置信息
        String path = TestFdfs.class.getClassLoader().getResource("tracker.conf").getPath();
        path = URLDecoder.decode(path,"utf-8");// 对结果进行编码，防止出现特殊字符
        ClientGlobal.init(path);

        // 获取tracker
        TrackerClient trackerClient = new TrackerClient();
        TrackerServer connection = trackerClient.getConnection();

        // 获取storage
        StorageClient storageClient = new StorageClient(connection, null);

        // 上传
        String originalFilename = file.getOriginalFilename();
        int i = originalFilename.lastIndexOf(".");
        String ext = originalFilename.substring(i + 1);
        String[] strings = storageClient.upload_file(file.getBytes(), ext, null);

        // 返回url
        String url = "http://192.168.200.128:8080";
        for (String string : strings) {
            url = url + "/"+string;
        }

        return Result.ok(url);
    }

}
