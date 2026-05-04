package com.airecruiter.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class SpaController {

    // Redireciona qualquer rota que não seja API ou arquivo estático para o index.html
    @RequestMapping(value = {"/", "/{path:[^\\.]*}", "/{path:^(?!api|static).*$}/**"})
    public String forward() {
        return "forward:/index.html";
    }
}