package com.atguigu.gmall.order.controller;


import com.atguigu.gmall.order.service.OrderService;
import com.atguigu.gmall.order.vo.OrderConfirmVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class OrderController {

    @Autowired
    private OrderService orderService;


    @GetMapping("confirm")
    public String confirm(Model model){
        OrderConfirmVo confirmVo = this.orderService.confirm();
        model.addAttribute("confirmVo",confirmVo);
        return "trade";
    }



}
