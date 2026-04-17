package org.jeecg.modules.airag.cs.interceptor;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 支持多次读取请求体的 HttpServletRequest 包装类。
 * 构造时一次性读完原始 inputStream 缓存到 byte[]，
 * 之后 getInputStream / getReader 每次返回基于该缓存的新流。
 *
 * 注意：仅适合 application/json 等小请求体场景，不要用于 multipart 上传。
 */
public class MultiReadHttpServletRequest extends HttpServletRequestWrapper {

    private final byte[] cachedBody;

    public MultiReadHttpServletRequest(HttpServletRequest request) throws IOException {
        super(request);
        try (InputStream is = request.getInputStream()) {
            this.cachedBody = is == null ? new byte[0] : is.readAllBytes();
        }
    }

    public byte[] getCachedBody() {
        return cachedBody;
    }

    public String getCachedBodyAsString() {
        String enc = getCharacterEncoding();
        return new String(cachedBody, enc != null ? java.nio.charset.Charset.forName(enc) : StandardCharsets.UTF_8);
    }

    @Override
    public ServletInputStream getInputStream() {
        return new CachedServletInputStream(cachedBody);
    }

    @Override
    public BufferedReader getReader() {
        String enc = getCharacterEncoding();
        return new BufferedReader(new InputStreamReader(getInputStream(),
                enc != null ? java.nio.charset.Charset.forName(enc) : StandardCharsets.UTF_8));
    }

    private static class CachedServletInputStream extends ServletInputStream {
        private final ByteArrayInputStream buffer;

        CachedServletInputStream(byte[] data) {
            this.buffer = new ByteArrayInputStream(data);
        }

        @Override
        public boolean isFinished() {
            return buffer.available() == 0;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            throw new UnsupportedOperationException("setReadListener is not supported");
        }

        @Override
        public int read() {
            return buffer.read();
        }

        @Override
        public int read(byte[] b, int off, int len) {
            return buffer.read(b, off, len);
        }
    }
}
