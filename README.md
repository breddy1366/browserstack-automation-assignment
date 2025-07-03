# BrowserStack Automation Assignment

## 👨‍💻 Tech Stack
- Java
- Selenium WebDriver
- BrowserStack (Parallel Testing)
- JSoup
- Google Translate API
- OkHttp (API requests)
- TestNG (or JUnit)

## 📋 Problem Statement
1. Scrape 5 articles from "Opinion" section of El País.
2. Extract and print titles & content.
3. Download cover images.
4. Translate article headers to English using Google Translate API.
5. Analyze common repeated words (3+ occurrences).
6. Run 5 parallel tests on BrowserStack.

## 🏗 Project Structure

src/
├── main/java/utils/Translator.java
└── test/java/tests/{BrowserStackTest, ScraperTest}.java


## 🧪 Running Tests
```bash
mvn clean test

