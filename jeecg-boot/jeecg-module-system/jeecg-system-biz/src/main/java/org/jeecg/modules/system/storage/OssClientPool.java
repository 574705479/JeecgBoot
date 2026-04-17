package org.jeecg.modules.system.storage;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 阿里云 OSS 客户端池
 * 同样的 endpoint+ak+sk+accelerate 复用同一 client，避免每次上传都 new+shutdown。
 * 配置变更时由 StorageUploadServiceImpl 调用 {@link #invalidateAll()}。
 */
@Slf4j
@Service
public class OssClientPool {

    private final ConcurrentHashMap<Key, OSS> pool = new ConcurrentHashMap<>();

    public OSS acquire(String endpoint, String accessKeyId, String accessKeySecret, boolean transferAccelerate) {
        String endpointForClient = transferAccelerate ? "https://oss-accelerate.aliyuncs.com" : endpointWithScheme(endpoint);
        Key key = new Key(endpointForClient, accessKeyId, accessKeySecret, transferAccelerate);
        return pool.computeIfAbsent(key, k -> {
            log.info("[OssClientPool] build new OSS client: endpoint={}, accel={}", endpointForClient, transferAccelerate);
            return new OSSClientBuilder().build(endpointForClient, accessKeyId, accessKeySecret);
        });
    }

    public void invalidateAll() {
        for (Map.Entry<Key, OSS> e : pool.entrySet()) {
            try {
                e.getValue().shutdown();
            } catch (Exception ex) {
                log.warn("[OssClientPool] shutdown client failed: {}", ex.getMessage());
            }
        }
        pool.clear();
        log.info("[OssClientPool] invalidated all OSS clients");
    }

    @PreDestroy
    public void destroy() {
        invalidateAll();
    }

    private static String endpointWithScheme(String endpoint) {
        if (endpoint == null) {
            return null;
        }
        String e = endpoint.trim();
        if (e.isEmpty()) {
            return e;
        }
        String lower = e.toLowerCase();
        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            return e;
        }
        return "https://" + e;
    }

    private static final class Key {
        final String endpoint;
        final String ak;
        final String sk;
        final boolean accel;

        Key(String endpoint, String ak, String sk, boolean accel) {
            this.endpoint = endpoint;
            this.ak = ak;
            this.sk = sk;
            this.accel = accel;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key)) return false;
            Key k = (Key) o;
            return accel == k.accel
                    && Objects.equals(endpoint, k.endpoint)
                    && Objects.equals(ak, k.ak)
                    && Objects.equals(sk, k.sk);
        }

        @Override
        public int hashCode() {
            return Objects.hash(endpoint, ak, sk, accel);
        }
    }
}
