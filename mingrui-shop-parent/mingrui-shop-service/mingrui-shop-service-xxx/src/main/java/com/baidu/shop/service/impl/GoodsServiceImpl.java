package com.baidu.shop.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baidu.shop.base.BaseApiService;
import com.baidu.shop.base.Result;
import com.baidu.shop.dto.SkuDTO;
import com.baidu.shop.dto.SpuDTO;
import com.baidu.shop.entity.*;
import com.baidu.shop.mapper.*;
import com.baidu.shop.service.BrandService;
import com.baidu.shop.service.GoodsService;
import com.baidu.shop.status.HTTPStatus;
import com.baidu.shop.utils.BaiduBeanUtil;
import com.baidu.shop.utils.ObjectUtil;
import com.baidu.shop.utils.StringUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
import tk.mybatis.mapper.entity.Example;

import javax.annotation.Resource;
import java.util.*;
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

    @Autowired
    private BrandService brandService;

    @Resource
    private BrandMapper brandMapper;

    @Resource
    private CategoryMapper categoryMapper;

    @Resource
    private SpuDetailMapper spuDetailMapper;

    @Resource
    private SkuMapper skuMapper;

    @Resource
    private StockMapper stockMapper;


    //获取spu信息
    @Override
    public Result<Map<String,Object>> getSpuInfo(SpuDTO spuDTO) {
    //此方法一共查询12条数据-->1.total总条数   2.查询列表   3-7.查询品牌   8-12通过in查询分类信息
        //分页
        if(ObjectUtil.isNotNull(spuDTO.getPage())
                && ObjectUtil.isNotNull(spuDTO.getRows()))
            PageHelper.startPage(spuDTO.getPage(),spuDTO.getRows());

        //构建条件查询
        Example example = new Example(SpuEntity.class);
        Example.Criteria criteria = example.createCriteria();
        //标题-->模糊查询
        if (StringUtil.isNotEmpty(spuDTO.getTitle()))
            criteria.andLike("title","%" + spuDTO.getTitle() + "%");
        //==1,上架时-->精准查询
        if (ObjectUtil.isNotNull(spuDTO.getSaleable()) && spuDTO.getSaleable() != 2)
            criteria.andEqualTo("saleable",spuDTO.getSaleable());

        //排序
        if(ObjectUtil.isNotNull(spuDTO.getSort()))
            example.setOrderByClause(spuDTO.getOrderByClause());

        //自定义函数-->将spu信息和品牌名称一块查询
        List<SpuEntity> list = spuMapper.selectByExample(example);

        //优化后的代码
        List<SpuDTO> spuDtoList = list.stream().map(spuEntity -> {
            SpuDTO spuDTO1 = BaiduBeanUtil.copyProperties(spuEntity, SpuDTO.class);

            //品牌名称
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

        PageInfo<SpuEntity> info = new PageInfo<>(list);

        return this.setResult(HTTPStatus.OK,info.getTotal() + "",spuDtoList);

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

    }

    //保存商品信息信息
    @Transactional
    @Override
    public Result<JSONObject> saveGoods(SpuDTO spuDTO) {
        Date date = new Date();

        SpuEntity spuEntity = BaiduBeanUtil.copyProperties(spuDTO,SpuEntity.class);
        spuEntity.setSaleable(1);
        spuEntity.setValid(1);
        spuEntity.setCreateTime(date);
        spuEntity.setLastUpdateTime(date);
        //新增spu(特殊规格)
        spuMapper.insertSelective(spuEntity);

        Integer spuId = spuEntity.getId();
        //新增spudetail(商品描述信息)
        SpuDetailEntity spuDetailEntity = BaiduBeanUtil.copyProperties(spuDTO.getSpuDetail(), SpuDetailEntity.class);
        spuDetailEntity.setSpuId(spuId);
        spuDetailMapper.insertSelective(spuDetailEntity);

        //新增sku和stock数据
        this.addSkusAndStock(spuDTO.getSkus(),spuId,date);

        return this.setResultSuccess();
    }

    //通过spuId获取sku通用信息
    @Override
    public Result<List<SkuDTO>> getSkuBySpuId(Integer spuId) {
        List<SkuDTO> list = skuMapper.selectSkuAndStockBySpuId(spuId);
        return this.setResultSuccess(list);
    }

    //通过spuId获取spu-detail库存信息
    @Override
    public Result<SpuDetailEntity> getSpuDetailBydSpu(Integer spuId) {
        SpuDetailEntity spuDetailEntity  = spuDetailMapper.selectByPrimaryKey(spuId);
        return this.setResultSuccess(spuDetailEntity);
    }

    //修改商品信息
    @Transactional
    @Override
    public Result<JSONObject> editGoods(SpuDTO spuDTO) {
        Date date = new Date();

        //修改spu信息
        SpuEntity spuEntity = BaiduBeanUtil.copyProperties(spuDTO, SpuEntity.class);
        spuEntity.setLastUpdateTime(date);
        spuMapper.updateByPrimaryKeySelective(spuEntity);

        //修改detail
        spuDetailMapper.updateByPrimaryKeySelective(
                BaiduBeanUtil.copyProperties(spuDTO.getSpuDetail(),SpuDetailEntity.class));

        //通过spuId查询出将要被删除的sku
        Example example = new Example(SkuEntity.class);
        example.createCriteria().andEqualTo("spuId",spuDTO.getId());

        List<Long> skuList = skuMapper.selectByExample(example)
                .stream()
                .map(sku -> sku.getId())
                .collect(Collectors.toList());
        //通过skuId集合删除sku
        skuMapper.deleteByIdList(skuList);

        //通过skuId集合删除stock
        stockMapper.deleteByIdList(skuList);

        //新增sku和stock数据
        this.addSkusAndStock(spuDTO.getSkus(),spuDTO.getId(),date);

        return this.setResultSuccess();
    }

    //--------------封装方法----------------------------------------------
    private void addSkusAndStock(List<SkuDTO> skus,Integer spuId,Date date){

        skus.stream().forEach(skuDTO -> {
            //新增sku(通用规格)
            SkuEntity skuEntity = BaiduBeanUtil.copyProperties(skuDTO, SkuEntity.class);
            skuEntity.setSpuId(spuId);
            skuEntity.setCreateTime(date);
            skuEntity.setLastUpdateTime(date);
            skuMapper.insertSelective(skuEntity);

            //新增stock(库存)
            StockEntity stockEntity = new StockEntity();
            stockEntity.setSkuId(skuEntity.getId());
            stockEntity.setStock(skuDTO.getStock());
            stockMapper.insertSelective(stockEntity);
        });

    }

}
