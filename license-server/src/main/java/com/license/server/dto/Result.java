package com.license.server.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {
    private int code;
    private T data;
    private String message;

    public static <T> Result<T> ok(T data) {
        return new Result<>(200, data, "success");
    }

    public static <T> Result<T> ok() {
        return new Result<>(200, null, "success");
    }

    public static <T> Result<T> error(int code, String message) {
        return new Result<>(code, null, message);
    }

    public static <T> Result<T> page(java.util.List<T> records, long total, int page, int size) {
        // For page results, use PageResult wrapper
        return new Result<>(200, null, "success");
    }
}
