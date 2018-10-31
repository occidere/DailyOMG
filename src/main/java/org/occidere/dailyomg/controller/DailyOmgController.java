package org.occidere.dailyomg.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.occidere.dailyomg.crawler.Crawler;
import org.occidere.dailyomg.crawler.GalleryCrawler;
import org.occidere.dailyomg.crawler.ScheduleCrawler;
import org.occidere.dailyomg.notification.LineNotify;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
public class DailyOmgController {

	@Autowired
	private LineNotify lineNotify;

	@Value("${line.bot.api.url}")
	private String lineBotApiUrl;

	/********** request **********/

	@RequestMapping(value = "/request/ohmygirl/image", method = RequestMethod.GET)
	public Mono<List<LinkedHashMap<String, String>>> requestOhmygirlImage(@RequestParam(value = "range", defaultValue = "1") int range) {
		return Mono.fromSupplier(() -> getOhmygirlImageList(range));
	}

	@RequestMapping(value = "/request/ohmygirl/schedule", method = RequestMethod.GET)
	public Mono<List<LinkedHashMap<String, String>>> requestOhmygirlSchedule(@RequestParam(value = "range", defaultValue = "1") int range) {
		return Mono.fromSupplier(() -> getOhmygirlScheduleList(range));
	}


	/********** notify **********/

	@RequestMapping(value = "/notify/line-notify/ohmygirl/image", method = RequestMethod.GET)
	public void notifyLineNotify(@RequestParam(value = "range", defaultValue = "1") int range) {
		CompletableFuture
				.completedFuture(getOhmygirlImageList(range))
				.thenAcceptAsync(lineNotify::sendImages);
	}

	/**
	 * 이미지를 json으로 말아서 line-api-server 의 /push/image 로 전달.
	 * 전송할 사진이 없으면 현재 시간과 함께 전송할 사진이 없다는 텍스트 메시지를 보낸다.
	 * @param range
	 * @throws Exception
	 */
	@RequestMapping(value = "/notify/line/ohmygirl/image", method = RequestMethod.GET)
	public void notifyLineOhmygirlImage(@RequestParam(value = "range", defaultValue = "1") int range) throws Exception {
		List<LinkedHashMap<String, String>> dataList = getOhmygirlImageList(range);
		String url = lineBotApiUrl + "/push/image";

		/* 전송할 이미지가 없는 경우 현재 날짜를 담아 텍스트 메시지를 보낸다 */
		if (CollectionUtils.isEmpty(dataList)) {
			url = lineBotApiUrl + "/push/text"; // 요청 주소를 텍스트 메시지 주소로 변경
			String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")); // 현재 날짜

			/* 전송할 텍스트 메시지 */
			dataList = new ArrayList<LinkedHashMap<String, String>>() {{
				add(new LinkedHashMap<String, String>() {{
					put(date, "새로운 사진 없음");
				}});
			}};
		}

		String jsonBody = new ObjectMapper().writeValueAsString(dataList);
		List<String> res = IOUtils.readLines(getResponse(url, jsonBody), "UTF-8");

		for (String line : res) {
			log.info(line);
		}
	}

	/**
	 * 스케쥴을 json으로 받아서 line-api-server의 /push/text 로 전달
	 * @param range
	 * @throws Exception
	 */
	@RequestMapping(value = "/notify/line/ohmygirl/schedule", method = RequestMethod.GET)
	public void notifyLineOhmygirlSchedule(@RequestParam(value = "range", defaultValue = "1") int range) throws Exception {
		String jsonBody = new ObjectMapper().writeValueAsString(getOhmygirlScheduleList(range));
		List<String> res = IOUtils.readLines(getResponse(lineBotApiUrl + "/push/text", jsonBody), "UTF-8");
		for(String line : res) {
			log.info(line);
		}
	}

	/********** health **********/

	@RequestMapping(value = "/health", method = RequestMethod.GET)
	public Mono<Long> healthCheck() {
		log.info("Health Check!");
		return Mono.just(System.currentTimeMillis());
	}


	@GetMapping("/")
	public Mono<String> hello() {
		return Mono.just("DailyOMG !!!");
	}

	private List<LinkedHashMap<String, String>> getOhmygirlImageList(int range) {
		Crawler crawler = new GalleryCrawler();
		crawler.setRange(range);
		return crawler.getResult();
	}

	private List<LinkedHashMap<String, String>> getOhmygirlScheduleList(int range) {
		Crawler crawler = new ScheduleCrawler();
		crawler.setRange(range);
		return crawler.getResult();
	}

	/**
	 * line-api-server 로 POST 요청
	 * @param jsonBody 요청 바디
	 * @return 응답 인풋 스트림
	 * @throws Exception
	 */
	private InputStream getResponse(String url, String jsonBody) throws Exception {
		HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Content-Type", "application/json");
		conn.setDoInput(true);
		conn.setDoOutput(true);

		IOUtils.write(jsonBody, conn.getOutputStream(), "UTF-8");
		return conn.getInputStream();
	}
}
