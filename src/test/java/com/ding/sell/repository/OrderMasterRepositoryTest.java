package com.ding.sell.repository;

import com.ding.sell.dataobject.OrderMaster;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.junit4.SpringRunner;

import java.math.BigDecimal;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest
public class OrderMasterRepositoryTest {
    @Autowired
    private OrderMasterRepository repository;

    @Test
    public void saveTest() {
        OrderMaster orderMaster = new OrderMaster();
        orderMaster.setOrderId("1238565");
        orderMaster.setBuyerName("杨女士");
        orderMaster.setBuyerAddress("东风东路699广东港澳中心403");
        orderMaster.setBuyerPhone("15155620677");
        orderMaster.setBuyerOpenid("79065443");
        orderMaster.setOrderAmount(new BigDecimal(188));
        OrderMaster result = repository.save(orderMaster);

        Assert.assertEquals("79065443" , result.getBuyerOpenid());
    }

    @Test
    public void findByBuyerOpenid() {
        PageRequest request = new PageRequest(0, 1);

        Page<OrderMaster> result = repository.findByBuyerOpenid("79065443", request);
        Assert.assertNotEquals(0, result.getTotalElements());
        System.out.println(result.getTotalElements());
    }
}