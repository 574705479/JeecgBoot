package org.jeecg.config.vo;

import javax.print.DocFlavor;

/**
 *
 * @author: scott
 * @date: 2022年04月18日 20:35
 */
public class Path {
    /**
     * 文件上传根目录，默认 ./upload
     * 必须在启动时被 @PostConstruct 校验为绝对路径并可写
     */
    private String upload = "./upload";
    private String webapp = "./webapp";

    public String getUpload() {
        return upload;
    }

    public void setUpload(String upload) {
        this.upload = upload;
    }

    public String getWebapp() {
        return webapp;
    }

    public void setWebapp(String webapp) {
        this.webapp = webapp;
    }
}
