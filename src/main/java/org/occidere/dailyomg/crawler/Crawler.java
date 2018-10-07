package org.occidere.dailyomg.crawler;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Connection.Method;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public abstract class Crawler {
	@Setter
	protected int range = 1;

	// 커넥션은 기본제공 + Property 는 개별설정 제공
	protected Document openConnection(String url, Method method, Map<String, String> requestHeaders, Map<String, String> requestBody) throws IOException {
		Connection conn = Jsoup.connect(url)
				.userAgent("Mozilla/5.0")
				.followRedirects(true)
				.method(method);

		if (CollectionUtils.isEmpty(requestHeaders) == false) {
			conn.headers(requestHeaders);
			log.info("Request Headers: " + requestHeaders);
		}

		if (CollectionUtils.isEmpty(requestBody) == false) {
			conn.data(requestBody);
			log.info("Request Body: " + requestBody);
		}

		Document doc = conn.execute().parse();
		log.info("Connected to " + url);

		return doc;
	}

	protected abstract boolean isInRange(String date);

	// 결과 리턴 동일한 포맷
	public abstract List<LinkedHashMap<String, String>> getResult();

}
