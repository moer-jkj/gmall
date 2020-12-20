package com.atguigu.gmall.pms.service.impl;


import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.feign.GmallPmsClient;
import com.atguigu.gmall.pms.mapper.SkuMapper;
import com.atguigu.gmall.pms.mapper.SpuDescMapper;
import com.atguigu.gmall.pms.service.*;

import com.atguigu.gmall.pms.vo.SkuVo;
import com.atguigu.gmall.pms.vo.SpuAttrValueVo;
import com.atguigu.gmall.pms.vo.SpuVo;
import com.atguigu.gmall.sms.vo.SkuSaleVo;
import io.seata.spring.annotation.GlobalTransactional;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.pms.mapper.SpuMapper;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;


@Service("spuService")
public class SpuServiceImpl extends ServiceImpl<SpuMapper, SpuEntity> implements SpuService {

    @Autowired
    private SpuDescMapper descMapper;


    @Autowired
    private SpuAttrValueService attrValueService;

    @Autowired
    private SkuMapper skuMapper;

    @Autowired
    private SkuImagesService imagesService;

    @Autowired
    private SkuAttrValueService skuAttrValueService;

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private SpuDescService descService;

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<SpuEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<SpuEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    public PageResultVo querySpuByCidAndPage(Long cid, PageParamVo pageParamVo) {
        QueryWrapper<SpuEntity> wrapper = new QueryWrapper<>();

        // 分类id不为0，要根据分类id查，否则查全部
        if (cid != 0) {
            System.out.println(cid);
            wrapper.eq("category_id", cid);
        }
        String key = pageParamVo.getKey();
        System.out.println(key);
        // 判断关键字是否为空
        if (StringUtils.isNotBlank(key)) {
            wrapper.and(t -> t.eq("id", key).or().like("name", key));
        }

        // 如果用户输入了检索条件，根据检索条件进行查询
        IPage<SpuEntity> page = this.page(
                pageParamVo.getPage(),
                wrapper
        );

        return new PageResultVo(page);
    }

    @GlobalTransactional
    @Override
    public void bigSave(SpuVo spu) {
        // 1.保存spu的相关信息
        // 1.1 保存spu的基本信息：pms_spu
        Long spuId = saveSpu(spu);

        // 1.2 保存spu_desc的基本信息 :pms_spu_desc
        this.descService.saveSpuDesc(spu,spuId);


        // 1.3 保存spu的基本属性信息 : pms_spu_attr_value
        saveBaseAttr(spu, spuId);


        // 2.保存sku相关信息
        saveSkuInfo(spu, spuId);


    }

    private void saveSkuInfo(SpuVo spu, Long spuId) {
        List<SkuVo> skus = spu.getSkus();
        if (CollectionUtils.isEmpty(skus)) {
            return;
        }
        // 2.1 保存sku的基本信息： pms_sku
        skus.forEach(sku -> {
            sku.setSpuId(spuId);
            sku.setBrandId(spu.getBrandId());
            sku.setCatagoryId(spu.getCategoryId());
            // 设置默认图片
            List<String> images = sku.getImages();
            if (!CollectionUtils.isEmpty(images)) {
                //sku.setDefaultImage(images.get(0));
                sku.setDefaultImage(StringUtils.isNotBlank(sku.getDefaultImage()) ? sku.getDefaultImage() : images.get(0));
            }

            this.skuMapper.insert(sku);
            Long skuId = sku.getId();

            // 2.2 保存sku的图片信息 : pms_sku_image
            if (!CollectionUtils.isEmpty(images)){
                this.imagesService.saveBatch(images.stream().map(image -> {
                    SkuImagesEntity skuImagesEntity = new SkuImagesEntity();
                    skuImagesEntity.setSkuId(skuId);
                    skuImagesEntity.setUrl(image);
                    skuImagesEntity.setDefaultStatus(StringUtils.equals(sku.getDefaultImage(),image) ? 1 : 0);
                    return skuImagesEntity;
                }).collect(Collectors.toList()));
            }
            // 2.3 保存sku的销售信息： pms_sku_attr_value
            List<SkuAttrValueEntity> saleAttrs = sku.getSaleAttrs();
            if (!CollectionUtils.isEmpty(saleAttrs)){
                saleAttrs.forEach(skuAttrValueEntity -> skuAttrValueEntity.setSkuId(skuId));
                this.skuAttrValueService.saveBatch(saleAttrs);
            }


            // 3.保存sku的营销信息
            SkuSaleVo skuSaleVo = new SkuSaleVo();
            BeanUtils.copyProperties(sku,skuSaleVo);
            skuSaleVo.setSkuId(skuId);
            this.pmsClient.saveSales(skuSaleVo);
            // 远程调用 sms
            // 3.1 保存sku的积分优惠 sms_sku_bounds
            // 3.2 保存sku的满减信息 sms_sku_full_reduction
            // 3.3 保存sku的打折信息 sms_sku_ladder
        });
    }

    private void saveBaseAttr(SpuVo spu, Long spuId) {
        List<SpuAttrValueVo> baseAttrs = spu.getBaseAttrs();
        if (!CollectionUtils.isEmpty(baseAttrs)) {

            // 需要的参数为一个entity类型的集合，需要将 baseAttrs转换为 entity集合
            this.attrValueService.saveBatch(baseAttrs.stream().map(spuAttrValueVo -> {
                SpuAttrValueEntity spuAttrValueEntity = new SpuAttrValueEntity();
                // 把vo的数据设置到entity
                BeanUtils.copyProperties(spuAttrValueVo, spuAttrValueEntity);
                spuAttrValueEntity.setSpuId(spuId);
                return spuAttrValueEntity;
            }).collect(Collectors.toList()));

            // 需要遍历 baseAttrs，然后将数据保存到数据库中
            // insert是mapper里面的方法，一次只能插入一条数据
            // 用service里面的saveBatch方法可以批量保存(IService里面的方法)
            // this.attrValueMapper.insert(spuAttrValueEntity);
        }
    }

    public void saveSpuDesc(SpuVo spu, Long spuId) {
        List<String> spuImages = spu.getSpuImages();
        if (!CollectionUtils.isEmpty(spuImages)) {
            SpuDescEntity spuDescEntity = new SpuDescEntity();
            spuDescEntity.setSpuId(spuId);
            spuDescEntity.setDecript(StringUtils.join(spuImages, ","));
            this.descMapper.insert(spuDescEntity);
        }
    }

    private Long saveSpu(SpuVo spu) {
        spu.setCreateTime(new Date());
        spu.setUpdateTime(spu.getCreateTime());
        this.save(spu);
        return spu.getId();
    }

}