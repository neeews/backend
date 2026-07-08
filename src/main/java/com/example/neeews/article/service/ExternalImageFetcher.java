package com.example.neeews.article.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;

@Slf4j
@Component
public class ExternalImageFetcher {

    private static final int MAX_REDIRECTS = 3;
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36";

    public record FetchedImage(byte[] bytes, String ext) {}

    public FetchedImage fetch(String url) {
        try {
            return fetchInternal(url, 0);
        } catch (Exception e) {
            log.warn("[이미지 다운로드] 실패 url={}: {}", url, e.getMessage());
            return null;
        }
    }

    private FetchedImage fetchInternal(String urlStr, int redirectCount) throws Exception {
        URL url = new URL(urlStr);
        if (!"http".equals(url.getProtocol()) && !"https".equals(url.getProtocol())) {
            throw new IOException("지원하지 않는 프로토콜입니다: " + url.getProtocol());
        }
        assertPublicAddress(url);

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        if (conn instanceof HttpsURLConnection https) {
            SSLContext ctx = SSLContext.getInstance("TLSv1.2");
            ctx.init(null, null, null);
            https.setSSLSocketFactory(ctx.getSocketFactory());
        }
        conn.setRequestProperty("User-Agent", USER_AGENT);
        conn.setRequestProperty("Accept", "image/webp,image/apng,image/*,*/*;q=0.8");
        conn.setConnectTimeout(8_000);
        conn.setReadTimeout(15_000);
        conn.setInstanceFollowRedirects(false);

        int status = conn.getResponseCode();
        if (status >= 300 && status < 400) {
            String location = conn.getHeaderField("Location");
            if (location == null || redirectCount >= MAX_REDIRECTS) {
                throw new IOException("리다이렉트 처리 실패 (status=" + status + ")");
            }
            return fetchInternal(new URL(url, location).toString(), redirectCount + 1);
        }
        if (status != HttpURLConnection.HTTP_OK) {
            throw new IOException("다운로드 실패: HTTP " + status);
        }

        String contentType = conn.getContentType();
        String ext = "jpg";
        if (contentType != null) {
            if (contentType.contains("png")) ext = "png";
            else if (contentType.contains("gif")) ext = "gif";
            else if (contentType.contains("webp")) ext = "webp";
        }

        byte[] bytes = conn.getInputStream().readAllBytes();
        return new FetchedImage(bytes, ext);
    }

    /**
     * SSRF 방지: 내부망/루프백/링크로컬 등 사설 주소로 향하는 요청을 차단한다.
     * 리다이렉트를 수동으로 따라가며 매 홉마다 재검증한다.
     */
    private void assertPublicAddress(URL url) throws IOException {
        InetAddress[] addresses = InetAddress.getAllByName(url.getHost());
        for (InetAddress addr : addresses) {
            if (addr.isLoopbackAddress() || addr.isLinkLocalAddress() || addr.isSiteLocalAddress()
                    || addr.isAnyLocalAddress() || addr.isMulticastAddress()) {
                throw new IOException("허용되지 않는 대상 주소입니다: " + url.getHost());
            }
        }
    }
}
