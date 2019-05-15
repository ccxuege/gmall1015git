package com.atguigu.gmall.search.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.service.AttrService;
import com.atguigu.gmall.service.SearchService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.*;

@Controller
public class SearchController {

    @Reference
    SearchService searchService;
    @Reference
    AttrService attrService;

    @RequestMapping("list.html")
    public String list(SkuLsParam skuLsParam, ModelMap map){
        //商品列表结果
        List<SkuLsInfo> skuLsInfoList =  searchService.search(skuLsParam);
        //属性筛选条件列表
        Set<String> ids = new HashSet<>();
        for (SkuLsInfo skuLsInfo : skuLsInfoList) {
            List<SkuLsAttrValue> skuAttrValueList = skuLsInfo.getSkuAttrValueList();
            for (SkuLsAttrValue skuLsAttrValue : skuAttrValueList) {
                String valueId = skuLsAttrValue.getValueId();
                ids.add(valueId);
            }
        }
        List<BaseAttrInfo> attrList = attrService.getAttrListByValueIds(ids);
        //去掉已经被筛选过的属性
        String[] valueIdFromParam = skuLsParam.getValueId();
        if (valueIdFromParam!=null){
            Iterator<BaseAttrInfo> attrListIterator = attrList.iterator();
            while (attrListIterator.hasNext()){
                List<BaseAttrValue> attrValueList = attrListIterator.next().getAttrValueList();
                for (BaseAttrValue baseAttrValue : attrValueList) {
                    String valueIdFromAttrList = baseAttrValue.getId();
                    //需要排除的属性的属性值
                    for (String fromParam : valueIdFromParam) {
                        if (valueIdFromAttrList.equals(fromParam)){
                            //去掉当前属性
                            attrListIterator.remove();
                        }
                    }
                }
            }
        }
        ArrayList<Crumb> crumbs = new ArrayList<>();
        String[] valueIdFromCrumb = skuLsParam.getValueId();
        if (valueIdFromCrumb!=null){
            for (String valueId : valueIdFromCrumb) {
                Crumb crumb = new Crumb();
                crumb.setValueName(valueId);

                String[] valueIdForCumbParam = new  String[1];
                valueIdForCumbParam[0] = valueId;
                String crumbUrl = getUrlParam(skuLsParam, valueIdForCumbParam);
                crumb.setUrlParam(crumbUrl);
                crumbs.add(crumb);
            }
        }

        map.put("skuLsInfoList",skuLsInfoList);
        map.put("attrList",attrList);
        String urlParam = getUrlParam(skuLsParam);
        map.put("urlParam",urlParam);
        map.put("crumbs",crumbs);

        return "list";
    }

    private String getUrlParam(SkuLsParam skuLsParam,String... valueIdForCrumb) {

        String urlParam = "";
        String catalog3Id = skuLsParam.getCatalog3Id();
        String keyword = skuLsParam.getKeyword();
        String[] valueId = skuLsParam.getValueId();
        if (StringUtils.isNotBlank(catalog3Id)){
            if (StringUtils.isNotBlank(urlParam)){
                urlParam = urlParam + "&";
            }
            urlParam = urlParam + "catalog3Id=" + catalog3Id;
        }
        if (StringUtils.isNotBlank(keyword)){
            if (StringUtils.isNotBlank(urlParam)){
                urlParam = urlParam + "&";
            }
            urlParam = urlParam + "keyword=" + keyword;
        }
        if (valueId != null){
            for (String id : valueId) {
                int length = valueIdForCrumb.length;
                if (length == 0){
                    //非面包屑的情况页面的url
                    urlParam = urlParam + "&" + "valueId=" + id;
                }else {
                    if (!id.equals(valueIdForCrumb[0])){
                        // 面包屑url
                        urlParam = urlParam + "&" + "valueId=" + id;
                    }
                }
            }
        }
        return urlParam;
    }
}
