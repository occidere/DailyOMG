package org.occidere.dailyomg.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
 import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.occidere.dailyomg.crawler.Crawler;
import org.occidere.dailyomg.notification.LineNotify;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.net.HttpURLConnection;
import java.net.URL;
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


	/********** notify **********/

	@RequestMapping(value = "/notify/line-notify", method = RequestMethod.GET)
	public void notifyLineNotify(@RequestParam(value = "range", defaultValue = "1") int range) {
		CompletableFuture
				.completedFuture(getOhmygirlImageList(range))
				.thenAcceptAsync(lineNotify::sendImages);
	}

	/**
	 * 이미지를 json으로 말아서 line-api-server 의 /push/image 로 전달
	 * @param range
	 * @throws Exception
	 */
	@RequestMapping(value = "/notify/line/daily-omg", method = RequestMethod.GET)
	public void notifyLineDailyOmg(@RequestParam(value = "range", defaultValue = "1") int range) throws Exception {
		HttpURLConnection conn = (HttpURLConnection) new URL(lineBotApiUrl).openConnection();
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Content-Type", "application/json");
		conn.setDoInput(true);
		conn.setDoOutput(true);

		String jsonBody = new ObjectMapper().writeValueAsString(getOhmygirlImageList(range));
		IOUtils.write(jsonBody, conn.getOutputStream(), "UTF-8");

		List<String> res = IOUtils.readLines(conn.getInputStream(), "UTF-8");
		for(String line : res) {
			log.info(line);
		}
	}

	/********** test **********/

	@GetMapping("/test/request/ohmygirl/image")
	public Mono<List<LinkedHashMap<String, String>>> event() throws Exception {
		HttpURLConnection conn = (HttpURLConnection) new URL("http://localhost:8080/request/ohmygirl/image?range=1")
				.openConnection();

		ObjectMapper mapper = new ObjectMapper();
		List<LinkedHashMap<String, String>> imageLinkedHashMapList = mapper.readValue(conn.getInputStream(), List.class);

		imageLinkedHashMapList.forEach(lhm ->log.info("{}", lhm.toString()));

		return Mono.just(imageLinkedHashMapList);
	}

	@GetMapping("/")
	public Mono<String> hello() {
		return Mono.just("DailyOMG !!!");
	}

	private List<LinkedHashMap<String, String>> getOhmygirlImageList(int range) {
		return new Crawler() {{ setRecentRange(range); }}.getImageList();
	}
}
