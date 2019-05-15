package com.atguigu.gmall.search.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.SkuLsInfo;
import com.atguigu.gmall.bean.SkuLsParam;
import com.atguigu.gmall.service.SearchService;
import io.searchbox.client.JestClient;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.highlight.HighlightBuilder;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class SearchServiceImpl implements SearchService {

    @Autowired
    JestClient jestClient;
    @Override
    public List<SkuLsInfo> search(SkuLsParam skuLsParam) {

        ArrayList<SkuLsInfo> skuLsInfos = new ArrayList<>();

        //进行搜索
        String dsl = getMyDslString(skuLsParam);
//        System.err.print(dsl);
        Search builder = new Search.Builder(dsl).addIndex("gmall1015").addType("SkulsInfo").build();
        try {
            SearchResult execute = jestClient.execute(builder);
            List<SearchResult.Hit<SkuLsInfo, Void>> hits = execute.getHits(SkuLsInfo.class);
            for (SearchResult.Hit<SkuLsInfo, Void> hit : hits) {
                SkuLsInfo source = hit.source;
                Map<String, List<String>> highlight = hit.highlight;
                if (highlight != null){
                    List<String> skuNames = highlight.get("skuName");
                    source.setSkuName(skuNames.get(0));
                }
                skuLsInfos.add(source);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return skuLsInfos;
    }

    private String getMyDslString(SkuLsParam skuLsParam) {

        //创建dsl语句对象

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();


        String keyword = skuLsParam.getKeyword();//匹配
        String catalog3Id = skuLsParam.getCatalog3Id();//过滤
        String[] valueId = skuLsParam.getValueId();//过滤
        //filter term 过滤
        if (StringUtils.isNotBlank(catalog3Id)){
            TermQueryBuilder termQueryBuilder = new TermQueryBuilder("catalog3Id", catalog3Id);
            boolQueryBuilder.filter(termQueryBuilder);
        }
        if (valueId !=  null){
            for (String id : valueId) {
                TermQueryBuilder termQueryBuilder = new TermQueryBuilder("skuAttrValueList.valueId", id);
                boolQueryBuilder.filter(termQueryBuilder);
            }
        }
        //must match 匹配
        if (StringUtils.isNotBlank(keyword)){
            MatchQueryBuilder matchQueryBuilder = new MatchQueryBuilder("skuName", keyword);
            boolQueryBuilder.must(matchQueryBuilder);
        }
        //高亮
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.preTags("<span style='color:red;font-weight:bolder'>");
        highlightBuilder.postTags("</span>");
        highlightBuilder.field("skuName");
        searchSourceBuilder.highlight(highlightBuilder);

        //分页
        searchSourceBuilder.from(0);
        searchSourceBuilder.size(100);
        //转化为dsl的字符串
        searchSourceBuilder.query(boolQueryBuilder);
        String dsl = searchSourceBuilder.toString();

        return dsl;
    }
}
