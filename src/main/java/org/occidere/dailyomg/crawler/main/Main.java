package org.occidere.dailyomg.crawler.main;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class Main {
	static String galleryUrl = "http://ohmygirl.ml/bbs/board.php?bo_table=gallery&page=1"; // div class=list-desc
	static String postUrl = "http://ohmygirl.ml/bbs/board.php?bo_table=gallery&wr_id=2552"; // div class=view-content

	public static void main(String[] args) throws Exception {
		HttpURLConnection conn = (HttpURLConnection) new URL(galleryUrl).openConnection();
		conn.setRequestProperty("User-Agent", "Mozilla/5.0");

		BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		String line;
		while ((line = br.readLine()) != null) {
			System.out.println(line);
		}


		br.close();
	}
}
