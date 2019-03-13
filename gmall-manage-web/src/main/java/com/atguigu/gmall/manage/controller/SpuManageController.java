package com.atguigu.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.manage.util.MyFileUploader;
import com.atguigu.gmall.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Controller
public class SpuManageController {

    @Reference
    ManageService manageService;

    @RequestMapping("getSpuSaleAttrListBySpuId")
    @ResponseBody
    public List<SpuSaleAttr> getSpuSaleAttrListBySpuId(String spuId){
        List<SpuSaleAttr> spuSaleAttrs = manageService.getSpuSaleAttrListBySpuId(spuId);
        return spuSaleAttrs;
    }
    @RequestMapping("spuImageList")
    @ResponseBody
    public List<SpuImage> spuImageList(String spuId){

        List<SpuImage> spuImages = manageService.spuImageList(spuId);

        return spuImages;
    }


    @RequestMapping("fileUpload")
    @ResponseBody
    public String fileUpload(@RequestParam("file") MultipartFile multipartFile){

        String url = MyFileUploader.uploadImage(multipartFile);
        //图片上传
        return url;
    }


    @ResponseBody
    @RequestMapping("spuList")
    public List<SpuInfo> getSpuInfoList(@RequestParam Map<String,String> map){

        String catalog3Id = map.get("catalog3Id");
        SpuInfo spuInfo = new SpuInfo();
        spuInfo.setCatalog3Id(catalog3Id);
       List<SpuInfo> spuInfoList =  manageService.getSpuInfoList(spuInfo);
        return spuInfoList;
    }
    @RequestMapping("baseSaleAttrList")
    @ResponseBody
    public List<BaseSaleAttr> baseSaleAttrList(){
        List<BaseSaleAttr> baseSaleAttrs =  manageService.manageService();
        return baseSaleAttrs;
    }
    @ResponseBody
    @RequestMapping("saveSpu")
    public String saveSpu(SpuInfo spuInfo){

        manageService.saveSpu(spuInfo);
        return "SUCCESS";
    }
}
