package com.cafetron.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import java.util.Locale;

public class VendorQueuePage extends BasePage {
    public static final String PATH = "/vendor/queue";

    private final By pageShell = By.cssSelector(
            "#vendor-queue-page, [data-testid='vendor-queue-page'], main");
    private final By queueGrid = By.cssSelector(
            "#vendor-queue-grid, [data-testid='vendor-queue-grid'], .vendor-queue-grid, .queue-grid");
    private final By queueList = By.cssSelector(
            "#vendor-queue-list, [data-testid='vendor-queue-list'], .vendor-queue-list, .queue-list");
    private final By emptyState = By.cssSelector(
            "#vendor-queue-empty-state, [data-testid='vendor-queue-empty-state'], .empty-state");
    private final By errorNotice = By.cssSelector(
            "#vendor-queue-error-notice, [data-testid='vendor-queue-error-notice'], .alert-error");

    public VendorQueuePage(WebDriver driver) {
        super(driver);
    }

    public void open() {
        navigateTo(PATH);
    }

    public boolean isDisplayed() {
        return currentUrlContains(PATH) && isDisplayed(pageShell);
    }

    public boolean hasQueueState() {
        return isDisplayed(queueGrid)
                || isDisplayed(queueList)
                || isDisplayed(emptyState)
                || isDisplayed(errorNotice)
                || hasQueueCopy();
    }

    public boolean hasImplementedQueueState() {
        return isDisplayed() && hasQueueState() && !isPlaceholderOnly();
    }

    public boolean isPlaceholderOnly() {
        String text = visibleTextSnapshot().toLowerCase(Locale.ROOT);
        return text.contains("experience in progress")
                || text.contains("route is wired into the app shell")
                || text.contains("once the module implementation lands")
                || text.contains("module implementation lands")
                || text.contains("placeholder")
                || text.contains("coming soon")
                || text.contains("under construction");
    }

    public String visibleTextSnapshot() {
        String text = getOptionalText(By.tagName("body")).replaceAll("\\s+", " ").trim();
        return text.length() > 300 ? text.substring(0, 300) + "..." : text;
    }

    private boolean hasQueueCopy() {
        String text = visibleTextSnapshot().toLowerCase(Locale.ROOT);
        return text.contains("vendor queue")
                || text.contains("pickup queue")
                || text.contains("pending orders")
                || text.contains("queued orders")
                || text.contains("no orders")
                || text.contains("accept")
                || text.contains("decline");
    }
}
