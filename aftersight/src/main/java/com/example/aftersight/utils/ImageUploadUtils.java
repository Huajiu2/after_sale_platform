package com.example.aftersight.utils;

import com.aliyun.oss.OSS;
import com.example.aftersight.config.OssConfiguration;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/**
 * 上传图片工具类
 */
@Component   //交给spring容器管理
public class ImageUploadUtils {

    @Autowired
    private OSS ossClient;

    @Autowired
    private OssConfiguration ossConfiguration;

    public String uploadImage(MultipartFile file,String originalFilename){
        String extension = getFileExtension(originalFilename);
        if(extension==null)
            throw new RuntimeException("请上传图片");
        String name = "images/" + UUID.randomUUID().toString() + extension;

        try(InputStream inputStream=file.getInputStream()){
            ossClient.putObject(
                    ossConfiguration.getBucketName(),
                    name,
                    inputStream
            );
            return buildUrl(name);
        } catch (IOException e) {
            throw new RuntimeException("文件上传失败",e);
        }
    }


    //获取文件扩展名
    private String getFileExtension(String fileName){
        // 只返回后缀名不带点：jpg
        String ext = FilenameUtils.getExtension(fileName);
        // 拼接带点的后缀
        String suffix = ext.isBlank() ? "" : "." + ext.toLowerCase();
        return suffix;
    }

    private String buildUrl(String objectName) {
        // 形式：https://{bucket}.{endpoint}/{objectName}
        String endpoint = ossConfiguration.getEndpoint();
        String bucket = ossConfiguration.getBucketName();
        // 注意 endpoint 可能带 https:// 前缀，需要处理
        if (endpoint.startsWith("https://")) {
            endpoint = endpoint.substring(8);
        } else if (endpoint.startsWith("http://")) {
            endpoint = endpoint.substring(7);
        }
        return String.format("https://%s.%s/%s", bucket, endpoint, objectName);
    }
}
