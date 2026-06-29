package com.cafetron.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.Locale;
import java.util.stream.Collectors;

public class AdminVendorsPage extends BasePage {
    private final By page = By.id("vendor-manage-page");
    private final By addVendorButton = By.id("vendor-manage-add-vendor-btn");
    private final By vendorsGrid = By.id("vendor-manage-vendors-grid");
    private final By emptyState = By.id("vendor-manage-empty-state");
    private final By form = By.id("vendor-form");
    private final By vendorNameInput = By.id("vendor-name-input");
    private final By vendorEmailInput = By.id("vendor-email-input");
    private final By vendorPhoneInput = By.id("vendor-phone-input");
    private final By vendorContactPersonInput = By.id("vendor-contact-person-input");
    private final By submitButton = By.id("vendor-form-submit-btn");
    private final By toast = By.id("vendor-manage-toast");
    private final By vendorRecordCandidates = By.cssSelector(
            "#vendor-manage-vendors-grid tr, #vendor-manage-vendors-grid li, "
                    + "#vendor-manage-vendors-grid article, #vendor-manage-vendors-grid .card, "
                    + "#vendor-manage-vendors-grid [class*='row']");
    private final By vendorTimestampCandidates = By.cssSelector(
            "time, [datetime], [id*='time'], [id*='date'], [class*='time'], [class*='date'], "
                    + "[data-testid*='time'], [data-testid*='date']");

    public AdminVendorsPage(WebDriver driver) {
        super(driver);
    }

    public void open() {
        navigateTo("/admin/vendors");
    }

    public boolean isDisplayed() {
        return isDisplayed(page);
    }

    public boolean hasVendorState() {
        return isDisplayed(vendorsGrid) || isDisplayed(emptyState);
    }

    public void openCreateVendorForm() {
        click(addVendorButton);
    }

    public boolean isVendorFormDisplayed() {
        return isDisplayed(form);
    }

    public void createVendor(String name, String email, String phone, String contactPerson) {
        openCreateVendorForm();
        type(vendorNameInput, name);
        type(vendorEmailInput, email);
        type(vendorPhoneInput, phone);
        type(vendorContactPersonInput, contactPerson);
        click(submitButton);
    }

    public boolean hasFeedback() {
        return isDisplayed(toast);
    }

    public String vendorFeedbackText() {
        return getOptionalText(toast);
    }

    public boolean waitForVendorRecord(String vendorName) {
        try {
            String expectedVendorName = normalize(vendorName);
            wait.until(driver -> normalize(vendorGridTextNow()).contains(expectedVendorName));
            return true;
        } catch (TimeoutException exception) {
            return false;
        }
    }

    public String vendorRecordTimestampEvidenceText(String vendorName) {
        WebElement vendorRecord = findVendorRecord(vendorName);
        if (vendorRecord != null) {
            String timestampText = timestampTextFrom(vendorRecord);
            String recordText = vendorRecord.getText().trim();
            return (timestampText + " " + recordText).replaceAll("\\s+", " ").trim();
        }

        return vendorGridTextNow();
    }

    private WebElement findVendorRecord(String vendorName) {
        String expectedVendorName = normalize(vendorName);
        return findAll(vendorRecordCandidates).stream()
                .filter(WebElement::isDisplayed)
                .filter(element -> normalize(element.getText()).contains(expectedVendorName))
                .findFirst()
                .orElse(null);
    }

    private String vendorGridTextNow() {
        return findAll(vendorsGrid).stream()
                .filter(WebElement::isDisplayed)
                .map(WebElement::getText)
                .map(String::trim)
                .filter(text -> !text.isBlank())
                .collect(Collectors.joining(" "));
    }

    private String timestampTextFrom(WebElement recordElement) {
        return recordElement.findElements(vendorTimestampCandidates).stream()
                .filter(WebElement::isDisplayed)
                .map(this::timestampTextFromElement)
                .filter(text -> !text.isBlank())
                .collect(Collectors.joining(" "));
    }

    private String timestampTextFromElement(WebElement element) {
        String text = element.getText().trim();
        if (!text.isBlank()) {
            return text;
        }

        String datetime = element.getDomAttribute("datetime");
        if (datetime != null && !datetime.isBlank()) {
            return datetime.trim();
        }

        String title = element.getDomAttribute("title");
        return title == null ? "" : title.trim();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
