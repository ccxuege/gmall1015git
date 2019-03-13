package com.atguigu.gmall.item.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.bean.SkuSaleAttrValue;
import com.atguigu.gmall.bean.SpuSaleAttr;
import com.atguigu.gmall.service.ManageService;
import com.atguigu.gmall.service.SkuService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ItemController {

    @Reference
    SkuService skuService;
    @Reference
    ManageService manageService;


    @RequestMapping("{skuId}.html")
    public String item(@PathVariable("skuId") String skuId, ModelMap map){

        SkuInfo skuInfo = skuService.item(skuId);

        map.put("skuInfo",skuInfo);

        //spu销售属性列表
        String spuId = skuInfo.getSpuId();
        List<SpuSaleAttr> spuSaleAttrListCheckBySku =  manageService.getSpuSaleAttrListCheckBySku(spuId,skuId);
        map.put("spuSaleAttrListCheckBySku",spuSaleAttrListCheckBySku);

        //页面sku销售属性的hash表格
        Map<String, String> skuMap = new HashMap<>();
        List<SkuInfo> skuInfos = skuService.getSkuSaleAttrValueListBySpu(spuId);
        for (SkuInfo info : skuInfos) {
            List<SkuSaleAttrValue> skuSaleAttrValueList = info.getSkuSaleAttrValueList();
            String k = "";
            for (SkuSaleAttrValue skuSaleAttrValue : skuSaleAttrValueList) {
                k = k + "|" + skuSaleAttrValue.getSaleAttrValueId();
            }
            String v = info.getId();
            skuMap.put(k,v);
        }
        String s = JSON.toJSONString(skuMap);
        map.put("skuMap",s);
        return "item";
    }
}
