package com.baidu.shop.service.impl;

import com.baidu.shop.entity.CategoryBrandEntity;
import com.baidu.shop.entity.SpecGroupEntity;
import com.baidu.shop.mapper.BrandMapper;
import com.baidu.shop.mapper.CategoryBrandMapper;
import com.baidu.shop.mapper.CategoryMapper;
import com.baidu.shop.base.BaseApiService;
import com.baidu.shop.base.Result;
import com.baidu.shop.entity.CategoryEntity;
import com.baidu.shop.mapper.SpecGroupMapper;
import com.baidu.shop.service.CategoryService;
import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
import tk.mybatis.mapper.entity.Example;

import javax.annotation.Resource;
import java.util.List;

/**
 * @ClassName CategoryServiceImpl
 * @Description: TODO
 * @Author zhangxiangxing
 * @Date 2020/8/27
 * @Version V1.0
 **/
@RestController
public class CategoryServiceImpl extends BaseApiService implements CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;

    @Resource
    private CategoryBrandMapper categoryBrandMapper;

    @Resource
    private SpecGroupMapper specGroupMapper;

    @Resource
    private BrandMapper brandMapper;


    //通过查询商品分类
    @Transactional
    @Override
    public Result<List<CategoryEntity>> getCategoryByPid(Integer pid) {

        //new对象
        CategoryEntity categoryEntity = new CategoryEntity();


        categoryEntity.setParentId(pid);

        List<CategoryEntity> list = categoryMapper.select(categoryEntity);

        //return new Result<List<CategoryEntity>>(HTTPStatus.OK,"",list)
        return this.setResultSuccess(list);
    }

    //新增商品分类
    @Transactional
    @Override
    public Result<JsonObject> saveCategory(CategoryEntity categoryEntity) {
        //数据库表有非空约束--->updateByPrimaryKey不行

        //方式二:
        //通过页面传递过来的parentId,查询parentId对应的数据是否为父节点isParent==1
        //如果parentId对应的isParent != 1
        //需要修改为1

        //方式一
        //通过新增节点的父id,将父节点的parent状态改为1
        CategoryEntity parentCategory = new CategoryEntity();
        parentCategory.setId(categoryEntity.getParentId());
        parentCategory.setIsParent(1);

        categoryMapper.updateByPrimaryKeySelective(parentCategory);

        categoryMapper.insertSelective(categoryEntity);

        return this.setResultSuccess();
    }

    //修改商品分类
    @Transactional
    @Override
    public Result<JsonObject> editCategory(CategoryEntity categoryEntity) {

        categoryMapper.updateByPrimaryKeySelective(categoryEntity);

        return this.setResultSuccess();
    }

    //删除商品分类
    @Transactional
    @Override
    public Result<JsonObject> deleteCategory(Integer id) {

        //验证传入的id是否有效,并且查询出来的数据对接下来的程序有用
        CategoryEntity categoryEntity = categoryMapper.selectByPrimaryKey(id);
        //如果 不能 查询到分类信息
        if(categoryEntity == null) return this.setResultError("当前id不存在");

        //如果 能 查询到分类信息
        //判断当前节点是不是父节点
        if(categoryEntity.getIsParent() == 1) return this.setResultError("当前节点为父节点,不能删除");

        //通过分类id查询  关系表中  和品牌的关系
        //条件查询-->就是where后边的条件
        //构建条件查询 通过当前被删除节点的parentId查询数据
        Example example = new Example(CategoryEntity.class);
        example.createCriteria().andEqualTo("parentId",categoryEntity.getParentId());
        List<CategoryEntity> list = categoryMapper.selectByExample(example);

        //如果查询出来的数据只有一条
        if(!list.isEmpty() && list.size() == 1){//将父节点的isParent状态改为0

            CategoryEntity parentCateEntity = new CategoryEntity();
            parentCateEntity.setId(categoryEntity.getParentId());
            parentCateEntity.setIsParent(0);
            categoryMapper.updateByPrimaryKeySelective(parentCateEntity);

        }

        //select * from table where parenqw 21` tId=categoryEntity.getParentId
        //如果只查询出来一条数据的话,通过getParentId将父节点的parent状态改为0
        //如果没有的话,将父节点isParent的值改为0

        categoryMapper.deleteByPrimaryKey(id);//执行删除
        //delete from table where id = ?

        return this.setResultSuccess();
    }

    //删除代码优化
    public Result<JsonObject> ss(Integer id){
        //通过当前id查询分类信息
        CategoryEntity categoryEntity = categoryMapper.selectByPrimaryKey(id);
        if (null == categoryEntity.getId()) {
            return this.setResultError("当前id不存在");
        }
        //不能查询到分类信息直接return
        //如果能查到 判断当前节点是不是父节点
        if (categoryEntity.getIsParent() == 1) {
            return this.setResultError("当前节点为父节点,不能删除");
        }
        //通过分类id查询  关系表中  和品牌的关系
        Example example1 = new Example(CategoryBrandEntity.class);
        example1.createCriteria().andEqualTo("categoryId", id);
        List<CategoryBrandEntity> list1 = categoryBrandMapper.selectByExample(example1);
        //判断是否有和品牌的绑定
        //如果有返回信息

        //不能打印绑定的品牌名称 太多了
        if (list1.size() >= 1) {
            return this.setResultError("该分类绑定有品牌不能删除");
        }

        //通过分类id查询  关系表中  和规格的关系
        Example example2 = new Example(SpecGroupEntity.class);
        example2.createCriteria().andEqualTo("cid", id);
        List<SpecGroupEntity> list2 = specGroupMapper.selectByExample(example2);
        //判断是否有和规格的绑定
        //如果有返回信息

        if (list2.size() >= 1) {
            String mag = "";
            for (SpecGroupEntity spec : list2) {
                mag += "," + spec.getName();
            }

            return this.setResultError("该分类绑定有规格" + "(" + mag  + ")" + "不能删除");
        }


        //还需要判断一下除了当前被删除的id外还有没有父id是当前节点的数据
        Example example = new Example(CategoryEntity.class);

        example.createCriteria().andEqualTo("parentId", categoryEntity.getParentId());

        List<CategoryEntity> list = categoryMapper.selectByExample(example);

        if (list.size() == 1) {
            CategoryEntity categoryEntity1 = new CategoryEntity();
            categoryEntity1.setId(categoryEntity.getParentId());
            categoryEntity1.setIsParent(0);
            categoryMapper.updateByPrimaryKeySelective(categoryEntity1);
        }

        //如果只查询出来一条数据的话,通过ParentId将父节点的isParent状态改为0
        //没有的话将父节点ParentId改为0


        categoryMapper.deleteByPrimaryKey(id);

        return this.setResultSuccess();
    }

    //通过品牌id查询商品分类
    @Override
    public Result<List<CategoryEntity>> getByBrand(Integer brandId) {

        List<CategoryEntity> brandId1 = categoryMapper.getByBrandId(brandId);
        return this.setResultSuccess(brandId1);
    }


}
