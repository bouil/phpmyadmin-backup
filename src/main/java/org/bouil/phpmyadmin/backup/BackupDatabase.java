package org.bouil.phpmyadmin.backup;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;

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

    @Scheduled(fixedDelay = 1 * DAY) // every day
    public void executeBackup() throws MessagingException {

        log.info("Starting backup");

        WebDriver driver = new HtmlUnitDriver();

        log.info("Get the login page " + phpMyAdminUrl);
        driver.get(phpMyAdminUrl);

        driver.findElement(By.id("user")).clear();
        driver.findElement(By.id("user")).sendKeys(login);
        driver.findElement(By.id("password")).clear();
        driver.findElement(By.id("password")).sendKeys(password);
        driver.findElement(By.cssSelector("input[type=submit]")).click();

        log.info("choose database " + database);
        driver.switchTo().frame("frame_navigation");
        driver.findElement(By.id("databaseList")).findElement(By.linkText(database)).click();

        log.info("click on Export");
        driver.switchTo().frame("frame_content");
        driver.findElement(By.id("topmenu")).findElement(By.partialLinkText("Export")).click();

        log.info("validate default settings");
        driver.findElement(By.id("buttonGo")).click();

        log.info("download file");
        String pageSource = driver.getPageSource();

        log.info("Sending email to " + to);
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);

        helper.setFrom(from);
        helper.setTo(to);

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd hh:mm");
        String date = simpleDateFormat.format(new Date());

        helper.setSubject(String.format("PhpMyAdmin [%s] backup at %s", database, date));
        helper.setText(String.format("Enclosed is the backup of the database %s taken from PhpMyAdmin %s", database,
                                     phpMyAdminUrl));
        helper.addAttachment(database + ".sql", new ByteArrayResource(pageSource.getBytes()), "text/plain");
        mailSender.send(mimeMessage);

        log.info("Done");
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
}
