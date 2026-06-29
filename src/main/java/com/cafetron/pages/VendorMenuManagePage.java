package com.cafetron.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class VendorMenuManagePage extends BasePage {
    private final By page = By.id("menu-manage-page");
    private final By addItemButton = By.id("menu-manage-add-item-btn");
    private final By itemsGrid = By.id("menu-manage-items-grid");
    private final By emptyState = By.id("menu-manage-empty-state");
    private final By loadingState = By.xpath("//p[contains(normalize-space(.), 'Loading menu items')]");
    private final By form = By.id("menu-item-form");
    private final By nameInput = By.id("menu-item-name-input");
    private final By priceInput = By.id("menu-item-price-input");
    private final By stockInput = By.id("menu-item-stock-input");
    private final By foodTypeInput = By.id("menu-item-food-type-input");
    private final By submitButton = By.id("menu-item-form-submit-btn");
    private final By toast = By.id("menu-manage-toast");
    private final By itemNames = By.cssSelector("[id^='menu-manage-item-name-']");
    private final By validationMessages = By.cssSelector("#menu-item-form [id$='-error']");
    private final By deleteButton = By.cssSelector("btn-danger");
    private final By formErrorMsg = By.id("menu-item-name-error");
    private final By menuEditButton = By.id("menu-manage-item-edit-btn-1");
    private final By menuEditConfButton = By.id("menu-item-form-submit-btn");

    public VendorMenuManagePage(WebDriver driver) {
        super(driver);
    }

    public void open() {
        navigateTo("/menu/manage");
    }

    public boolean isDisplayed() {
        return isDisplayed(page);
    }

    public void clickMenuEdit(){
        click(menuEditButton);
    }

    public void menuEditConfButton(){
        click(menuEditConfButton);
    }

    public boolean hasMenuManagementState() {
        return waitForMenuManagementState();
    }

    public boolean isDeleteDisplayed(){
        return isDisplayed(deleteButton);
    }

    public boolean isFormErrorDisplayed(){
        return isDisplayed(formErrorMsg);
    }

    public void openCreateItemForm() {
        waitForCreateItemFormToBeAvailable();
        click(addItemButton);
        waitForVisible(form);
    }

    public boolean isItemFormDisplayed() {
        return isDisplayed(form);
    }

    public void createItem(String name, String price, String stock, String foodType) {
        openCreateItemForm();
        fillItemForm(name, price, stock, foodType);
        submitItemForm();
    }

    public void fillItemForm(String name, String price, String stock, String foodType) {
        type(nameInput, name);
        type(priceInput, price);
        type(stockInput, stock);
        type(foodTypeInput, foodType);
    }

    public void submitItemForm() {
        click(submitButton);
    }

    public boolean submitItemFormIfEnabled() {
        WebElement submit = waitForVisible(submitButton);
        if (!submit.isEnabled()) {
            return false;
        }
        submit.click();
        return true;
    }

    public boolean waitForMenuManagementState() {
        try {
            wait.until(driver -> !isDisplayedNow(loadingState)
                    && (isDisplayedNow(itemsGrid) || isDisplayedNow(emptyState)));
            return true;
        } catch (TimeoutException exception) {
            return false;
        }
    }

    public boolean waitForItemCountAtLeast(String itemName, int expectedCount) {
        try {
            wait.until(driver -> !isDisplayedNow(loadingState)
                    && countItemsNamed(itemName) >= expectedCount);
            return true;
        } catch (TimeoutException exception) {
            return false;
        }
    }

    public boolean waitForDuplicateSaveAttemptToSettle(String itemName, int originalCount, boolean submitClicked) {
        try {
            wait.until(driver -> {
                int currentCount = countItemsNamed(itemName);
                if (currentCount > originalCount) {
                    return true;
                }
                if (!isDisplayedNow(form) && !isDisplayedNow(loadingState)
                        && (isDisplayedNow(itemsGrid) || isDisplayedNow(emptyState))) {
                    return true;
                }
                if (!isDisplayedNow(form)) {
                    return false;
                }
                return hasDuplicateFeedbackNow()
                        || (!submitClicked && !isSubmitButtonEnabledNow());
            });
            return true;
        } catch (TimeoutException exception) {
            return false;
        }
    }

    public int countItemsNamed(String itemName) {
        String expectedName = normalize(itemName);
        int count = 0;
        for (WebElement itemNameElement : findAll(itemNames)) {
            if (isDisplayedSafely(itemNameElement)
                    && normalize(textSafely(itemNameElement)).equals(expectedName)) {
                count++;
            }
        }
        return count;
    }

    public String menuItemNamesSnapshot() {
        List<String> names = findAll(itemNames).stream()
                .filter(this::isDisplayedSafely)
                .map(this::textSafely)
                .map(String::trim)
                .filter(text -> !text.isBlank())
                .collect(Collectors.toList());
        return names.isEmpty() ? visibleTextSnapshot() : String.join(", ", names);
    }

    public String visibleTextSnapshot() {
        return getOptionalText(page);
    }

    public boolean hasSaveFeedback() {
        return isDisplayed(toast);
    }

    public String saveFeedbackText() {
        return getOptionalText(toast);
    }

    private void waitForCreateItemFormToBeAvailable() {
        wait.until(driver -> !isDisplayedNow(loadingState)
                && !isDisplayedNow(form)
                && isDisplayedNow(addItemButton)
                && (isDisplayedNow(itemsGrid) || isDisplayedNow(emptyState)));
    }

    private boolean hasDuplicateFeedbackNow() {
        String feedbackText = (textNow(toast) + " " + textNow(validationMessages)).toLowerCase(Locale.ROOT);
        return feedbackText.contains("duplicate")
                || feedbackText.contains("already")
                || feedbackText.contains("exist");
    }

    private boolean isSubmitButtonEnabledNow() {
        List<WebElement> submitButtons = findAll(submitButton);
        return !submitButtons.isEmpty()
                && isDisplayedSafely(submitButtons.get(0))
                && isEnabledSafely(submitButtons.get(0));
    }

    private boolean isDisplayedNow(By locator) {
        return findAll(locator).stream().anyMatch(this::isDisplayedSafely);
    }

    private String textNow(By locator) {
        return findAll(locator).stream()
                .filter(this::isDisplayedSafely)
                .map(this::textSafely)
                .map(String::trim)
                .filter(text -> !text.isBlank())
                .collect(Collectors.joining(" "));
    }

    private boolean isDisplayedSafely(WebElement element) {
        try {
            return element.isDisplayed();
        } catch (StaleElementReferenceException exception) {
            return false;
        }
    }

    private boolean isEnabledSafely(WebElement element) {
        try {
            return element.isEnabled();
        } catch (StaleElementReferenceException exception) {
            return false;
        }
    }

    private String textSafely(WebElement element) {
        try {
            return element.getText();
        } catch (StaleElementReferenceException exception) {
            return "";
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    public boolean verifyLoginUrl(String currentUrl) {
        if (currentUrl == null) {
            return false;
        }
        return currentUrl.contains("/login");
    }
}
