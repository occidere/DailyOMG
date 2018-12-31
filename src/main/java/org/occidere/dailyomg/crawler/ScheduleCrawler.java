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

				// 9.01 -> 09.01
				if (date.split("\\.")[0].length() < 2) {
					date = "0" + date;
				}

				if (isInRange(date)) {
					List<LinkedHashMap<String, String>> dailyScheduleList = new ArrayList<>();

					// 일정이 있으면 일정 리스트 값을, 없으면 없다는 메시지를 담을 준비
					Elements scheduleElements = mediaNoMargin.getElementsByTag("li");
					List<String> schedules = scheduleElements.eachText();
					if (schedules.isEmpty()) {
						schedules = new ArrayList<>();
						schedules.add("공식 일정 없음");
					}

					// 일정 값을 trim
					for (String schedule : schedules) {
						LinkedHashMap<String, String> dateScheduleMap = new LinkedHashMap<>();
						dateScheduleMap.put(date, StringUtils.trimToEmpty(schedule));
						dailyScheduleList.add(dateScheduleMap);
					}

					scheduleList.addAll(dailyScheduleList); // 일정 전부 리스트에 담는다
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			log.error(e.getMessage());
		}

		log.info("{}", scheduleList);

		return scheduleList; // 1일부터 31일까지 순차 크롤링이기 때문에 항상 정렬이 되어있다
	}

	@Override
	protected boolean isInRange(String date) {
		System.out.println("Date = " + date);
		LocalDate now = LocalDate.now(ZoneId.of("Asia/Seoul"));
		// 10.08(월) -> 2018.10.08
		date = String.format("%04d.%s", now.getYear(), date.substring(0, 5));

		LocalDate postDate = LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy.MM.dd"));
		long diff = ChronoUnit.DAYS.between(LocalDate.now(), postDate); // 미래면 양수, 현재는 0이 나옴
//		log.info("0 <= diff({}) < range({}) ?", diff, range);

		return 0 <= diff && diff <= range;
	}
}
