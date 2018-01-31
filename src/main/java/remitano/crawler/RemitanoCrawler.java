package remitano.crawler;

import java.io.IOException;
import java.util.List;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

public class RemitanoCrawler {
	private WebDriver driver;
	private static final String REQUEST_URL = "https://slack.com/api/chat.postMessage?token=xoxp-264540871856-264540872416-265119749955-1f2d9b5007ee705fc25d97e245f23559&channel=C7SKWS7BK&pretty=1&text=";

	public void createService() throws IOException {
		System.setProperty("webdriver.chrome.driver", "/Users/ea/Downloads/chromedriver");
		// Add options to Google Chrome. The window-size is important for
		// responsive sites
		ChromeOptions options = new ChromeOptions();
		options.addArguments("headless");
		options.addArguments("window-size=1200x600");
		options.addArguments("--mute-audio");
		driver = new ChromeDriver(options);
	}

	public void stopService() throws IOException {
		driver.close();
		driver.quit();
		driver = null;
		Runtime.getRuntime().exec("killall chromedriver");
//		Runtime.getRuntime().exec("killall chrome");
		System.out.println("Service stopped");
	}

	public float crawlRemitano() throws InterruptedException {
		System.out.println("Starting remi crawler");
		driver.get("https://remitano.com/vn");
		Thread.sleep(30 * 1000);
		List<WebElement> priceList = driver.findElements(By.cssSelector("strong.offer-price.text-success"));
		priceList.get(0).click();
		for (int i = 0; i < 5; i++) {
			try {
				String priceText = priceList.get(i).getText().replaceAll("VND/bitUSD", "").replaceAll(",", ".").trim();
				float price = Float.parseFloat(priceText);
				System.out.println("Best price is : " + price);
				if (price >= 23.5) {
					return price;
				} else {
					return 0;
				}
			} catch (Exception e) {

			}
		}
		return 0;
	}

	public float crawlBlockchain() {
		System.out.println("Starting blockchain crawler");
		driver.get("https://blockchain.info/unconfirmed-transactions");
		List<WebElement> unconfirmElement = driver.findElements(By.cssSelector("#header"));
		String unconfirmString = unconfirmElement.get(0).getText().replaceAll("Unconfirmed Transactions", "").trim();
		return Float.parseFloat(unconfirmString);
	}

	public void sendSlackMessage(String message) throws IOException {
		CloseableHttpClient httpclient = HttpClients.createDefault();
		try {
			HttpGet httpget = new HttpGet(REQUEST_URL + message);

			System.out.println("Executing request " + httpget.getRequestLine());

			CloseableHttpResponse responseBody = httpclient.execute(httpget);
			System.out.println("----------------------------------------");
			System.out.println(responseBody);
		} finally {
			httpclient.close();
		}
	}

	public void crawl() throws IOException, InterruptedException {
		createService();
		float result = 0;
		float unconfirms = 0;
		try {
			result = crawlRemitano();
			unconfirms = crawlBlockchain();
			if (result > 0) {
				System.out.println(result);
				sendSlackMessage(String.valueOf(result) + "+" + String.valueOf(unconfirms));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		stopService();
	}

	public static void main(String[] args) throws IOException, InterruptedException {
		RemitanoCrawler remi = new RemitanoCrawler();
		while (true) {
			try {
				remi.crawl();
			} catch (Exception e) {
				e.printStackTrace();
			}
			Thread.sleep(5 * 60 * 1000);
		}
	}
}
