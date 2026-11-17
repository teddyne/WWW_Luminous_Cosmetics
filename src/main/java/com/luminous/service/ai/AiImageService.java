package com.luminous.service.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.util.Set;

@Service
public class AiImageService {

    public record ImageData(byte[] bytes, String mimeType) {
    }

    private static final Logger log = LoggerFactory.getLogger(AiImageService.class);
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp");
    private static final int MAX_REDIRECTS = 3;
    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 10000;

    @Value("${app.ai.image.max-bytes:5242880}")
    private long maxBytes;

    public ImageData downloadFromUrl(String imageUrl) throws IOException {
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new IllegalArgumentException("Thiếu đường dẫn ảnh");
        }
        URI uri;
        try {
            uri = URI.create(imageUrl.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Đường dẫn ảnh không hợp lệ");
        }
        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException("Chỉ chấp nhận ảnh qua đường dẫn HTTPS");
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("Đường dẫn ảnh không hợp lệ");
        }
        assertPublicHost(host);
        byte[] bytes = fetchBytes(uri.toURL(), 0);
        String mimeType = detectMimeType(bytes);
        if (mimeType == null) {
            throw new IllegalArgumentException("Tập tin không phải là ảnh hợp lệ");
        }
        return new ImageData(bytes, mimeType);
    }

    public ImageData readUpload(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Thiếu tập tin ảnh");
        }
        if (file.getSize() > maxBytes) {
            throw new IllegalArgumentException("Ảnh vượt quá 5MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Chỉ chấp nhận ảnh JPEG, PNG, GIF hoặc WEBP");
        }
        byte[] bytes = file.getBytes();
        String mimeType = detectMimeType(bytes);
        if (mimeType == null) {
            throw new IllegalArgumentException("Tập tin không phải là ảnh hợp lệ");
        }
        return new ImageData(bytes, mimeType);
    }

    private byte[] fetchBytes(URL url, int redirects) throws IOException {
        if (redirects > MAX_REDIRECTS) {
            throw new IOException("Ảnh chuyển hướng quá nhiều lần");
        }
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setInstanceFollowRedirects(false);
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        connection.setRequestProperty("User-Agent", "Luminous-AI/1.0");
        connection.setRequestProperty("Accept", "image/*");
        int status = connection.getResponseCode();
        if (status >= 300 && status < 400) {
            String location = connection.getHeaderField("Location");
            connection.disconnect();
            if (location == null || location.isBlank()) {
                throw new IOException("Không tải được ảnh");
            }
            URI next;
            try {
                next = url.toURI().resolve(location);
            } catch (Exception e) {
                throw new IOException("Không tải được ảnh");
            }
            if (!"https".equalsIgnoreCase(next.getScheme())) {
                throw new IllegalArgumentException("Chỉ chấp nhận ảnh qua đường dẫn HTTPS");
            }
            assertPublicHost(next.getHost());
            return fetchBytes(next.toURL(), redirects + 1);
        }
        if (status != HttpURLConnection.HTTP_OK) {
            connection.disconnect();
            throw new IOException("Không tải được ảnh (mã " + status + ")");
        }
        String contentType = connection.getContentType();
        if (contentType != null) {
            String mime = contentType.split(";")[0].trim().toLowerCase();
            if (!ALLOWED_MIME_TYPES.contains(mime)) {
                connection.disconnect();
                throw new IllegalArgumentException("Đường dẫn không trỏ đến ảnh hợp lệ");
            }
        }
        long contentLength = connection.getContentLengthLong();
        if (contentLength > maxBytes) {
            connection.disconnect();
            throw new IllegalArgumentException("Ảnh vượt quá 5MB");
        }
        try (InputStream in = connection.getInputStream();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            long total = 0;
            while ((read = in.read(buffer)) != -1) {
                total += read;
                if (total > maxBytes) {
                    throw new IllegalArgumentException("Ảnh vượt quá 5MB");
                }
                out.write(buffer, 0, read);
            }
            return out.toByteArray();
        } finally {
            connection.disconnect();
        }
    }

    private void assertPublicHost(String host) throws IOException {
        InetAddress[] addresses = InetAddress.getAllByName(host);
        for (InetAddress address : addresses) {
            if (address.isLoopbackAddress() || address.isLinkLocalAddress()
                    || address.isSiteLocalAddress() || address.isMulticastAddress()
                    || address.isAnyLocalAddress()) {
                throw new IllegalArgumentException("Không chấp nhận đường dẫn ảnh nội bộ");
            }
        }
    }

    public static String detectMimeType(byte[] bytes) {
        if (bytes == null || bytes.length < 12) {
            return null;
        }
        if (bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8 && bytes[2] == (byte) 0xFF) {
            return "image/jpeg";
        }
        if (bytes[0] == (byte) 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47) {
            return "image/png";
        }
        if (bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == '8') {
            return "image/gif";
        }
        if (bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
            return "image/webp";
        }
        return null;
    }
}
