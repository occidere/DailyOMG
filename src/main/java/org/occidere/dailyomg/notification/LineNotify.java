package org.occidere.dailyomg.notification;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

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

	public void sendImages(List<LinkedHashMap<String, String>> LinkedHashMapList) {
		for (LinkedHashMap<String, String> titleImageLinkedHashMap : LinkedHashMapList) {
			try {
				sendImage(titleImageLinkedHashMap);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void sendImage(LinkedHashMap<String, String> titleImageMap) throws IOException {
		HttpURLConnection conn = (HttpURLConnection) new URL(LINE_NOTIFY_API).openConnection();
		conn.setRequestMethod("POST");
		conn.setDoInput(true);
		conn.setDoOutput(true);
		conn.setRequestProperty("authorization", "Bearer " + apiKey);

		// LinkedHashMap에는 오직 1개의 원소만 들어가기 때문에 아래와 같이 파싱
		Map.Entry<String, String> titleUrlEntry = titleImageMap.entrySet()
				.iterator()
				.next();

		String title = titleUrlEntry.getKey();
		String image = titleUrlEntry.getValue();
		String thumb = image;

		String message = String.format("message=%s&imageThumbnail=%s&imageFullsize=%s", title, thumb, image);

		try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream()))) {
			bw.write(message);
		} catch (IOException e) {
			e.printStackTrace();
		}

		String res = String.join("", IOUtils.readLines(conn.getInputStream(), "UTF-8"));
		log.info("{} => {}", image, res);
	}
}
