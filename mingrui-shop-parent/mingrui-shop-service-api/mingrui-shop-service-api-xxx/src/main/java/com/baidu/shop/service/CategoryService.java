package com.baidu.shop.service;

import com.baidu.shop.base.Result;
import com.baidu.shop.entity.CategoryEntity;
import com.baidu.shop.validate.group.MingruiOperation;
import com.google.gson.JsonObject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.spring.web.json.Json;

import java.util.List;

@Api(tags = "商品分类信息")
public interface CategoryService {

    @ApiOperation(value = "通过查询商品分类")
    @GetMapping(value = "category/list")
    public Result<List<CategoryEntity>> getCategoryByPid(Integer pid);

    @ApiOperation(value = "新增商品分类")
    @PostMapping(value = "category/add")
    //声明哪个组下面的参数参加校验-->当前是校验新增组
    Result<JsonObject> saveCategory(@Validated({MingruiOperation.Add.class})@RequestBody CategoryEntity categoryEntity);

    @ApiOperation(value = "修改商品分类")
    @PutMapping(value = "category/edit")
    //声明哪个组下面的参数参加校验-->当前是校验Update组
    Result<JsonObject> editCategory(@Validated({MingruiOperation.Update.class})@RequestBody CategoryEntity categoryEntity);

    @ApiOperation(value = "删除商品分类")
    @DeleteMapping(value = "category/delete")
    Result<JsonObject> deleteCategory(Integer id);

    @ApiOperation(value = "通过品牌id查询商品分类")
    @GetMapping(value = "category/getByBrand")
    public Result<List<CategoryEntity>> getByBrand(Integer brandId);
}
