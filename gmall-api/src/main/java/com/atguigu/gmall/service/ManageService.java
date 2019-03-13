package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.BaseSaleAttr;
import com.atguigu.gmall.bean.SpuImage;
import com.atguigu.gmall.bean.SpuInfo;
import com.atguigu.gmall.bean.SpuSaleAttr;

import java.util.List;

public interface ManageService {
    List<SpuInfo> getSpuInfoList(SpuInfo spuInfo);

    List<BaseSaleAttr> manageService();

    void saveSpu(SpuInfo spuInfo);

    List<SpuImage> spuImageList(String spuId);

    List<SpuSaleAttr> getSpuSaleAttrListBySpuId(String spuId);

    List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(String spuId,String skuId);
}
