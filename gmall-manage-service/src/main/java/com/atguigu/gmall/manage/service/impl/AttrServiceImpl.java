package com.atguigu.gmall.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.BaseAttrInfo;
import com.atguigu.gmall.bean.BaseAttrValue;
import com.atguigu.gmall.manage.mapper.BaseAttrInfoMapper;
import com.atguigu.gmall.manage.mapper.BaseAttrValueMapper;
import com.atguigu.gmall.service.AttrService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Set;

@Service
public class AttrServiceImpl implements AttrService {

    @Autowired
    BaseAttrInfoMapper baseAttrInfoMapper;
    @Autowired
    BaseAttrValueMapper baseAttrValueMapper;


    @Override
    public List<BaseAttrInfo> getAttrList(String catalog3Id) {

        List<BaseAttrInfo> list = null;
        if (StringUtils.isBlank(catalog3Id)){
           list = baseAttrInfoMapper.selectAll();
        }else {
            BaseAttrInfo baseAttrInfo = new BaseAttrInfo();

            baseAttrInfo.setCatalog3Id(catalog3Id);

           list = baseAttrInfoMapper.select(baseAttrInfo);
        }

        return list;


    }

    @Override
    public void saveAttr(BaseAttrInfo baseAttrInfo) {
        baseAttrInfoMapper.insertSelective(baseAttrInfo);
        String attrId = baseAttrInfo.getId();

        List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
        for (BaseAttrValue baseAttrValue : attrValueList){
            baseAttrValue.setAttrId(attrId);
            baseAttrValueMapper.insertSelective(baseAttrValue);
        }
    }

    @Override
    public List<BaseAttrInfo> getAttrListByCatalog3Id(String catalog3Id) {
        List<BaseAttrInfo> baseAttrInfos = null;
        if (StringUtils.isNoneBlank(catalog3Id)){
            BaseAttrInfo baseAttrInfo = new BaseAttrInfo();

            baseAttrInfo.setCatalog3Id(catalog3Id);

            baseAttrInfos = baseAttrInfoMapper.select(baseAttrInfo);

            for (BaseAttrInfo attrInfo : baseAttrInfos) {
                String attrId = attrInfo.getId();
                BaseAttrValue baseAttrValue = new BaseAttrValue();
                baseAttrValue.setAttrId(attrId);
                List<BaseAttrValue> baseAttrValues = baseAttrValueMapper.select(baseAttrValue);
                attrInfo.setAttrValueList(baseAttrValues);
            }
        }



        return baseAttrInfos;

    }

    @Override
    public List<BaseAttrInfo> getAttrListByValueIds(Set<String> ids) {

        String idJoin = StringUtils.join(ids, ",");
        List<BaseAttrInfo> baseAttrInfos = baseAttrInfoMapper.selectAttrListByValueIds(idJoin);
        return baseAttrInfos;
    }
}
