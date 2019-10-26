package org.occidere.dailyomg.crawler;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection.Method;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * ohmygirl.ml의 gallery 게시판의 사진들을 크롤링하여 list로 전달
 *
 * @author occidere
 */
@Slf4j
public class GalleryCrawler extends Crawler {

    private static final String GALLERY_URL = "http://ohmygirl.ml/bbs/board.php?bo_table=gallery&page=";

    /**
     * 각 이미지 리스트의 모든 이미지들을 모아서 하나의 이미지 리스트로 묶어 반환
     *
     * @return 모든 이미지가 담긴 리스트
     */
    @Override
    public List<LinkedHashMap<String, String>> getResult() {
        List<LinkedHashMap<String, String>> imageList = new LinkedList<>();
        List<String> galleryUrlList = getGalleryList();

        for (String galleryUrl : galleryUrlList) {
            try {
                imageList.addAll(getImageUrlList(galleryUrl));
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }

        return imageList;
    }

    /**
     * 각 포스트에 담긴 사진들을 리스트에 담아 반환
     *
     * @param url 포스트의 url
     * @return 포스트에 담긴 이미지들의 리스트
     */
    private List<LinkedHashMap<String, String>> getImageUrlList(String url) throws IOException {
        Document doc = openConnection(url, Method.GET, null, null);
        Element viewWrap = doc.getElementsByClass("view-wrap")
                .get(0); // div.class=view-wrap는 1개밖에 없음

        String title = viewWrap.getElementsByTag("h1").text();

        return viewWrap.getElementsByClass("img-tag")
                .stream()
                .map(imgTag -> new LinkedHashMap<String, String>() {{
                    put(title, imgTag.select("img").attr("src"));
                }})
                .collect(Collectors.toList());
    }

    /**
     * 지정한 기간(range) 에 포함되는 게시글들의 url을 하나의 리스트에 담아 반환한다.
     * page를 1부터 시작하여 증가시켜가며 지정한 날짜 범위를 초과하기 전 까지 게시글의 주소를 가져온다.
     *
     * @return 지정 기간에 속하는 게시글들의 url이 담긴 리스트
     */
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

    /**
     * 특정 페이지의 게시글 목록이 주어졌을 때, 해당 목록에 있는 게시글들 중 지정한 범위에 해당하는 게시글의 url 만 리스트로 모아 반환
     *
     * @param page 게시글 목록의 페이지 번호
     * @return 지정 범위 내의 게시글 url이 담긴 리스트. 지정 범위 내의 게시글이 없는 경우 빈 리스트가 전달된다.
     */
    private List<String> getGalleryUrlList(int page) throws IOException {
        List<String> postList = new LinkedList<>();

        Document doc = openConnection(GALLERY_URL + page, Method.GET, null, null);
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

    /**
     * 게시글의 날짜가 지정한 범위 내에 속하는지 검사하는 메서드
     *
     * @param date yy.MM.dd 형식의 게시글의 등록일
     * @return 게시글의 등록일이 범위 내이면 true, 아니면 false
     */
    @Override
    protected boolean isInRange(String date) {
        LocalDate postDate = LocalDate.parse(date, DateTimeFormatter.ofPattern("yy.MM.dd"));
        long diff = Math.abs(ChronoUnit.DAYS.between(postDate, LocalDate.now()));
        log.info("diff({}) <= range({}) ?", diff, range);

        return diff <= range;
    }
}
