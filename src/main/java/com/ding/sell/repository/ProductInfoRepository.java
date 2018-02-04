package com.ding.sell.repository;

import com.ding.sell.dataobject.ProductInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductInfoRepository extends JpaRepository<ProductInfo, String>{
    /**
     * 查询上架商品
     */
    List<ProductInfo> findByProductStatus(Integer productStatus);
}
