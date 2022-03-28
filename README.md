# Web Scraping

## Getting Started

### Prerequisites

Kindly ensure you have the following installed on your machine:

- [ ] [Java 8](https://www.java.com/en/download/help/download_options.xml)
- [ ] [Maven](https://maven.apache.org/install.html)
- [ ] An IDE or Editor of your choice

### Running the Application

1. Go inside project folder
```
$ cd java-web-scraper
```

2. Create mongo database
```
Create database called 'indeed'
```

3. Create mongo collection
```
Create collection called 'jobs'
```

4. Configure mongo db details
```
In Main.java file, set mongo db host name and port
```

5. Install the dependencies and package the application
```
$ mvn package
```

6. Run the web scraper
```
java -jar target/java-web-scrapper-1.0-SNAPSHOT.jar or Execute Main.java file in your IDE
```

7. User inputs
```
What : Job title, key words, or company
```

```
Where : city, district or territory
```

```
Pages : Number of pages you want to scrap. This should be an integer value
```

