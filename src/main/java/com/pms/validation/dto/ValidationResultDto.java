package com.pms.validation.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class ValidationResultDto {
    private boolean valid = true;
    private List<String> errors = new ArrayList<>();

    public void addError(String msg) {
        this.valid = false;
        this.errors.add(msg);
    }
}
