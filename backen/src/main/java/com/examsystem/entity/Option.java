package com.examsystem.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Data;

@Data
public class Option {
    private String key;
    private String value;
    private String imageUrl;
    
    // 用于从字符串构造 Option 对象
    public Option() {
    }
    
    // 从字符串构造 Option 对象，格式如 "A. 地球"
    @JsonCreator
    public Option(String optionStr) {
        if (optionStr != null && optionStr.length() > 2) {
            this.key = optionStr.substring(0, 1);
            if (optionStr.charAt(1) == '.') {
                this.value = optionStr.substring(2).trim();
            } else {
                this.value = optionStr.substring(1).trim();
            }
        }
    }
}