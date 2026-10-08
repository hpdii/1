package com.donk.server;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint kiem chung ket noi. Dung o Ngay 0 de xac nhan dien thoai goi duoc
 * server qua mang LAN truoc khi xay bat ky tinh nang nao.
 */
@RestController
@RequestMapping("/api")
public class PingController {

    @GetMapping("/ping")
    public Map<String, Object> ping() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("pong", true);
        body.put("service", "donk-server");
        body.put("time", LocalDateTime.now().toString());
        return body;
    }
}
