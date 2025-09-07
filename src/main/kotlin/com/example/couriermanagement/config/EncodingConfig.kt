package com.example.couriermanagement.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.StringHttpMessageConverter
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.nio.charset.StandardCharsets

@Configuration
class EncodingConfig : WebMvcConfigurer {
    
    @Bean
    fun stringHttpMessageConverter(): StringHttpMessageConverter {
        return StringHttpMessageConverter(StandardCharsets.UTF_8)
    }
    
    override fun configureMessageConverters(converters: MutableList<HttpMessageConverter<*>>) {
        converters.add(0, stringHttpMessageConverter())
    }
}