package com.baidu.shop.service.impl;

import com.baidu.shop.base.BaseApiService;
import com.baidu.shop.base.Result;
import com.baidu.shop.dto.SpuDTO;
import com.baidu.shop.entity.BrandEntity;
import com.baidu.shop.entity.SpuEntity;
import com.baidu.shop.mapper.BrandMapper;
import com.baidu.shop.mapper.CategoryMapper;
import com.baidu.shop.mapper.SpuMapper;
import com.baidu.shop.service.GoodsService;
import com.baidu.shop.status.HTTPStatus;
import com.baidu.shop.utils.BaiduBeanUtil;
import com.baidu.shop.utils.ObjectUtil;
import com.baidu.shop.utils.StringUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.web.bind.annotation.RestController;
import tk.mybatis.mapper.entity.Example;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * @ClassName GoodsServiceImpl
 * @Description: TODO
 * @Author zhangxiangxing
 * @Date 2020/9/7
 * @Version V1.0
 **/
@RestController
public class GoodsServiceImpl extends BaseApiService implements GoodsService {

    @Resource
    private SpuMapper spuMapper;

    @Resource
    private BrandMapper brandMapper;

    @Resource
    private CategoryMapper categoryMapper;

    //此方法一共查询12条数据-->1.total总条数   2.查询列表   3-7.查询品牌   8-12通过in查询分类信息
    @Override
    public Result<Map<String,Object>> getSpuInfo(SpuDTO spuDTO) {

        //分页
        if(ObjectUtil.isNotNull(spuDTO.getPage())
                && ObjectUtil.isNotNull(spuDTO.getRows()))
            PageHelper.startPage(spuDTO.getPage(),spuDTO.getRows());

        //构建条件查询
        Example example = new Example(SpuEntity.class);

        //排序
        if(ObjectUtil.isNotNull(spuDTO.getSort()))
            example.setOrderByClause(spuDTO.getOrderByClause());

        //构建查询条件
        Example.Criteria criteria = example.createCriteria();
        //标题-->模糊查询
        if (StringUtil.isNotEmpty(spuDTO.getTitle()))
            criteria.andLike("title","%" + spuDTO.getTitle() + "%");
        //==1,上架时-->精准查询
        if (ObjectUtil.isNotNull(spuDTO.getSaleable()) && spuDTO.getSaleable() != 2)
            criteria.andEqualTo("saleable",spuDTO.getSaleable());

        List<SpuEntity> list = spuMapper.selectByExample(example);

        //优化后的代码
        List<SpuDTO> spuDtoList = list.stream().map(spuEntity -> {
            SpuDTO spuDTO1 = BaiduBeanUtil.copyProperties(spuEntity, SpuDTO.class);

           /* //设置品牌名称
            BrandDTO brandDTO = new BrandDTO();
            brandDTO.setId(spuEntity.getBrandId());
            Result<PageInfo<BrandEntity>> brandInfo = brandService.getBrandInfo(brandDTO);

            if (ObjectUtil.isNotNull(brandInfo)) {

                PageInfo<BrandEntity> data = brandInfo.getData();
                List<BrandEntity> list1 = data.getList();

                if (!list1.isEmpty() && list1.size() == 1) {
                    spuDTO1.setBrandName(list1.get(0).getName());
                }
            }
*/
            BrandEntity brandEntity = brandMapper.selectByPrimaryKey(spuEntity.getBrandId());
            if (null != brandEntity) spuDTO1.setBrandName(brandEntity.getName());
            //分类名称
                String categoryName = categoryMapper.selectByIdList(
                        Arrays.asList(spuDTO1.getCid1(), spuDTO1.getCid2(), spuDTO1.getCid3()))
                        .stream().map(category -> category.getName())
                        .collect(Collectors.joining("/"));

                spuDTO1.setCategoryName(categoryName);

                return spuDTO1;
            }).collect(Collectors.toList());

        PageInfo<SpuDTO> info = new PageInfo<>(spuDtoList);

        //return this.setResultSuccess(info);
//--------以下是为了解决分页显示不全的问题----------------------------------------------------------
        //我需要返回的数据的DTO
        //但是我还需要info.total

        //HashMap<String, Object> map = new HashMap<>();
        //map.put("list",spuDtoList);
        //map.put("total",info.getTotal());

        //存在的问题
            //feign如果调用当前函数Result<Map<String,Object>>
            //如果你通过map.get("list")-->得到的数据理论上为List<SpuDTO>,实际为List<LinkedHashMap>
        //因为值的泛型是Object类型,此时用feign调用当前函数Result<Map<String,Object>>d的话,它不知道Object是什么类型
        //map存储的是键值对形式

        //解决办法:可以新建一个response类,里面存属性有List<SpuDTO>,Integer total


        return this.setResult(HTTPStatus.OK,info.getTotal() + "",spuDtoList);
    }

}
