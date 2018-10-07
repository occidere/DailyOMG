package org.occidere.dailyomg.crawler;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Connection.Method;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class ScheduleCrawler extends Crawler {
	private static final String SCHEDULE_URL = "http://ohmygirl.ml/bbs/board.php?bo_table=omg_schedule";

	@Override
	public List<LinkedHashMap<String, String>> getResult() {
		List<LinkedHashMap<String, String>> scheduleList = new ArrayList<>();

		try {
			LocalDate now = LocalDate.now(ZoneId.of("Asia/Seoul"));
			String params = String.format("&year=%04d&month=%02d", now.getYear(), now.getMonthValue());

			Document doc = openConnection(SCHEDULE_URL + params, Method.GET, null, null);
			Elements mediaNoMargins = doc.getElementsByClass("media no-margin");

			for (Element mediaNoMargin : mediaNoMargins) {
				String date = mediaNoMargin.getElementsByClass("visible-xs").first().text();

				if (isInRange(date)) {
					scheduleList.addAll(
							mediaNoMargin.getElementsByTag("li").stream()
									.map(li ->
											new LinkedHashMap<String, String>() {{
												put(date, StringUtils.trimToEmpty(li.text()));
											}}
									)
									.collect(Collectors.toList())
					);
				}
			}
		} catch (Exception e) {
			log.error(e.getMessage());
		}

		return scheduleList;
	}

	@Override
	protected boolean isInRange(String date) {
		LocalDate now = LocalDate.now(ZoneId.of("Asia/Seoul"));
		// 10.08(월) -> 2018.10.08
		date = String.format("%04d.%s", now.getYear(), date.substring(0, 5));

		LocalDate postDate = LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy.MM.dd"));
		int days = Period.between(now, postDate).getDays();

		return 0 <= days && days <= range;
	}
}
