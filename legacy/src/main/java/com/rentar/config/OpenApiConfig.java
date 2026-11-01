package com.rentar.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI rentarOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Rentar API")
                        .version("1.0.0")
                        .description("API REST para la administracion de Rentar"));
    }
}
