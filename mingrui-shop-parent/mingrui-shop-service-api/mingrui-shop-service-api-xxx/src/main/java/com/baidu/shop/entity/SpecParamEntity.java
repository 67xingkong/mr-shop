package com.baidu.shop.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * @ClassName SpecParamEntity
 * @Description: TODO
 * @Author zhangxiangxing
 * @Date 2020/9/3
 * @Version V1.0
 **/
@Table(name = "tb_spec_param")
@Data
public class SpecParamEntity {//规格参数组下的参数名

    @Id
    private Integer id;

    private Integer cid;

    private Integer groupId;

    private String name;

    //numeric是mysql数据库的关键字,
    @Column(name = "`numeric`")
    private Boolean numeric;

    private String unit;

    private Boolean generic;

    private Boolean searching;

    private String segments;
}
