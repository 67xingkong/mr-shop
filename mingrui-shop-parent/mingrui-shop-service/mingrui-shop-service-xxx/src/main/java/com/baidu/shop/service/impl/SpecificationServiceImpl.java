package com.baidu.shop.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baidu.shop.base.BaseApiService;
import com.baidu.shop.base.Result;
import com.baidu.shop.dto.SpecGroupDTO;
import com.baidu.shop.dto.SpecParamDTO;
import com.baidu.shop.entity.SpecGroupEntity;
import com.baidu.shop.entity.SpecParamEntity;
import com.baidu.shop.mapper.SpecGroupMapper;
import com.baidu.shop.mapper.SpecParamMapper;
import com.baidu.shop.service.SpecificationService;
import com.baidu.shop.utils.BaiduBeanUtil;
import com.baidu.shop.utils.ObjectUtil;
import com.baidu.shop.utils.StringUtil;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
import tk.mybatis.mapper.entity.Example;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName SpecificationServiceImpl
 * @Description: TODO
 * @Author zhangxiangxing
 * @Date 2020/9/3
 * @Version V1.0
 **/
@RestController
public class SpecificationServiceImpl extends BaseApiService implements SpecificationService {

    @Resource
    private SpecGroupMapper specGroupMapper;
    @Resource
    private SpecParamMapper specParamMapper;

    //通过条件查询规格组
    @Override
    public Result<List<SpecGroupEntity>> getSpecGroupInfo(SpecGroupDTO specGroupDTO) {

        //通过分类id查询数据
        Example example = new Example(SpecGroupEntity.class);

        if(ObjectUtil.isNotNull(specGroupDTO.getCid()))
            example.createCriteria().andEqualTo("cid",specGroupDTO.getCid());

        List<SpecGroupEntity> list = specGroupMapper.selectByExample(example);
        return this.setResultSuccess(list);
    }

    //新增规格组
    @Transactional
    @Override
    public Result<JSONObject> save(SpecGroupDTO specGroupDTO) {
        specGroupMapper.insertSelective(BaiduBeanUtil.copyProperties(specGroupDTO,SpecGroupEntity.class));
        return this.setResultSuccess();
    }

    //修改规格组
    @Transactional
    @Override
    public Result<JSONObject> edit(SpecGroupDTO specGroupDTO) {
        specGroupMapper.updateByPrimaryKeySelective(BaiduBeanUtil.copyProperties(specGroupDTO,SpecGroupEntity.class));
        return this.setResultSuccess();
    }

    //删除规格组
    @Transactional
    @Override
    public Result<JSONObject> delete(Integer id) {
        //如果id不为空
        //通过组id查询关系表的数据
            //如果有数据---不能删除
            //如果没有---删除

        String names = "";

        Example example = new Example(SpecParamEntity.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo(id);
        List<SpecGroupEntity> list = specGroupMapper.selectByExample(criteria);

        if(list.size() != 0){
            for (SpecGroupEntity specGroupEntity : list) {
                String name = specGroupEntity.getName();
                names += name + " ";
            }
            return this.setResultError("该分类被" + names + "绑定,不能被删除");
        }

        specGroupMapper.deleteByPrimaryKey(id);

        return this.setResultSuccess();
    }

    //查询规格参数
    @Transactional
    @Override
    public Result<SpecParamEntity> getSpecParamInfo(SpecParamDTO specParamDTO) {

        Example example = new Example(SpecParamEntity.class);
        Example.Criteria criteria = example.createCriteria();

        if(ObjectUtil.isNotNull(specParamDTO.getGroupId())){
            criteria.andEqualTo("groupId",specParamDTO.getGroupId());
        }

        if(ObjectUtil.isNotNull(specParamDTO.getCid())){
            criteria.andEqualTo("cid",specParamDTO.getCid());
        }

        List<SpecParamEntity> list = specParamMapper.selectByExample(example);

        return this.setResultSuccess(list);
    }

    //新增规格参数
    @Transactional
    @Override
    public Result<JSONObject> addParam(SpecParamDTO specParamDTO) {

        specParamMapper.insertSelective(BaiduBeanUtil.copyProperties(specParamDTO,SpecParamEntity.class));

        return this.setResultSuccess();
    }

    //修改规格参数
    @Transactional
    @Override
    public Result<JSONObject> editParam(SpecParamDTO specParamDTO) {

        specParamMapper.updateByPrimaryKeySelective(BaiduBeanUtil.copyProperties(specParamDTO,SpecParamEntity.class));

        return this.setResultSuccess();
    }

    //删除规格参数
    @Transactional
    @Override
    public Result<JSONObject> delParam(Integer id) {

        specParamMapper.deleteByPrimaryKey(id);

        return this.setResultSuccess();
    }


}
