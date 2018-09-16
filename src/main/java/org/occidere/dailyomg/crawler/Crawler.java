package org.occidere.dailyomg.crawler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.List;

public abstract class Crawler {
	private String url;
	private Document doc;

	public Crawler(String url) {
		this.url = url;
		doc = getDocument();
	}

	protected Document getDocument() {
		return (Document) Jsoup.connect(url).userAgent("Mozilla/5.0");
	}

	protected abstract List<String> parseImageList();

}
