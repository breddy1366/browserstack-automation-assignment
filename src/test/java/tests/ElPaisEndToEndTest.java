package tests;

import com.aventstack.extentreports.*;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.google.gson.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.*;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.testng.ITestContext;
import org.testng.annotations.*;
import utils.ImageDownloader;
import utils.Translator;

import java.io.File;
import java.io.FileReader;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;

public class ElPaisEndToEndTest {

    private static final String CONFIG_PATH = "resources/browserstack-config.json";
    private static final String DOWNLOAD_FOLDER = "downloads";
    private ExtentReports extent;
    private ThreadLocal<ExtentTest> test = new ThreadLocal<>();

    private JsonObject loadConfig() throws Exception {
        Path configFilePath = Paths.get(CONFIG_PATH);
        File file = configFilePath.toFile();
        if (!file.exists()) {
            throw new RuntimeException("Config not found at " + configFilePath);
        }
        return JsonParser.parseReader(new FileReader(file)).getAsJsonObject();
    }

    @BeforeClass
    public void setupReport() {
        ExtentSparkReporter spark = new ExtentSparkReporter("Reports/ElPaisE2E_Report.html");
        extent = new ExtentReports();
        extent.attachReporter(spark);
    }

    @DataProvider(name = "browserConfigs", parallel = true)
    public Object[][] getBrowserConfigs(ITestContext context) throws Exception {
        JsonArray envs = loadConfig().getAsJsonArray("environments");
        String idx = context.getCurrentXmlTest().getParameter("envIndexes");
        List<Integer> indexes = new ArrayList<>();
        if (idx != null && !idx.isEmpty()) {
            for (String s : idx.split(",")) indexes.add(Integer.parseInt(s.trim()));
        } else {
            for (int i = 0; i < envs.size(); i++) indexes.add(i);
        }
        Object[][] data = new Object[indexes.size()][1];
        for (int i = 0; i < indexes.size(); i++) {
            data[i][0] = envs.get(indexes.get(i)).getAsJsonObject();
        }
        return data;
    }

    @Test(dataProvider = "browserConfigs")
    public void runEndToEnd(JsonObject envCaps) throws Exception {
        ExtentTest logger = extent.createTest("Run on " +
            (envCaps.has("browser") ? envCaps.get("browser").getAsString() : envCaps.get("device").getAsString()));
        test.set(logger);

        JsonObject cfg = loadConfig();
        String user = cfg.get("browserstack.user").getAsString();
        String key = cfg.get("browserstack.key").getAsString();

        MutableCapabilities caps = createCapabilities(envCaps, cfg.getAsJsonObject("capabilities"), logger);
        if (caps == null) return;

        WebDriver driver = startDriverWithRetry(user, key, caps, logger);
        if (driver == null) return;

        try {
            driver.get("https://elpais.com/opinion/");
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
            closeLanguagePopup(driver, logger);

            String body = driver.findElement(By.tagName("body")).getText();
            if (!body.contains("Opinión")) {
                logger.fail("Page not in Spanish. Content Sample " + extractSample(body));
                return;
            }
            logger.pass("Page is in Spanish");

            Document doc = Jsoup.connect("https://elpais.com/opinion/").get();
            Elements articles = doc.select("article");
            if (articles.isEmpty()) {
                logger.fail("No articles found");
                return;
            }

            ensureFolderExists(DOWNLOAD_FOLDER);
            List<String> translated = new ArrayList<>();
            int count = 0;

            for (Element art : articles) {
                if (count >= 5) break;
                String title = java.util.Optional.ofNullable(art.selectFirst("img.ep_i"))
                        .map(e -> e.attr("alt"))
                        .filter(t -> !t.isEmpty())
                        .orElse(art.selectFirst("h2").text());

                if (title.isEmpty()) continue;

                String link = art.selectFirst("a").absUrl("href");
                if (link.isEmpty()) link = "https://elpais.com" + art.selectFirst("a").attr("href");

                Document contentDoc = Jsoup.connect(link).get();
                String content = contentDoc.select("p").stream()
                        .map(Element::text)
                        .reduce("", (a, b) -> a + " " + b);
                logger.info("Title (ES): " + title);
                logger.info("Content Snippet: " + snippet(content));

                String imgUrl = contentDoc.selectFirst("meta[property=og:image]").attr("content");
                if (!imgUrl.isEmpty()) {
                    String path = DOWNLOAD_FOLDER + "/article_" + count + ".jpg";
                    try {
                        ImageDownloader.download(imgUrl, path);
                        logger.info("Image saved: " + path);
                    } catch (Exception e) {
                        logger.warning("Image download failed: " + e.getMessage());
                    }
                }

                String tr;
                try {
                    tr = Translator.translate(title);
                    logger.pass("Translated Title: " + tr);
                } catch (Exception e) {
                    tr = title + " (translated)";
                    logger.warning("Translation fallback: " + tr);
                }

                translated.add(tr);
                count++;
            }

            Map<String, Integer> wc = new HashMap<>();
            for (String t : translated) {
                for (String wd : t.toLowerCase().split("\\W+")) {
                    if (wd.length() < 3) continue;
                    wc.put(wd, wc.getOrDefault(wd, 0) + 1);
                }
            }

            wc.forEach((w, c) -> {
                if (c > 2) logger.info("Repeated word: " + w + " -> " + c);
            });

            logger.pass("Test complete");
        } finally {
            driver.quit();
        }
    }

    private MutableCapabilities createCapabilities(JsonObject env, JsonObject common, ExtentTest log) {
        MutableCapabilities caps = new MutableCapabilities();
        Map<String, Object> opts = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : common.entrySet()) {
            opts.put(entry.getKey(), entry.getValue().getAsString());
        }

        if (env.has("browser")) {
            caps.setCapability("browserName", env.get("browser").getAsString());
            if (env.has("browser_version")) caps.setCapability("browserVersion", env.get("browser_version").getAsString());
            opts.put("os", env.get("os").getAsString());
            opts.put("osVersion", env.get("os_version").getAsString());
        } else if (env.has("device")) {
            caps.setCapability("browserName", "Browser");
            opts.put("deviceName", env.get("device").getAsString());
            opts.put("realMobile", env.get("real_mobile").getAsString());
            opts.put("osVersion", env.get("os_version").getAsString());
        } else {
            log.skip("Invalid envCaps: neither browser nor device");
            return null;
        }

        caps.setCapability("bstack:options", opts);
        return caps;
    }

    private WebDriver startDriverWithRetry(String user, String key, MutableCapabilities caps, ExtentTest log) {
        WebDriver driver = null;
        int max = 5, delay = 5000;
        for (int i = 1; i <= max; i++) {
            try {
                String encodedUser = URLEncoder.encode(user, StandardCharsets.UTF_8.toString());
                String encodedKey = URLEncoder.encode(key, StandardCharsets.UTF_8.toString());
                URL hub = new URL("https://" + encodedUser + ":" + encodedKey + "@hub-cloud.browserstack.com/wd/hub");
                log.info("Starting session (attempt " + i + ")");
                driver = new RemoteWebDriver(hub, caps);
                return driver;
            } catch (Exception e) {
                if (e.getMessage().contains("BROWSERSTACK_QUEUE_SIZE_EXCEEDED") && i < max) {
                    log.warning("Queue full. Retrying in " + (delay / 1000) + "s");
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ignored) {}
                    delay *= 2;
                } else {
                    log.fail("Unable to start session: " + e.getMessage());
                    return null;
                }
            }
        }
        return null;
    }

    private void closeLanguagePopup(WebDriver driver, ExtentTest log) {
        try {
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(3));
            List<By> popupSelectors = Arrays.asList(
                By.id("didomi-notice-agree-button"),
                By.cssSelector("button[title='Aceptar']"),
                By.xpath("//button[contains(text(), 'Aceptar')]"),
                By.xpath("//button[contains(text(), 'Aceptar y continuar')]")
            );
            for (By selector : popupSelectors) {
                List<WebElement> elements = driver.findElements(selector);
                if (!elements.isEmpty()) {
                    elements.get(0).click();
                    log.info("Popup dismissed: " + selector.toString());
                    break;
                }
            }
        } catch (Exception e) {
            log.warning("Popup dismiss failed: " + e.getMessage());
        } finally {
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
        }
    }

    private void ensureFolderExists(String path) {
        File f = new File(path);
        if (!f.exists()) f.mkdirs();
    }

    private String snippet(String s) {
        return s.length() > 200 ? s.substring(0, 200) + "…" : s;
    }

    private String extractSample(String s) {
        return s.length() > 100 ? s.substring(0, 100) + "…" : s;
    }

    @AfterClass
    public void teardown() {
        extent.flush();
    }
}
