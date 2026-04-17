package org.jeecg.modules.system.storage;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.region.Region;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 腾讯云 COS 客户端池
 * 同样的 region+secretId+secretKey+globalAccel 复用同一 client，避免每次上传都 new+shutdown。
 */
@Slf4j
@Service
public class CosClientPool {

    private final ConcurrentHashMap<Key, COSClient> pool = new ConcurrentHashMap<>();

    public COSClient acquire(String regionId, String secretId, String secretKey, boolean globalAccelerate) {
        Key key = new Key(regionId, secretId, secretKey, globalAccelerate);
        return pool.computeIfAbsent(key, k -> {
            log.info("[CosClientPool] build new COS client: region={}, accel={}", regionId, globalAccelerate);
            ClientConfig cfg = new ClientConfig(new Region(regionId));
            if (globalAccelerate) {
                cfg.setEndPointSuffix("cos.accelerate.myqcloud.com");
            }
            return new COSClient(new BasicCOSCredentials(secretId, secretKey), cfg);
        });
    }

    public void invalidateAll() {
        for (Map.Entry<Key, COSClient> e : pool.entrySet()) {
            try {
                e.getValue().shutdown();
            } catch (Exception ex) {
                log.warn("[CosClientPool] shutdown client failed: {}", ex.getMessage());
            }
        }
        pool.clear();
        log.info("[CosClientPool] invalidated all COS clients");
    }

    @PreDestroy
    public void destroy() {
        invalidateAll();
    }

    private static final class Key {
        final String region;
        final String sid;
        final String skey;
        final boolean accel;

        Key(String region, String sid, String skey, boolean accel) {
            this.region = region;
            this.sid = sid;
            this.skey = skey;
            this.accel = accel;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key)) return false;
            Key k = (Key) o;
            return accel == k.accel
                    && Objects.equals(region, k.region)
                    && Objects.equals(sid, k.sid)
                    && Objects.equals(skey, k.skey);
        }

        @Override
        public int hashCode() {
            return Objects.hash(region, sid, skey, accel);
        }
    }
}
