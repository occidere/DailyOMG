package org.occidere.dailyomg.crawler;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.occidere.dailyomg.util.Pair;

import java.io.IOException;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;

@Slf4j
public class Crawler {
	private final String GALLERY_URL = "http://ohmygirl.ml/bbs/board.php?bo_table=gallery&page=";
	private final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yy.MM.dd");
	@Setter
	private int recentRange = 1;

	public List<Pair<String, String>> getImageList() {
		List<Pair<String, String>> imageList = new LinkedList<>();
		List<String> galleryUrlList = getGalleryList();

		for (String galleryUrl : galleryUrlList) {
			try {
				imageList.addAll(getImageUrlList(galleryUrl));
			} catch (Exception e) {
				log.error(e.getMessage());
			}
		}

		System.out.println(imageList);

		return imageList;
	}

	private List<Pair<String, String>> getImageUrlList(String url) throws IOException {
		List<Pair<String, String>> imageUrlList = new LinkedList<>();

		Document doc = Jsoup.connect(url).userAgent("Mozilla/5.0").get();
		Elements viewWraps = doc.getElementsByClass("view-wrap");

		for (Element viewWrap : viewWraps) {
			String title = viewWrap.getElementsByTag("h1").text();
			String imageUrl = viewWrap.getElementsByClass("view-content")
					.select("img")
					.attr("src");

			imageUrlList.add(new Pair(title, imageUrl));
		}


		return imageUrlList;
	}

	private List<String> getGalleryList() {
		List<String> galleryList = new LinkedList<>();

		try {
			for (int page = 1; ; page++) {
				List<String> list = getGalleryUrlList(page);

				if (list.isEmpty()) {
					break;
				} else {
					galleryList.addAll(list);
				}
			}
		} catch (Exception e) {
			log.error(e.getMessage());
		}

		return galleryList;
	}

	private List<String> getGalleryUrlList(int page) throws IOException {
		List<String> postList = new LinkedList<>();

		Document doc = Jsoup.connect(GALLERY_URL + page).userAgent("Mozilla/5.0").get();
		Elements listFronts = doc.getElementsByClass("list-front");

		for (Element listFront : listFronts) {
			Element listDesc = listFront.getElementsByClass("list-desc").first();
			String postUrl = listDesc.select("a").attr("href");
			String date = listFront.getElementsByClass("wr-date en").first().text();

			if (isInRange(date)) {
				postList.add(postUrl);
			}
		}

		return postList;
	}

	private boolean isInRange(String date) {
		LocalDate postDate = LocalDate.parse(date, FORMATTER);
		int days = Period.between(postDate, LocalDate.now()).getDays();

		return days <= recentRange;
	}

	public static void main(String[] args) throws Exception {
		Crawler crawler = new Crawler();
		crawler.getImageList();
	}
}
