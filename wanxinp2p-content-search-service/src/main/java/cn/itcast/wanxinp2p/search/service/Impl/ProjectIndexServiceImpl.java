package cn.itcast.wanxinp2p.search.service.Impl;

import cn.itcast.wanxinp2p.api.search.model.ProjectQueryParamsDTO;
import cn.itcast.wanxinp2p.api.transaction.model.ProjectDTO;
import cn.itcast.wanxinp2p.common.domain.PageVO;
import cn.itcast.wanxinp2p.search.service.ProjectindexService;
import org.apache.commons.lang.StringUtils;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Map;

/**
 * @author old money
 * @create 2022-02-23 15:30
 */
@Service
public class ProjectIndexServiceImpl implements ProjectindexService {


    @Autowired
    private RestHighLevelClient restHighLevelClient;

    /**
     * ES 索引库名称
     */
    @Value("${wanxinp2p.ex.index}")
    private String projectIndex;


    /**
     * 从ES索引库检索标的信息
     * @param projectQueryParamsDTO
     * @param pageNo
     * @param pageSize
     * @param sortBy
     * @param order
     * @return
     */
    @Override
    public PageVO<ProjectDTO> queryProjectIndex(ProjectQueryParamsDTO projectQueryParamsDTO, Integer pageNo, Integer pageSize, String sortBy, String order) {

        //1.创建搜索请求对象
        SearchRequest searchRequest = new SearchRequest(projectIndex);

        //2.搜索条件
            //2.1创建条件封装对象
            BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();
            //2.2非空判断并封装条件
            if (StringUtils.isNotBlank(projectQueryParamsDTO.getName())){
                queryBuilder.must(QueryBuilders.termQuery("name",projectQueryParamsDTO.getName()));
            }
            if (projectQueryParamsDTO.getStartPeriod() != null){
                queryBuilder.must(QueryBuilders.rangeQuery("period").gte(projectQueryParamsDTO.getStartPeriod()));
            }
            if (projectQueryParamsDTO.getEndPeriod() != null){
                queryBuilder.must(QueryBuilders.rangeQuery("period").lte(projectQueryParamsDTO.getEndPeriod()));
            }

        //3.创建SearchSourceBuilder对象 ----总的封装对象
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

            //3.1封装条件
            sourceBuilder.query(queryBuilder);

            //3.2设置排序参数
            if (StringUtils.isNotBlank(sortBy) && StringUtils.isNotBlank(order)){
                if (order.toLowerCase().equals("asc")){
                    sourceBuilder.sort(sortBy, SortOrder.ASC);
                }
                if (order.toLowerCase().equals("desc")){
                    sourceBuilder.sort(sortBy,SortOrder.DESC);
                }
            }else{
                sourceBuilder.sort("createdate",SortOrder.DESC);
            }

            //3.3设置分页参数
            sourceBuilder.from((pageNo-1)*pageSize);
            sourceBuilder.size(pageSize);

        //4.完成封装
        searchRequest.source(sourceBuilder);

        //5.执行搜索
        ArrayList<ProjectDTO> list = new ArrayList<>();
        PageVO<ProjectDTO> pageVO = new PageVO<>();
        try {
            SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);


            //6.获取响应结果
            SearchHits hits = searchResponse.getHits();
            long totalHits = hits.getTotalHits().value;//匹配的总记录数
            pageVO.setTotal(totalHits);
            SearchHit[] searchHits = hits.getHits();//获取匹配数据

            //7.循环封装DTO
            for(SearchHit hit:searchHits){
                ProjectDTO projectDTO = new ProjectDTO();
                Map<String, Object> sourceAsMap = hit.getSourceAsMap();
                Double amount = (Double) sourceAsMap.get("amount");
                String projectstatus = (String) sourceAsMap.get("projectstatus");
                Integer period = Integer.parseInt(sourceAsMap.get("period").toString());
                String name = (String) sourceAsMap.get("name");
                String description = (String) sourceAsMap.get("description");
                BigDecimal annualrate =new BigDecimal(sourceAsMap.get("annualrate").toString());
                projectDTO.setAmount(new BigDecimal(amount));
                projectDTO.setProjectStatus(projectstatus);
                projectDTO.setPeriod(period);
                projectDTO.setName(name);
                projectDTO.setDescription(description);
                projectDTO.setAnnualRate(annualrate);

                list.add(projectDTO);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        //8.封装为PageVO对象并返回
        pageVO.setContent(list);
        pageVO.setPageSize(pageSize);
        pageVO.setPageNo(pageNo);

        return pageVO;
    }
}
