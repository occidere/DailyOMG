package org.occidere.dailyomg.crawler;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Connection.Method;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
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
			String params = String.format("&year=%04d&month=%d", now.getYear(), now.getMonthValue());

			Document doc = openConnection(SCHEDULE_URL + params, Method.GET, null, null);
			/* today 만 media bg-today no-margin 이라 정규식으로 잡음 */
			Elements mediaNoMargins = doc.select("[class~=^media.*no-margin$]");

			for (Element mediaNoMargin : mediaNoMargins) {
				String date = mediaNoMargin.getElementsByClass("visible-xs").first().text();

				if (isInRange(date)) {
					List<LinkedHashMap<String, String>> dailyScheduleList = mediaNoMargin.getElementsByTag("li")
							.stream()
							.map(li ->
									new LinkedHashMap<String, String>() {{
										put(date, StringUtils.trimToEmpty(li.text()));
									}}
							)
							.collect(Collectors.toList());

					/* 일정이 없어도 출력하기 위한 값 추가 */
					if (dailyScheduleList.isEmpty()) {
						dailyScheduleList.add(new LinkedHashMap<String, String>() {{
							put(date, "공식 일정 없음");
						}});
					}

					scheduleList.addAll(dailyScheduleList); // 일정 전부 리스트에 담는다
				}
			}
		} catch (Exception e) {
			log.error(e.getMessage());
		}

		log.info("{}", scheduleList);

		return scheduleList; // 1일부터 31일까지 순차 크롤링이기 때문에 항상 정렬이 되어있다
	}

	@Override
	protected boolean isInRange(String date) {
		LocalDate now = LocalDate.now(ZoneId.of("Asia/Seoul"));
		// 10.08(월) -> 2018.10.08
		date = String.format("%04d.%s", now.getYear(), date.substring(0, 5));

		LocalDate postDate = LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy.MM.dd"));
		long diff = ChronoUnit.DAYS.between(LocalDate.now(), postDate); // 미래면 양수, 현재는 0이 나옴
//		log.info("0 <= diff({}) < range({}) ?", diff, range);

		return 0 <= diff && diff <= range;
	}
}
