package com.cafetron.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class AdminOperationsPage extends BasePage {
    private final By page = By.id("admin-operations-page");
    private final By windowState = By.id("admin-operations-window-state");
    private final By toggleButton = By.id("admin-operations-window-toggle-btn");
    private final By cutoffInput = By.id("admin-operations-cutoff-input");
    private final By cutoffSaveButton = By.id("admin-operations-cutoff-save-btn");
    private final By statusBanner = By.id("admin-operations-status-banner");
    private final By tableOrderingAllowed = By.id("admin-operations-table-ordering-allowed");
    private final By toast = By.id("admin-operations-toast");

    public AdminOperationsPage(WebDriver driver) {
        super(driver);
    }

    public void open() {
        navigateTo("/admin/operations");
    }

    public boolean isDisplayed() {
        return isDisplayed(page);
    }

    public boolean hasOrderingWindowControls() {
        return isDisplayed(windowState) && isDisplayed(toggleButton);
    }

    public boolean hasCutoffControls() {
        return isDisplayed(cutoffInput) && isDisplayed(cutoffSaveButton);
    }

    public boolean hasStatusSummary() {
        return isDisplayed(statusBanner) || isDisplayed(tableOrderingAllowed);
    }

    public void toggleOrderingWindow() {
        click(toggleButton);
    }

    public void setCutoffTime(String cutoffTime) {
        type(cutoffInput, cutoffTime);
        click(cutoffSaveButton);
    }

    public void closeOrderingWindowIfOpen() {
        if (!isOrderingWindowClosed()) {
            toggleOrderingWindow();
            wait.until(driver -> isOrderingWindowClosed() || hasFeedback());
        }
    }

    public boolean isOrderingWindowClosed() {
        String stateText = getOptionalText(windowState).toLowerCase();
        return stateText.contains("closed") || stateText.contains("disabled") || stateText.contains("not allowed");
    }

    public String orderingAllowedText() {
        return getOptionalText(tableOrderingAllowed);
    }

    public boolean isOrderingAllowedNo() {
        return "No".equalsIgnoreCase(orderingAllowedText());
    }

    public boolean hasFeedback() {
        return isDisplayed(toast);
    }

    public void clearCutoffAndSave() {
        type(cutoffInput, "");
        click(cutoffSaveButton);
    }
}
