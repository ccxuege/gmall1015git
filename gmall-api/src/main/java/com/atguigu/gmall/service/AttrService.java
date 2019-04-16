package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.BaseAttrInfo;

import java.util.List;
import java.util.Set;

public interface AttrService {

  List<BaseAttrInfo> getAttrList(String catalog3Id);

    void saveAttr(BaseAttrInfo baseAttrInfo);

    List<BaseAttrInfo> getAttrListByCatalog3Id(String catalog3Id);

    List<BaseAttrInfo> getAttrListByValueIds(Set<String> ids);
}
