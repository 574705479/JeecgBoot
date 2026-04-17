package org.jeecg.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.config.tencent.JeecgTencent;
import org.jeecg.config.vo.*;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Role;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Paths;


/**
 * 加载项目配置
 * @author: jeecg-boot
 */
@Slf4j
@Component("jeecgBaseConfig")
@ConfigurationProperties(prefix = "jeecg")
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class JeecgBaseConfig {

    /**
     * 启动校验：确保 jeecg.path.upload 配置存在且目录可创建/可写。
     * 防止启动后才在文件上传时报错。
     */
    @PostConstruct
    public void validatePath() {
        if (path == null) {
            path = new Path();
        }
        String uploadPath = path.getUpload();
        if (uploadPath == null || uploadPath.trim().isEmpty()) {
            uploadPath = "./upload";
            path.setUpload(uploadPath);
            log.warn("[JeecgBaseConfig] jeecg.path.upload 未配置，使用默认路径: {}", uploadPath);
        }
        try {
            File dir = Paths.get(uploadPath).toAbsolutePath().normalize().toFile();
            if (!dir.exists() && !dir.mkdirs()) {
                throw new IllegalStateException("无法创建上传目录: " + dir.getAbsolutePath());
            }
            if (!dir.isDirectory() || !dir.canWrite()) {
                throw new IllegalStateException("上传目录不可写: " + dir.getAbsolutePath());
            }
            log.info("[JeecgBaseConfig] 文件上传根目录: {}", dir.getAbsolutePath());
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("校验 jeecg.path.upload 失败: " + uploadPath, e);
        }
    }

    /**
     * 签名密钥串(字典等敏感接口)
     * @TODO 降低使用成本加的默认值,实际以 yml配置 为准
     */
    private String signatureSecret = "dd05f1c54d63749eda95f9fa6d49v442a";
    /**
     * 自定义后台资源前缀，解决表单设计器无法通过前端nginx转发访问
     */
    private String customResourcePrefixPath;
    /**
     * 需要加强校验的接口清单
     */
    private String signUrls;
    /**
     * 上传模式
     * 本地：local、阿里云：alioss
     */
    private String uploadType;
    
    /**
     * 平台安全模式配置
     */
    private Firewall firewall;
    
    /**
     * shiro拦截排除
     */
    private Shiro shiro;
    /**
     * 上传文件配置
     */
    private Path path;

    /**
     * 前端页面访问地址
     * pc: http://localhost:3100
     * app: http://localhost:8051
     */
    private DomainUrl domainUrl;

    /**
     * 文件预览
     */
    private String fileViewDomain;
     /**
     * ES配置
     */
    private Elasticsearch elasticsearch;

    /**
     * 微信支付
     * @return
     */
    private WeiXinPay weiXinPay;

    /**
     * 百度开放API配置
     */
    private BaiduApi baiduApi;

    /**
     * oss配置
     */
    @Getter
    @Setter
    private JeecgOSS oss;

    /**
     * 短信发送方式 aliyun阿里云短信 tencent腾讯云短信
     */
    @Getter
    @Setter
    private String smsSendType = "aliyun";
    
    /**
     * 腾讯配置
     */
    @Getter
    @Setter
    private JeecgTencent tencent;

    public String getCustomResourcePrefixPath() {
        return customResourcePrefixPath;
    }

    public void setCustomResourcePrefixPath(String customResourcePrefixPath) {
        this.customResourcePrefixPath = customResourcePrefixPath;
    }

    public Elasticsearch getElasticsearch() {
        return elasticsearch;
    }

    public void setElasticsearch(Elasticsearch elasticsearch) {
        this.elasticsearch = elasticsearch;
    }

    public Firewall getFirewall() {
        return firewall;
    }

    public void setFirewall(Firewall firewall) {
        this.firewall = firewall;
    }

    public String getSignatureSecret() {
        return signatureSecret;
    }

    public void setSignatureSecret(String signatureSecret) {
        this.signatureSecret = signatureSecret;
    }

    public Shiro getShiro() {
        return shiro;
    }

    public void setShiro(Shiro shiro) {
        this.shiro = shiro;
    }

    public Path getPath() {
        return path;
    }

    public void setPath(Path path) {
        this.path = path;
    }

    public DomainUrl getDomainUrl() {
        return domainUrl;
    }

    public void setDomainUrl(DomainUrl domainUrl) {
        this.domainUrl = domainUrl;
    }
    public String getSignUrls() {
        return signUrls;
    }

    public void setSignUrls(String signUrls) {
        this.signUrls = signUrls;
    }


    public String getFileViewDomain() {
        return fileViewDomain;
    }

    public void setFileViewDomain(String fileViewDomain) {
        this.fileViewDomain = fileViewDomain;
    }

    public String getUploadType() {
        return uploadType;
    }

    public void setUploadType(String uploadType) {
        this.uploadType = uploadType;
    }

    public WeiXinPay getWeiXinPay() {
        return weiXinPay;
    }

    public void setWeiXinPay(WeiXinPay weiXinPay) {
        this.weiXinPay = weiXinPay;
    }

    public BaiduApi getBaiduApi() {
        return baiduApi;
    }

    public void setBaiduApi(BaiduApi baiduApi) {
        this.baiduApi = baiduApi;
    }

}
