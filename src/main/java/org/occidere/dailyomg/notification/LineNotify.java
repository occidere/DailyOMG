package org.occidere.dailyomg.notification;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.occidere.dailyomg.util.Pair;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

@Slf4j
public class LineNotify {
	private String apiKey;
	private final String LINE_NOTIFY_API = "https://notify-api.line.me/api/notify";

	public LineNotify() {
		try {
			init();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void init() throws Exception {
		PropertiesConfiguration config = new PropertiesConfiguration("line.properties");
		apiKey = config.getString("line.notify.api.key");
	}

	public void sendImages(List<Pair<String, String>> pairList) {
		for (Pair<String, String> titleImagePair : pairList) {
			try {
				sendImage(titleImagePair);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void sendImage(Pair<String, String> titleImagePair) throws IOException {
		HttpURLConnection conn = (HttpURLConnection) new URL(LINE_NOTIFY_API).openConnection();
		conn.setRequestMethod("POST");
		conn.setDoInput(true);
		conn.setDoOutput(true);
		conn.setRequestProperty("authorization", "Bearer " + apiKey);

		String title = titleImagePair.getFirst();
		String image = titleImagePair.getSecond();
		String thumb = image;

		String message = String.format("message=%s&imageThumbnail=%s&imageFullsize=%s", title, thumb, image);

		try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream()))) {
			bw.write(message);
		} catch (IOException e) {
			e.printStackTrace();
		}

		BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		StringBuilder res = new StringBuilder();
		String line;
		while ((line = br.readLine()) != null) {
			res.append(line);
		}
		br.close();

		log.info("{} => {}", image, res.toString());
	}
}
