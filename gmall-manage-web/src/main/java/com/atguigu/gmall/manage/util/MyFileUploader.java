package com.atguigu.gmall.manage.util;
import org.csource.common.MyException;
import org.csource.fastdfs.ClientGlobal;
import org.csource.fastdfs.StorageClient;
import org.csource.fastdfs.TrackerClient;
import org.csource.fastdfs.TrackerServer;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public class MyFileUploader {

    public static String uploadImage(MultipartFile multipartFile){
        String path = MyFileUploader.class.getClassLoader().getResource("tracker.conf").getPath();
        String url = "http://192.168.71.10";
        try {
            ClientGlobal.init(path);

            TrackerClient trackerClient = new TrackerClient();
            TrackerServer trackerServer = trackerClient.getConnection();
            StorageClient storageClient = new StorageClient(trackerServer, null);
//            String orginalFilename = "C:\\Users\\曹\\Desktop\\timg.jpg";
            byte[] bytes = multipartFile.getBytes();
            String originalFilename = multipartFile.getOriginalFilename();
            int i = originalFilename.lastIndexOf(".");
            //获取后缀名
            String substring = originalFilename.substring(i + 1);
            String[] pngs = storageClient.upload_file(bytes,substring,null);
            for (String png : pngs) {
                url = url + "/" + png;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (MyException e) {
            e.printStackTrace();
        }

        return url;
    }
}
