package com.projek.tokweb.config.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.cloudinary.Cloudinary;

@Configuration
public class CloudinaryConfig {
    

    @Bean
    public Cloudinary cloudinary() {
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", "dr1ilfxca");
        config.put("api_key", "194429683613698");
        config.put("api_secret", "dbsEXkJX48dKOg_r16BEsuq081c");
        return new Cloudinary(config);
    }
}
