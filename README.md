# BrowserStack Automation Assignment

## Tech Stack
- Java
- Maven
- Selenium WebDriver
- TestNG
- BrowserStack Integration
- Jsoup (for HTML parsing)
- OkHttp (for REST API integration)
- Google Translate API

## Problem Statement
1. Scrape 5 articles from "Opinion" section of El País.
2. Extract and print article titles and content.
3. Download cover images locally.
4. Translate titles to English using Google Translate API.
5. Identify and print repeated words (3+ times).
6. Run 5 parallel tests on BrowserStack (Desktop & Mobile).

##  Project Structure

src/
├── main/
│ └── java/
│ └── utils/
│ ├── Translator.java
│ └── ImageDownloader.java
├── test/
│ └── java/
│ └── tests/
│ ├── ScraperTest.java
│ └── BrowserStackTest.java
└── resources/
└── browserstack-config.json


🚀 How to Run Tests

✅ Local Execution

mvn clean test -DsuiteXmlFile=testng-local.xml

🌐 BrowserStack Execution (5 Parallel Sessions)
Set your BrowserStack credentials in resources/browserstack-config.json.

Run the tests:

mvn clean test -DsuiteXmlFile=testng-browserstack.xml
