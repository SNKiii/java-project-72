### Hexlet tests and linter status:
[![Actions Status](https://github.com/SNKiii/java-project-72/actions/workflows/hexlet-check.yml/badge.svg)](https://github.com/SNKiii/java-project-72/actions)
[![SonarQube](https://github.com/SNKiii/java-project-72/actions/workflows/build.yml/badge.svg)](https://github.com/SNKiii/java-project-72/actions/workflows/build.yml)
[![Tests](https://github.com/SNKiii/java-project-72/actions/workflows/check.yml/badge.svg?branch=main)](https://github.com/SNKiii/java-project-72/actions/workflows/check.yml)

🚀 **Live Demo:** [https://java-project-72-q3ex.onrender.com](https://java-project-72-q3ex.onrender.com)

---

## 📖 About

**Page Analyzer** is a web application for SEO analysis of websites. It checks site availability, extracts meta tags (title, h1, description), and stores check history. Built with **Java 21**, **Javalin 6**, and **JTE** templating engine.

---

## 🚀 Features

- ➕ Add URLs for analysis
- ✅ Run website availability checks (HTTP status)
- 📊 Extract SEO data: title, h1, description
- 💾 Store check history in a database (H2 for development, PostgreSQL for production)
- 📋 Display all sites and their latest checks
- 🐳 Docker support for easy deployment

---

## 🛠 Technologies

| Category | Technologies |
|----------|-------------|
| **Language** | Java 21 |
| **Web Framework** | Javalin 6 |
| **Templating** | JTE |
| **Databases** | H2 (dev), PostgreSQL (prod) |
| **Connection Pool** | HikariCP |
| **HTML Parsing** | Jsoup |
| **Build Tool** | Gradle |
| **Containerization** | Docker |
| **CI/CD** | GitHub Actions |
| **Testing** | JUnit 5, MockWebServer |

---

## 🔧 Local Development

### Requirements
- Java 21 or higher
- Gradle 8.7 (wrapper included)

### Run Locally (H2)

```bash
git clone https://github.com/SNKiii/java-project-72.git
cd java-project-72/app
./gradlew run
