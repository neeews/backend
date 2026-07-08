package com.example.neeews.article.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.DnsResolver;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.util.Timeout;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

/**
 * 외부 이미지 URL을 다운로드한다.
 * SSRF 방지를 위해 DNS 해석과 실제 연결이 항상 같은(검증된) 주소를 사용하도록
 * Apache HttpClient5의 DnsResolver 훅에서 주소 검증을 수행한다.
 * (URL.openConnection() 방식은 검증 시점과 연결 시점의 DNS 조회가 분리되어
 * DNS 리바인딩에 취약하다.)
 */
@Slf4j
@Component
public class ExternalImageFetcher {

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36";
    private static final int MAX_REDIRECTS = 3;

    public record FetchedImage(byte[] bytes, String ext) {}

    private final CloseableHttpClient httpClient;

    public ExternalImageFetcher() {
        DnsResolver validatingDnsResolver = new DnsResolver() {
            @Override
            public InetAddress[] resolve(String host) throws UnknownHostException {
                InetAddress[] addresses = InetAddress.getAllByName(host);
                for (InetAddress addr : addresses) {
                    if (!isPublicAddress(addr)) {
                        throw new UnknownHostException("허용되지 않는 대상 주소입니다: " + host);
                    }
                }
                return addresses;
            }

            @Override
            public String resolveCanonicalHostname(String host) {
                return host;
            }
        };

        HttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setDnsResolver(validatingDnsResolver)
                .build();

        this.httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setConnectTimeout(Timeout.ofSeconds(8))
                        .setResponseTimeout(Timeout.ofSeconds(15))
                        .setMaxRedirects(MAX_REDIRECTS)
                        .build())
                .build();
    }

    public FetchedImage fetch(String url) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            log.warn("[이미지 다운로드] 지원하지 않는 프로토콜: {}", url);
            return null;
        }

        HttpGet request = new HttpGet(url);
        request.setHeader("User-Agent", USER_AGENT);
        request.setHeader("Accept", "image/webp,image/apng,image/*,*/*;q=0.8");

        try {
            return httpClient.execute(request, response -> {
                if (response.getCode() != 200) {
                    log.warn("[이미지 다운로드] 실패 url={}: HTTP {}", url, response.getCode());
                    return null;
                }
                if (response.getEntity() == null) {
                    return null;
                }
                String contentType = response.getEntity().getContentType();
                String ext = "jpg";
                if (contentType != null) {
                    if (contentType.contains("png")) ext = "png";
                    else if (contentType.contains("gif")) ext = "gif";
                    else if (contentType.contains("webp")) ext = "webp";
                }
                byte[] bytes = EntityUtils.toByteArray(response.getEntity());
                return new FetchedImage(bytes, ext);
            });
        } catch (IOException e) {
            log.warn("[이미지 다운로드] 실패 url={}: {}", url, e.getMessage());
            return null;
        }
    }

    @PreDestroy
    public void close() throws IOException {
        httpClient.close();
    }

    /**
     * 내부망/루프백/링크로컬 등 사설 주소로 향하는 요청을 차단한다.
     * IPv4-mapped IPv6 주소는 매핑된 IPv4 주소 기준으로 재검사한다.
     */
    private boolean isPublicAddress(InetAddress addr) {
        if (addr.isLoopbackAddress() || addr.isLinkLocalAddress() || addr.isSiteLocalAddress()
                || addr.isAnyLocalAddress() || addr.isMulticastAddress()) {
            return false;
        }

        byte[] bytes = addr.getAddress();
        if (bytes.length == 16) {
            if ((bytes[0] & 0xfe) == 0xfc) {
                return false; // fc00::/7 (IPv6 Unique Local Address)
            }
            if (isIpv4MappedOrCompatible(bytes)) {
                try {
                    InetAddress mapped = InetAddress.getByAddress(Arrays.copyOfRange(bytes, 12, 16));
                    return isPublicAddress(mapped);
                } catch (UnknownHostException e) {
                    return false;
                }
            }
        } else if (bytes.length == 4) {
            int b0 = bytes[0] & 0xff;
            int b1 = bytes[1] & 0xff;
            if (b0 == 100 && (b1 & 0xc0) == 64) {
                return false; // 100.64.0.0/10 (CGNAT)
            }
            if (b0 == 192 && b1 == 0 && (bytes[2] & 0xff) == 0) {
                return false; // 192.0.0.0/24 (IETF Protocol Assignments)
            }
            if (b0 == 198 && (b1 == 18 || b1 == 19)) {
                return false; // 198.18.0.0/15 (벤치마킹용 예약 대역)
            }
        }
        return true;
    }

    private boolean isIpv4MappedOrCompatible(byte[] v6) {
        for (int i = 0; i < 10; i++) {
            if (v6[i] != 0) {
                return false;
            }
        }
        // ::0.0.0.0/96 (IPv4-compatible, 마지막 4바이트가 0이 아니면 IPv4로 취급) 또는 ::ffff:0:0/96 (IPv4-mapped)
        return v6[10] == 0 && v6[11] == 0
                || (v6[10] == (byte) 0xff && v6[11] == (byte) 0xff);
    }
}
