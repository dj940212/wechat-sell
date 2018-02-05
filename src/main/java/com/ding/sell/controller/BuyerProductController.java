package com.ding.sell.controller;

import com.ding.sell.VO.ProductInfoVO;
import com.ding.sell.VO.ProductVO;
import com.ding.sell.VO.ResultVO;
import com.ding.sell.dataobject.ProductCategory;
import com.ding.sell.dataobject.ProductInfo;
import com.ding.sell.service.CategoryService;
import com.ding.sell.service.ProductService;
import com.ding.sell.utils.ResultVOUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 买家商品
 */

@RestController
@RequestMapping("/buyer/product")
public class BuyerProductController {
    @Autowired
    ProductService productService;
    @Autowired
    CategoryService categoryService;

    @GetMapping("/list")
    public ResultVO list() {
        //1.查询所有上架商品
        List<ProductInfo> productInfoList = productService.findUpAll();

        //2.所有类目列表
        List<Integer> categoryTypeList = productInfoList.stream()
                .map(e -> e.getCategoryType())
                .collect(Collectors.toList());

        List<ProductCategory> productCategoryList = categoryService.findByCategoryTypeIn(categoryTypeList);

        //2.数据拼装
        List<ProductVO> productVOList = new ArrayList<>();
        for (ProductCategory productCategory : productCategoryList) {
            ProductVO productVO = new ProductVO();
            productVO.setCategorytype(productCategory.getCategoryType());
            productVO.setCategoryName(productCategory.getCategoryName());

            //商品详情列表
            List<ProductInfoVO> productInfoVOList = new ArrayList<>();

            for (ProductInfo productInfo: productInfoList) {
                if (productInfo.getCategoryType().equals(productCategory.getCategoryType())) {
                    ProductInfoVO productInfoVO = new ProductInfoVO();
                    BeanUtils.copyProperties(productInfo, productInfoVO);
                    productInfoVOList.add(productInfoVO);
                }
            }

            productVO.setProductInfoVOList(productInfoVOList);
            productVOList.add(productVO);
        }


        return ResultVOUtil.success(productVOList);
    }
}
