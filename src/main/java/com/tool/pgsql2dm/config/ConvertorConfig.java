package com.tool.pgsql2dm.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "convertor")
public class ConvertorConfig {
    // the path of pgsql file , directory or file
    private String filePath;
}
