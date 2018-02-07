package com.ding.sell.service.impl;

import com.ding.sell.converter.OrderMaster2OrderDTOConverter;
import com.ding.sell.dataobject.OrderDetail;
import com.ding.sell.dataobject.OrderMaster;
import com.ding.sell.dataobject.ProductInfo;
import com.ding.sell.dto.CartDTO;
import com.ding.sell.dto.OrderDTO;
import com.ding.sell.enums.OrderStatusEnum;
import com.ding.sell.enums.PayStatusEnum;
import com.ding.sell.enums.ResultEnum;
import com.ding.sell.exception.SellException;
import com.ding.sell.repository.OrderDetailRepository;
import com.ding.sell.repository.OrderMasterRepository;
import com.ding.sell.service.OrderService;
import com.ding.sell.service.ProductService;
import com.ding.sell.utils.KeyUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    @Autowired
    private ProductService productService;

    @Autowired
    private OrderDetailRepository orderDetailRepository;

    @Autowired
    private OrderMasterRepository orderMasterRepository;

    @Override
    public OrderDTO create(OrderDTO orderDTO) {
        String orderId = KeyUtil.genUniqueKey();
        BigDecimal orderAmount = new BigDecimal(BigInteger.ZERO);

        //1.查询商品数量价格
        for (OrderDetail orderDetail:orderDTO.getOrderDetailList()) {
            ProductInfo productInfo = productService.findOne(orderDetail.getProductId());
            if (productInfo == null) {
                throw new SellException(ResultEnum.PRODUCT_NOT_EXIST);
            }
            //2.订单总价计算总价
            orderAmount = productInfo.getProductPrice().
                    multiply(new BigDecimal(orderDetail.getProductQuantity())).add(orderAmount);

            //订单详情入库
            BeanUtils.copyProperties(productInfo, orderDetail);
            orderDetail.setDetailId(KeyUtil.genUniqueKey());
            orderDetail.setOrderId(orderId);
            orderDetailRepository.save(orderDetail);
        }
        //3.写入订单数据库（orderMaster和orderDetail）
        OrderMaster orderMaster = new OrderMaster();
        BeanUtils.copyProperties(orderDTO, orderMaster);
        orderMaster.setOrderId(orderId);
        orderMaster.setOrderAmount(orderAmount);
        orderMaster.setOrderStatus(OrderStatusEnum.NEW.getCode());
        orderMaster.setPayStatus(PayStatusEnum.NEW.getCode());
        orderMasterRepository.save(orderMaster);

        //4.扣库存
        List<CartDTO> cartDTOList = orderDTO.getOrderDetailList().stream().map(e ->
            new CartDTO(e.getProductId(), e.getProductQuantity())
        ).collect(Collectors.toList());
        productService.decreaseStock(cartDTOList);

        return orderDTO;
    }

    @Override
    public OrderDTO findOne(String orderId) {
        //1.查看主订单
        OrderMaster orderMaster = orderMasterRepository.findOne(orderId);
        if( null == orderMaster ) {
            throw new SellException(ResultEnum.ORDER_NOT_EXIST);
        }

        //2.查看订单详情
        List<OrderDetail> orderDetailList = orderDetailRepository.findByOrderId(orderId);
        if(CollectionUtils.isEmpty(orderDetailList)) {
            throw new SellException(ResultEnum.ORDERDETAIL_NOT_EXIST);
        }

        OrderDTO orderDTO = new OrderDTO();
        BeanUtils.copyProperties(orderMaster , orderDTO);
        orderDTO.setOrderDetailList(orderDetailList);

        return orderDTO;
    }

    @Override
    public Page<OrderDTO> findList(String buyerOpenid, Pageable pageable) {
        Page<OrderMaster> orderMasterPage = orderMasterRepository.findByBuyerOpenid(buyerOpenid, pageable);

        List<OrderDTO> orderDTOList = OrderMaster2OrderDTOConverter.convert(orderMasterPage.getContent());

        return new PageImpl<OrderDTO>(orderDTOList , pageable , orderMasterPage.getTotalElements());
    }

    @Override
    @Transactional
    public OrderDTO cancel(OrderDTO orderDTO) {
        //判断订单状态
        if(!orderDTO.getOrderStatus().equals(OrderStatusEnum.NEW.getCode())) {
            log.error("【取消订单】 订单状态不正确：orderId={},orderStatus={}" , orderDTO.getOrderId() , orderDTO.getOrderStatus());
            throw new SellException(ResultEnum.ORDER_STATUS_ERROR);
        }

        //更新订单状态
        OrderMaster orderMaster = new OrderMaster();
        orderDTO.setOrderStatus(OrderStatusEnum.CANCEL.getCode());
        BeanUtils.copyProperties(orderDTO , orderMaster);
        OrderMaster updateResult = orderMasterRepository.save(orderMaster);
        if( null == updateResult) {
            log.error("【取消订单】更新订单状态失败：orderMaster={}" , orderMaster);
            throw new SellException(ResultEnum.ORDER_UPDATE_FAIL);
        }

        //增加库存
        if(CollectionUtils.isEmpty(orderDTO.getOrderDetailList())) {
            log.error("【取消订单】订单中无商品详情：orderDTO={}" , orderDTO);
            throw new SellException(ResultEnum.ORDER_DETAIL_EMPTY);
        }

        List<CartDTO> cartDTOList = orderDTO.getOrderDetailList().stream()
                .map(e -> new CartDTO(e.getProductId() , e.getProductQuantity()))
                .collect(Collectors.toList());

        productService.increaseStock(cartDTOList);
        //判断支付状态，如果是已支付，则退款
        if(PayStatusEnum.FINISH.getCode().equals(orderDTO.getPayStatus())) {
            //TODO
        }

        return orderDTO;
    }

    @Override
    public OrderDTO finish(OrderDTO orderDTO) {
        //判断订单状态
        if(!OrderStatusEnum.NEW.getCode().equals(orderDTO.getOrderStatus())) {
            log.error("【完结订单】 订单状态不正确：orderId={},orderStatus={}" , orderDTO.getOrderId() , orderDTO.getOrderStatus());
            throw new SellException(ResultEnum.ORDER_STATUS_ERROR);
        }


        //更新订单
        OrderMaster orderMaster = new OrderMaster();
        orderDTO.setOrderStatus(OrderStatusEnum.FINISH.getCode());
        BeanUtils.copyProperties(orderDTO , orderMaster);
        OrderMaster updateResult = orderMasterRepository.save(orderMaster);
        if( null == updateResult) {
            log.error("【取消订单】更新订单状态失败：orderMaster={}" , orderMaster);
            throw new SellException(ResultEnum.ORDER_UPDATE_FAIL);
        }

        return orderDTO;
    }

    @Override
    public OrderDTO pay(OrderDTO orderDTO) {
        //检查订单支付状态
        if(!PayStatusEnum.NEW.getCode().equals(orderDTO.getPayStatus())) {
            log.error("【支付订单】 订单支付状态不正确：orderId={},orderStatus={}" , orderDTO.getOrderId() , orderDTO.getPayStatus());
            throw new SellException(ResultEnum.ORDER_STATUS_ERROR);
        }

        //更新订单
        OrderMaster orderMaster = new OrderMaster();
        orderDTO.setPayStatus(PayStatusEnum.FINISH.getCode());
        BeanUtils.copyProperties(orderDTO , orderMaster);
        OrderMaster updateResult = orderMasterRepository.save(orderMaster);
        if( null == updateResult) {
            log.error("【支付订单】更新订单支付状态失败：orderMaster={}" , orderMaster);
            throw new SellException(ResultEnum.ORDER_UPDATE_FAIL);
        }

        return orderDTO;
    }

    @Override
    public Page<OrderDTO> findList(Pageable pageable) {
        return null;
    }
}