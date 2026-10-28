package com.example.neeews.huggingface.domain;

import java.util.Set;

// 라이선스 id만으로 조건이 확정되는 것만 판정한다. "other"(직접 만든 라이선스)나 Llama·Gemma처럼
// 조건이 붙은 라이선스를 자동으로 "가능"이라 표시했다가 틀리면 사용자가 믿고 상업 서비스에 쓸 수 있다.
public enum CommercialUse {

    ALLOWED,
    NON_COMMERCIAL,
    CHECK_REQUIRED;

    private static final Set<String> PERMISSIVE_LICENSES = Set.of(
            "mit", "apache-2.0", "bsd-2-clause", "bsd-3-clause", "cc0-1.0", "cc-by-4.0", "unlicense");
    // cc-by-nc-4.0, cc-by-nc-sa-4.0, cc-by-nc-nd-4.0 등 비영리(NonCommercial) 조항이 붙은 CC 라이선스 전부
    private static final String NON_COMMERCIAL_PREFIX = "cc-by-nc";

    public static CommercialUse of(String licenseId) {
        if (licenseId == null) return CHECK_REQUIRED;
        if (PERMISSIVE_LICENSES.contains(licenseId)) return ALLOWED;
        if (licenseId.startsWith(NON_COMMERCIAL_PREFIX)) return NON_COMMERCIAL;
        return CHECK_REQUIRED;
    }
}
