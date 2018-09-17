package org.occidere.dailyomg.main;

import org.occidere.dailyomg.crawler.Crawler;
import org.occidere.dailyomg.notification.LineNotify;

public class Main {
	public static void main(String[] args) {
		Crawler crawler = new Crawler();

		if(args.length == 1) {
			crawler.setRecentRange(Integer.parseInt(args[0]));
		}

		LineNotify lineNotify = new LineNotify();
		lineNotify.sendImages(crawler.getImageList());
	}
}
