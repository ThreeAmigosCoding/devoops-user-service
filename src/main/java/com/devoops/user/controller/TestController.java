package com.devoops.user.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/user")
public class TestController {

    @GetMapping("test")
    public String test() {
        return "User Service is up and running!";
    }
}
