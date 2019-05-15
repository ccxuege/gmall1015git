package com.atguigu.gmall.search;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.bean.SkuLsInfo;
import com.atguigu.gmall.service.SkuService;
import io.searchbox.client.JestClient;
import io.searchbox.core.Index;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GmallSearchServiceApplicationTests {

	@Autowired
	JestClient jestClient;
	@Reference
	SkuService skuService;
	@Test
	public void contextLoads() throws IOException {
		//查询spl
		List<SkuInfo> skuInfoList = skuService.getSkuInfoWithValueId();

		//转化skuLsInfo
		List<SkuLsInfo> skuLsInfos = new ArrayList<>();

		for (SkuInfo skuInfo : skuInfoList) {
			SkuLsInfo skuLsInfo = new SkuLsInfo();

			BeanUtils.copyProperties(skuInfo,skuLsInfo);

			skuLsInfos.add(skuLsInfo);
			//链接es导入数据
			Index index = new Index.Builder(skuLsInfo).index("gmall1015").type("SkulsInfo").id(skuLsInfo.getId()).build();
			jestClient.execute(index);
		}


		System.out.print(jestClient);
	}

}
