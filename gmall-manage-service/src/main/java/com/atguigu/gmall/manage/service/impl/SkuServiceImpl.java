package com.atguigu.gmall.manage.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.SkuAttrValue;
import com.atguigu.gmall.bean.SkuImage;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.bean.SkuSaleAttrValue;
import com.atguigu.gmall.manage.mapper.SkuAttrValueMapper;
import com.atguigu.gmall.manage.mapper.SkuImageMapper;
import com.atguigu.gmall.manage.mapper.SkuInfoMapper;
import com.atguigu.gmall.manage.mapper.SkuSaleAttrValueMapper;
import com.atguigu.gmall.service.SkuService;
import com.atguigu.gmall.util.RedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.util.List;

@Service
public class SkuServiceImpl implements SkuService {

    @Autowired
    SkuInfoMapper skuInfoMapper;
    @Autowired
    SkuAttrValueMapper skuAttrValueMapper;
    @Autowired
    SkuSaleAttrValueMapper skuSaleAttrValueMapper;
    @Autowired
    SkuImageMapper skuImageMapper;
    @Autowired
    RedisUtil redisUtil;

    @Override
    public List<SkuInfo> skuInfoListBySpu(String spuId) {

        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setSpuId(spuId);
        List<SkuInfo> select = skuInfoMapper.select(skuInfo);
        return select;

    }

    @Override
    public void saveSku(SkuInfo skuInfo) {

        //保存sku,返回Id
        skuInfoMapper.insertSelective(skuInfo);
        String skuId = skuInfo.getId();
        //保存平台属性
        List<SkuAttrValue> skuAttrValueList = skuInfo.getSkuAttrValueList();
        for (SkuAttrValue skuAttrValue : skuAttrValueList) {
            skuAttrValue.setSkuId(skuId);
            skuAttrValueMapper.insertSelective(skuAttrValue);
        }
        //保存销售属性
        List<SkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();
        for (SkuSaleAttrValue skuSaleAttrValue : skuSaleAttrValueList) {
            skuSaleAttrValue.setSkuId(skuId);
            skuSaleAttrValueMapper.insertSelective(skuSaleAttrValue);
        }
        //保存图片
        List<SkuImage> skuImageList = skuInfo.getSkuImageList();
        for (SkuImage skuImage : skuImageList) {
            skuImage.setSkuId(skuId);
            skuImageMapper.insertSelective(skuImage);
        }
    }

    @Override
    public SkuInfo item(String skuId) {

        Jedis jedis = redisUtil.getJedis();
        SkuInfo skuInfo = null;
        try {
            String skuStr = jedis.get("sku:" + skuId + ":info");
            skuInfo = JSON.parseObject(skuStr, SkuInfo.class);

            if (skuInfo == null){
                String OK = jedis.set("sku:" + skuId + ":lock", "1", "nx", "px", 10000);
                if(StringUtils.isNotBlank(OK)&&OK.equals("OK")){
                    skuInfo = getSkuFromDb(skuId);
                    if (skuInfo != null){
                        jedis.set("sku:" + skuId + ":info",JSON.toJSONString(skuInfo));
                        jedis.del("sku:" + skuId + ":lock");
                    }
                }else {
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }finally {
            jedis.close();
        }
        return skuInfo;
    }

    public SkuInfo getSkuFromDb(String skuId){
        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setId(skuId);
        SkuInfo skuInfo1 = skuInfoMapper.selectOne(skuInfo);

        SkuImage skuImage = new SkuImage();
        skuImage.setSkuId(skuId);
        List<SkuImage> skuImages = skuImageMapper.select(skuImage);

        skuInfo1.setSkuImageList(skuImages);
        return skuInfo1;
    }
    @Override
    public List<SkuInfo> getSkuSaleAttrValueListBySpu(String spuId) {

        List<SkuInfo> skuInfos = skuSaleAttrValueMapper.selectSkuSaleAttrValueListBySpu(spuId);
        return skuInfos;
    }

    @Override
    public List<SkuInfo> getSkuInfoWithValueId() {

        List<SkuInfo> skuInfos = skuInfoMapper.selectAll();
        for (SkuInfo skuInfo : skuInfos) {
            String skuId = skuInfo.getId();

            SkuAttrValue skuAttrValue = new SkuAttrValue();
            skuAttrValue.setSkuId(skuId);
            List<SkuAttrValue> select = skuAttrValueMapper.select(skuAttrValue);
            skuInfo.setSkuAttrValueList(select);
        }
        return skuInfos;
    }

    @Override
    public SkuInfo getSkuById(String skuId) {
        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setId(skuId);
        SkuInfo skuInfo1 = skuInfoMapper.selectByPrimaryKey(skuInfo);
        return skuInfo1;
    }

}
