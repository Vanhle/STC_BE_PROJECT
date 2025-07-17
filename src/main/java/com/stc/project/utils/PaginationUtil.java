package com.stc.project.utils;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.web.util.UriComponentsBuilder;

// Sinh các HTTP Headers liên quan đến phân trang để trả về trong response của API REST,
//     Tạo các URL tương ứng cho từng trang (next, prev, first, last) dựa trên baseUrl, số trang (page) và kích thước trang (size).
public final class PaginationUtil {

    private PaginationUtil() {
    }

    // Tạo HTTP Headers chứa thông tin về phân trang cho phản hồi HTTP.
    public static HttpHeaders generatePaginationHttpHeaders(Page page, String baseUrl) {

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Total-Count", Long.toString(page.getTotalElements())); // tổng số bản ghi
        String link = ""; // URL đến các trang liên quan (next, prev, last, first) để frontend có thể chuyển trang
        if ((page.getNumber() + 1) < page.getTotalPages()) {
            link = "<" + generateUri(baseUrl, page.getNumber() + 1, page.getSize()) + ">; rel=\"next\",";
        }
        // prev link
        if ((page.getNumber()) > 0) {
            link += "<" + generateUri(baseUrl, page.getNumber() - 1, page.getSize()) + ">; rel=\"prev\",";
        }
        // last and first link
        int lastPage = 0;
        if (page.getTotalPages() > 0) {
            lastPage = page.getTotalPages() - 1;
        }
        link += "<" + generateUri(baseUrl, lastPage, page.getSize()) + ">; rel=\"last\",";
        link += "<" + generateUri(baseUrl, 0, page.getSize()) + ">; rel=\"first\"";
        headers.add(HttpHeaders.LINK, link);
        return headers;
    }

    // Sinh URL phân trang hoàn chỉnh bằng UriComponentsBuilder
    private static String generateUri(String baseUrl, int page, int size) {
        return UriComponentsBuilder.fromUriString(baseUrl).queryParam("page", page).queryParam("size", size).toUriString();
    }
}
