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
└── browserstack.yml

### How to Run Tests

### Explanation:

parallel="methods" with thread-count="2" means that a maximum of 2 test methods will run in parallel.
This configuration aligns with the typical free or trial BrowserStack plan, which supports 1–2 concurrent sessions.
In the test class, a DataProvider supplies 5 configurations. With this setting, 2 tests will execute concurrently, while the remaining 3 will wait in the queue.
If I try to run 5 parallel threads, BrowserStack will not support it under the current plan, and the extra sessions will be queued or result in BROWSERSTACK_QUEUE_SIZE_EXCEEDED errors.


### BrowserStack Execution
Set your BrowserStack credentials in resources/browserstack-config.json.

Run the tests:

mvn clean test
