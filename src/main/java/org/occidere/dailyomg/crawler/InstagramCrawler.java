package org.occidere.dailyomg.crawler;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Connection.Method;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class InstagramCrawler extends Crawler {

	// TODO: 드라이버 경로 OS 맞출 것
	@Value("${chromedriver.path")
	private String chromedriverPath = "/Users/occidere/Downloads/chromedriver";
	public static final String INSTAGRAM_URL = "https://www.instagram.com";
	public static String url;

	public InstagramCrawler(String url) {
		this.url = url;
	}

	@Override
	public List<LinkedHashMap<String, String>> getResult() {
		return getResultInRange(url);
	}

	/**
	 * url 에 해당하는 인스타그램과 연결된 상태의 WebDriver 객체를 반환한다
	 *
	 * @param url 연결할 인스타그램 url
	 * @return url 에 해당하는 인스타그램과 연결된 상태의 WebDriver 객체
	 */
	private WebDriver getChromeDriver(String url) { // url: https://www.instagram.com/wm_ohmygirl/?hl=ko
		System.setProperty("webdriver.chrome.driver", chromedriverPath);

		ChromeOptions chromeOptions = new ChromeOptions();
		chromeOptions.addArguments("headless");
//		chromeOptions.addArguments("--window-size=1920,1080");

		WebDriver driver = new ChromeDriver(chromeOptions);
		driver.get(url); // https://www.instagram.com/wm_ohmygirl/?hl=ko

		return driver;
	}

	/**
	 * url 로 부터 지정한 {@code range} 내의 사진들을 모두 가져온다.
	 *
	 * @param url 사진을 가져올 url
	 * @return 지정한 range 에 포함되는 url의 사진들이 담긴 List
	 */
	private List<LinkedHashMap<String, String>> getResultInRange(String url) {
		List<LinkedHashMap<String, String>> imageMapList = new ArrayList<>();
		WebDriver driver = getChromeDriver(url);

		try {
			log.info("URL: {}", url);

			// TODO: 컬렉션들 OOM 발생 방지 대책 마련
			Set<String> postUrlSet = new HashSet<>();

			boolean inRange = true;
			int retryCount = 5;

			// 더 이상 사진을 못 가져왔거나, 날짜 범위를 초과하기 전 까지 계속 스크롤 내리면서 크롤링
			while (retryCount > 0 && inRange) {
				// 포스팅 주소들 가져옴
				List<String> postUrlList = getPostList(getHtml(driver, "section"));

				// 이미 가져왔던 주소들이랑 중복 제외한 새로운 주소만 모음
				List<String> uniqueUrlList = postUrlList.stream()
						.filter(postUrl -> !postUrlSet.contains(postUrl))
						.collect(Collectors.toList());

				// set에 신규 주소 추가
				postUrlSet.addAll(uniqueUrlList);

				// 포스팅 주소를 새로 못 받아왔으면 스크롤 안하고 잠시 대기 후 재시도
				if (CollectionUtils.isEmpty(uniqueUrlList)) {
					retryCount--;
					log.info("Retry remains: {}", retryCount);
					Thread.sleep(1500);
				} else {
					scrollDown(driver, 1500); // 스크롤 내리고 이미지 로딩 위해 1.5초 대기
				}

				for (String postUrl : uniqueUrlList) {
					Map.Entry<String, List<LinkedHashMap<String, String>>> dateContent = getDateContentMap(postUrl)
							.entrySet().iterator().next();

					String date = dateContent.getKey();
					List<LinkedHashMap<String, String>> contentList = dateContent.getValue();

					if (isInRange(date)) {
						imageMapList.addAll(contentList);
					} else {
						// 날짜 범위 넘은 게시물이 나왔으면 더 이상 크롤링 안해도 됨
						inRange = false;
						break; // TODO: 여기서 멈춰도 되는지 고려
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			driver.quit();
		}

		return imageMapList;
	}

	/**
	 * 인스타 포스팅 주소에 담긴 포스팅 시간과 이미지들의 url을 Map 에 담아 반환한다.
	 *
	 * @param postUrl 인스타 포스팅 주소
	 * @return 포스팅 시간과 이미지들의 url이 담긴 Map
	 */
	private Map<String, List<LinkedHashMap<String, String>>> getDateContentMap(String postUrl) throws Exception {
		log.info("Post URL: {}", postUrl);

		Document doc = openConnection(postUrl, Method.GET, null, null);
		Element article = doc.getElementsByTag("article").get(0);

		// ISO 8601 (2018-09-27T14:19:44.000Z)
		String date = article.getElementsByTag("time").get(0).attr("datetime");
		log.info("Post Date: {}", date);

		// TODO: FFVAD 클래스명 변경 예의주시
		// TODO: 포스팅 내 이미지는 기본으로 2개까지만 노출됨 -> 그 이외는 < > 버튼 눌렀을 때 리액트로 생김. 이 부분 어떻게 할 것인지 고려
		List<LinkedHashMap<String, String>> contentList = article.getElementsByClass("FFVAD").stream()
				.map(imgTag -> new LinkedHashMap<String, String>() {{
					// 제목에 포함된 개행문자 제거
					put(imgTag.attr("alt").replaceAll("[\\n\\r]", ""), imgTag.attr("src"));
				}})
				.collect(Collectors.toList());
		log.info("Post images count: {}", contentList.size());

		return new HashMap<String, List<LinkedHashMap<String, String>>>() {{
			put(date, contentList);
		}};
	}

	@Override
	protected Document openConnection(String url, Method method, Map<String, String> requestHeaders, Map<String, String> requestBody) {
		WebDriver driver = getChromeDriver(url);
		Document doc = Jsoup.parse(getHtml(driver, "section"));
		driver.quit();
		return doc;
	}

	@Override
	protected boolean isInRange(String date) {
		LocalDate postDate = LocalDateTime.parse(date, DateTimeFormatter.ISO_DATE_TIME).toLocalDate(); // ISO 8601
		long days = Math.abs(ChronoUnit.DAYS.between(postDate, LocalDate.now()));
		log.warn("Days: {}\tRange: {}", days, range);

		return days <= range;
	}

	/**
	 * 페이지 스크롤 다운
	 *
	 * @param driver
	 * @param waitMs 스크롤 다운하고 이미지 로딩할 동안 기다릴 대기시간
	 */
	private void scrollDown(WebDriver driver, int waitMs) {
		try {
			((JavascriptExecutor) driver).executeScript("window.scrollBy(0,10000);"); // TODO: 스크롤 값 조절
			Thread.sleep(waitMs);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * section 태그 부분의 html 코드를 가져온다
	 *
	 * @param driver
	 * @return section 태그 부분의 html 코드
	 */
	private String getHtml(WebDriver driver, String tagName) {
		return ((ChromeDriver) driver).findElementByTagName(tagName)
				.getAttribute("outerHTML");
	}

	/**
	 * html 태그로 부터 인스타그램 포스트의 주소(resource 부분)를 가져온다.
	 * ex) /p/BlvUYgmglwz/
	 *
	 * @param html 인스타 포스팅 주소가 담긴 html 소스
	 * @return 인스타 포스팅 주소 (resource 부분)
	 */
	private List<String> getPostList(String html) {
		// TODO: 포스팅 주소가 담긴 부분의 클래스 이름 언제 바뀔 지 모르니 예의주시할 것
		return Jsoup.parse(html).getElementsByClass("v1Nh3 kIKUG  _bz0w")
				.stream()
				.map(x -> x.getElementsByTag("a"))
				.map(e -> INSTAGRAM_URL + e.attr("href"))
				.collect(Collectors.toList());
	}

	public static void main(String[] args) throws Exception {

	}
}
