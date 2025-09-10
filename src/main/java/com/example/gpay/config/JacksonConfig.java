//package com.example.gpay.config;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.core.JsonGenerator;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.context.annotation.Primary;
//
//@Configuration
//public class JacksonConfig {
//
//    @Bean
//    @Primary
//    public ObjectMapper objectMapper() {
//        ObjectMapper mapper = new ObjectMapper();
//
//        // Enable escaping of non-ASCII characters
//        mapper.getFactory().configure(JsonGenerator.Feature.ESCAPE_NON_ASCII, true);
//
//        // You can also add a custom serializer for strings
//        return mapper;
//    }
//}