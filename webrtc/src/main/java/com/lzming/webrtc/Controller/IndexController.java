package com.lzming.webrtc.Controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController
@RequestMapping("/index")
public class IndexController {

    @RequestMapping("/index")
    public String index(){
        return "index";
    }

}
