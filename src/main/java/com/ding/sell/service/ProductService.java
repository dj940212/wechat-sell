package com.ding.sell.service;
import com.ding.sell.dataobject.ProductInfo;
import com.ding.sell.dto.CartDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductService {

    /**
     * 查询所有在架商品
     */
    ProductInfo findOne(String productId);

    List<ProductInfo> findUpAll();

    Page<ProductInfo> findAll(Pageable pageable);

    ProductInfo save(ProductInfo productInfo);

    /**
     * 加库存
     */
    void decreaseStock(List<CartDTO> cartDTOList);
    /**
     * 减库存
     */
    void increaseStock(List<CartDTO> cartDTOList);
}
