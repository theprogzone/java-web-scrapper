package com.codebuy.scraper;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

import com.codebuy.scraper.db.MongoConnection;
import com.codebuy.scraper.dto.InputDTO;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Main {

    private static final String WEBSITE_URL = "https://hk.indeed.com/jobs";

    private static void scrapePage(Document document, MongoDatabase mongoDatabase) {
        try {
            Elements repositories = document.select("a.tapItem");
            MongoCollection<org.bson.Document> dbCollection = mongoDatabase.getCollection("jobs");
            List<org.bson.Document> basicDBObjects = new ArrayList<>();

            for (Element repository : repositories) {
                org.bson.Document document1 = new org.bson.Document();
                System.out.println(WEBSITE_URL.replace("/jobs", "") + repository.attr("href"));
                Map<String, List<String>> urlParamMap = getQueryParams(WEBSITE_URL.replace("/jobs", "") + repository.attr("href"));
                if (urlParamMap.get("jk") != null && !urlParamMap.get("jk").isEmpty()) {
                    System.out.println(urlParamMap.get("jk").get(0));
                    document1.put("_id", urlParamMap.get("jk").get(0));
                    document1.put("adsCardUrl", WEBSITE_URL.replace("/jobs", "") + repository.attr("href"));
                    System.out.println("original : " + WEBSITE_URL.replace("/jobs", "") + "/applystart?jk=" + urlParamMap.get("jk").get(0));
                    document1.put("original", WEBSITE_URL.replace("/jobs", "") + "/applystart?jk=" + urlParamMap.get("jk").get(0));
                    List<Element> titleElements = repository.select("span");
                    Optional<Element> titleElement = titleElements.stream().filter(t -> t.hasAttr("title")).findFirst();
                    if (titleElement.isPresent()) {
                        System.out.println(titleElement.get().text());
                        document1.put("title", titleElement.get().text());
                    }

                    List<Element> companyInfoElements = repository.select(".companyInfo");
                    for (Element companyProperty : companyInfoElements) {
                        Element companyNameEle = companyProperty.select(".companyName").first();
                        if (companyNameEle != null) {
                            System.out.println(companyNameEle.text());
                            document1.put("companyName", companyNameEle.text());
                        }
                        Element companyLocationEle = companyProperty.select(".companyLocation").first();
                        if (companyLocationEle != null) {
                            System.out.println(companyLocationEle.text());
                            document1.put("location", companyLocationEle.text());
                        }
                    }

                    Element salaryEle = repository.select(".salary-snippet-container").first();
                    if (salaryEle != null) {
                        System.out.println(salaryEle.text());
                        document1.put("salary", salaryEle.text());
                    } else {
                        document1.put("salary", "");
                    }

                    Element dateEle = repository.select(".date").first();
                    if (dateEle != null) {
                        String postedDate = dateEle.text().replace("Posted", "");
                        LocalDate localDate;
                        if ("today".equalsIgnoreCase(postedDate) || "just posted".equalsIgnoreCase(postedDate)) {
                            localDate = LocalDate.now();
                        } else {
                            String dayCount = postedDate.split("\\s")[0];
                            if ("30+".equals(dayCount)) {
                                localDate = LocalDate.now().minus(30, ChronoUnit.DAYS);
                            } else {
                                if (StringUtils.isNumeric(dayCount)) {
                                    localDate = LocalDate.now().minus(Long.parseLong(dayCount), ChronoUnit.DAYS);
                                } else {
                                    localDate = LocalDate.now();
                                }
                            }
                        }
                        System.out.println(localDate.toString());
                        document1.put("postDate", localDate.toString());
                    }
                    basicDBObjects.add(document1);
                    BasicDBObject query = new BasicDBObject();
                    query.put("_id", document1.get("_id"));
                    if (dbCollection.find(query).first() == null) {
                        dbCollection.insertOne(document1);
                    }
                    System.out.println();
                }
            }
        } catch (Exception e) {
            System.out.println("Error : " + e.getMessage());
        }
    }

    private static void getJobDetails(InputDTO inputDTO, MongoConnection mongoConnection) {
        MongoClient mongoClient = mongoConnection.getMongoClient();
        try {
            System.out.println(inputDTO);
            System.out.println();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(WEBSITE_URL);
            stringBuilder.append("?q=");
            stringBuilder.append(inputDTO.getWhat());
            stringBuilder.append("&l=");
            stringBuilder.append(inputDTO.getWhere());

            String url = stringBuilder.toString();
            System.out.println("Url : " + url);
            System.out.println();
            Document doc = Jsoup.connect(url).get();

            System.out.println("Website Title: " + doc.title());
            System.out.println();

            MongoDatabase mongoDatabase = mongoClient.getDatabase("indeed");
            scrapePage(doc, mongoDatabase);
            for (int i = 1; i < 67; i++) {
                String urlWithPagination = stringBuilder.toString() + "&start=" + (i * 10);
                System.out.println(urlWithPagination);
                Document document;
                try {
                    document = Jsoup.connect(urlWithPagination).get();
                    scrapePage(document, mongoDatabase);
                } catch (MalformedURLException e) {
                    System.out.println("Error : invalid url");
                    break;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            mongoClient.close();
        }
    }

    public static Map<String, List<String>> getQueryParams(String url) {
        try {
            Map<String, List<String>> params = new HashMap<String, List<String>>();
            String[] urlParts = url.split("\\?");
            if (urlParts.length > 1) {
                String query = urlParts[1];
                for (String param : query.split("&")) {
                    String[] pair = param.split("=");
                    String key = URLDecoder.decode(pair[0], "UTF-8");
                    String value = "";
                    if (pair.length > 1) {
                        value = URLDecoder.decode(pair[1], "UTF-8");
                    }

                    List<String> values = params.get(key);
                    if (values == null) {
                        values = new ArrayList<String>();
                        params.put(key, values);
                    }
                    values.add(value);
                }
            }
            return params;
        } catch (UnsupportedEncodingException ex) {
            throw new AssertionError(ex);
        }
    }

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);
        System.out.println("What : ");
        String what = scanner.nextLine();
        System.out.println("Where : ");
        String where = scanner.nextLine();

        MongoConnection mongoConnection = new MongoConnection("localhost", 27017, "", "");

        InputDTO inputDTO = new InputDTO(what, where);
        getJobDetails(inputDTO, mongoConnection);
    }
}
