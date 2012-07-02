package org.bouil.phpmyadmin.backup;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.commons.lang.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;

import com.gargoylesoftware.htmlunit.DefaultCredentialsProvider;
import com.gargoylesoftware.htmlunit.WebClient;

public class BackupDatabase {

    private static Logger log = LoggerFactory.getLogger(BackupDatabase.class);

    private static final int SECOND = 1000;
    private static final int MINUTE = 60 * SECOND;
    private static final int HOUR = 60 * MINUTE;
    private static final int DAY = 24 * HOUR;

    private JavaMailSender mailSender;

    private String phpMyAdminUrl;
    private String login;
    private String password;
    private String database;
    private String from;
    private String to;
    private String httpUser;
    private String httpPassword;

    @Scheduled(fixedDelay = 1 * DAY) // every day
    public void executeBackup() throws MessagingException, InterruptedException {
        log.info("Starting backup");

        WebDriver driver = createWebDriver();

        try {
            login(driver);

            chooseDatabase(driver);

            chooseExportPage(driver);

            validateExport(driver);

        } catch (RuntimeException e) {
            String pageSource = driver.getPageSource();
            System.out.println(pageSource);
            throw e;
        }
        log.info("download file");
        String pageSource = driver.getPageSource();

        if (StringUtils.isNotEmpty(to)) {
            sendBackupEmail(pageSource);
        } else {
            log.info("Email receipient not set for backup:");
            log.info(pageSource);
        }
        log.info("Done");

    }

    private WebDriver createWebDriver() {
        return new HtmlUnitDriver() {
            @Override
            protected WebClient modifyWebClient(WebClient client) {
                // This class ships with HtmlUnit itself
                DefaultCredentialsProvider creds = new DefaultCredentialsProvider();
                // Set credentials
                if (StringUtils.isNotEmpty(httpUser) || StringUtils.isNotEmpty(httpPassword)) {
                    creds.addCredentials(httpUser, httpPassword);
                }
                // And now add the provider to the webClient instance
                client.setCredentialsProvider(creds);
                return client;
            }
        };
    }

    private void login(WebDriver driver) {
        log.info("Get the login page " + phpMyAdminUrl);
        driver.get(phpMyAdminUrl);

        WebElement loginElement = driver.findElement(ByIdFallback("user", "input_username"));
        loginElement.clear();
        loginElement.sendKeys(login);

        WebElement passwordElement = driver.findElement(ByIdFallback("password", "input_password"));
        passwordElement.clear();
        passwordElement.sendKeys(password);

        passwordElement.sendKeys(Keys.RETURN);
    }

    private void chooseDatabase(WebDriver driver) {
        if (StringUtils.isNotEmpty(database)) {
            log.info("choose database " + database);
            driver.switchTo().frame("frame_navigation");
            driver.findElement(By.id("databaseList")).findElement(By.linkText(database)).click();
        }
    }

    private void chooseExportPage(WebDriver driver) {
        log.info("click on Export");
        driver.switchTo().frame("frame_content");
        driver.findElement(By.id("topmenu")).findElement(By.partialLinkText("Export")).click();
    }

    private void validateExport(WebDriver driver) {
        log.info("validate default settings");
        driver.findElement(By.id("buttonGo")).click();
    }

    private void sendBackupEmail(String pageSource) {
        if (StringUtils.isNotEmpty(to)) {
            log.info("Sending email to " + to);
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = null;
            try {
                helper = new MimeMessageHelper(mimeMessage, true);

                helper.setFrom(from);
                helper.setTo(to);

                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd hh:mm");
                String date = simpleDateFormat.format(new Date());

                helper.setSubject(String.format("PhpMyAdmin [%s] backup at %s", database, date));
                helper.setText(
                        String.format("Enclosed is the backup of the database %s taken from PhpMyAdmin %s", database,
                                      phpMyAdminUrl));
                helper.addAttachment(database + ".sql", new ByteArrayResource(pageSource.getBytes()), "text/plain");
                mailSender.send(mimeMessage);
            } catch (MessagingException e) {
                log.error(e.getMessage(), e);
            }
        } else {
            log.info("Email receipient not set for backup:");
            log.info(pageSource);
        }

    }

    private By ByIdFallback(String... ids) {
        By element = null;
        NoSuchElementException ex = null;
        for (String id : ids) {
            try {
                element = By.id(id);
            } catch (NoSuchElementException e) {
                ex = e;
            }
        }
        if (ex != null) {
            throw ex;
        } else {
            if (element != null) {
                return element;
            } else {
                throw new NoSuchElementException(ids + " not found");
            }
        }
    }

    public JavaMailSender getMailSender() {
        return mailSender;
    }

    @Required
    public void setMailSender(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public String getPhpMyAdminUrl() {
        return phpMyAdminUrl;
    }

    @Required
    public void setPhpMyAdminUrl(String phpMyAdminUrl) {
        this.phpMyAdminUrl = phpMyAdminUrl;
    }

    public String getLogin() {
        return login;
    }

    @Required
    public void setLogin(String login) {
        this.login = login;
    }

    public String getPassword() {
        return password;
    }

    @Required
    public void setPassword(String password) {
        this.password = password;
    }

    public String getDatabase() {
        return database;
    }

    @Required
    public void setDatabase(String database) {
        this.database = database;
    }

    public String getFrom() {
        return from;
    }

    @Required
    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    @Required
    public void setTo(String to) {
        this.to = to;
    }

    public void setHttpUser(String httpUser) {
        this.httpUser = httpUser;
    }

    public String getHttpUser() {
        return httpUser;
    }

    public void setHttpPassword(String httpPassword) {
        this.httpPassword = httpPassword;
    }

    public String getHttpPassword() {
        return httpPassword;
    }
}
