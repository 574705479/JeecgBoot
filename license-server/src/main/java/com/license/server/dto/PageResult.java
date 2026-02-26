package com.license.server.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {
    private int code = 200;
    private PageData<T> data;
    private String message = "success";

    public static <T> PageResult<T> of(List<T> records, long total, int page, int size) {
        PageResult<T> result = new PageResult<>();
        result.setData(new PageData<>(records, total, page, size));
        return result;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PageData<T> {
        private List<T> records;
        private long total;
        private int page;
        private int size;
    }
}
