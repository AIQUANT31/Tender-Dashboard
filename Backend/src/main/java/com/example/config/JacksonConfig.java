package com.example.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Configuration
public class JacksonConfig {

    private static final DateTimeFormatter ISO_LOCAL_DATE_TIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Bean
    public Jackson2ObjectMapperBuilder jacksonBuilder() {
        return new Jackson2ObjectMapperBuilder()
                .modules(customDateModule())
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);  // .featuresTO Disbale Means  1679300000000   and Enable into these "2026-03-20T10:30:45"
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(customDateModule());// add custom  serializer and deserializer
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    private Module customDateModule() {
        SimpleModule module = new SimpleModule("CustomDateModule");
        
        
        module.addSerializer(LocalDateTime.class, new JsonSerializer<LocalDateTime>() {  //java to json
            @Override  
            public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                if (value != null) {
                    gen.writeString(value.format(ISO_LOCAL_DATE_TIME));
                } else {
                    gen.writeNull();
                }
            }
        });
        
        
        module.addDeserializer(LocalDateTime.class, new JsonDeserializer<LocalDateTime>() { // Json to java
            @Override
            public LocalDateTime deserialize(com.fasterxml.jackson.core.JsonParser p, DeserializationContext ctxt) throws IOException {
                String value = p.getValueAsString();
                if (value != null && !value.isEmpty()) {
                    try{

                    
                        return LocalDateTime.parse(value, ISO_LOCAL_DATE_TIME);
                    } catch (Exception e) {
                        
                        try {
                            return LocalDateTime.parse(value);
                        } catch (Exception e2) {
                            return null;
                        }
                    }
                }
                return null;
            }
        });
        
        return module;
    }
}
