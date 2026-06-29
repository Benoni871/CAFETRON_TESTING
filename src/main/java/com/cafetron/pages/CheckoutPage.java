package com.cafetron.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

import java.util.Locale;

public class CheckoutPage extends BasePage {
    private final By overviewViewButton = By.id("checkout-overview-view-btn");
    private final By cardsViewButton = By.id("checkout-cards-view-btn");
    private final By overviewLocationInput = By.id("checkout-overview-location-input");
    private final By cardLocationInput = By.id("checkout-card-location-input");
    private final By locationInput = By.cssSelector(
            "input[id*='location'], input[name*='location'], input[placeholder*='Pickup location']");
    private final By overviewPlaceOrderButton = By.id("checkout-overview-place-order-btn");
    private final By cardPlaceOrderButton = By.id("checkout-card-place-order-btn");
    private final By finalPlaceOrderButton = By.xpath("//button[contains(normalize-space(.), 'Place Order')]");
    private final By nextButton = By.xpath("//button[contains(normalize-space(.), 'Next') and not(@disabled)]");
    private final By overviewTotal = By.id("checkout-overview-total");
    private final By cardTotal = By.id("checkout-card-total");
    private final By errorAlert = By.id("checkout-error-alert");
    private final By toast = By.id("checkout-toast");
    private final By pickupWindowSelect = By.cssSelector(
            "select[id*='pickup'], select[id*='window'], select[id*='slot'], "
                    + "select[name*='pickup'], select[name*='window'], select[name*='slot']");
    private final By pickupWindowChoice = By.cssSelector(
            "button[id*='pickup'], button[id*='pickup-window'], button[id*='pickup-slot'], button[id*='time-slot'], "
                    + "button[id*='slot'], input[type='radio'][id*='pickup'], "
                    + "input[type='radio'][id*='window'], input[type='radio'][id*='slot'], "
                    + "input[type='radio'][name*='pickup'], input[type='radio'][name*='window'], "
                    + "input[type='radio'][name*='slot'], [role='tab'][id*='pickup'], "
                    + "[role='tab'][id*='window'], [role='tab'][id*='slot']");
    private final By availablePickupWindowButton = By.xpath("//button[not(@disabled) "
            + "and (contains(normalize-space(.), 'AM') or contains(normalize-space(.), 'PM')) "
            + "and not(contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), "
            + "'closed today'))]");

    public CheckoutPage(WebDriver driver) {
        super(driver);
    }

    public void open() {
        navigateTo("/checkout");
    }

    public boolean isDisplayed() {
        return isDisplayed(overviewViewButton) || isDisplayed(cardsViewButton)
                || isDisplayed(overviewPlaceOrderButton) || isDisplayed(cardPlaceOrderButton)
                || isDisplayed(finalPlaceOrderButton);
    }

    public boolean hasTotals() {
        return isDisplayed(overviewTotal) || isDisplayed(cardTotal);
    }

    public void enterPickupLocation(String location) {
        if (isDisplayed(overviewLocationInput)) {
            type(overviewLocationInput, location);
        } else if (isDisplayed(cardLocationInput)) {
            type(cardLocationInput, location);
        } else {
            type(locationInput, location);
        }
    }

    public boolean isPlaceOrderAvailable() {
        return isDisplayed(overviewPlaceOrderButton)
                || isDisplayed(cardPlaceOrderButton)
                || isDisplayed(finalPlaceOrderButton);
    }

    public boolean selectFirstPickupWindow() {
        for (WebElement selectElement : findAll(pickupWindowSelect)) {
            if (selectElement.isDisplayed() && selectElement.isEnabled()) {
                Select select = new Select(selectElement);
                for (int index = 0; index < select.getOptions().size(); index++) {
                    WebElement option = select.getOptions().get(index);
                    if (option.isEnabled() && !option.getText().isBlank()
                            && (index > 0 || select.getOptions().size() == 1)) {
                        select.selectByIndex(index);
                        return true;
                    }
                }
            }
        }

        if (clickFirstDisplayedEnabled(availablePickupWindowButton)) {
            return true;
        }

        if (clickFirstDisplayedEnabled(pickupWindowChoice)) {
            return true;
        }

        return false;
    }

    public boolean advanceToPlaceOrderStep() {
        for (int step = 0; step < 4; step++) {
            if (isDisplayedNow(overviewPlaceOrderButton)
                    || isDisplayedNow(cardPlaceOrderButton)
                    || isDisplayedNow(finalPlaceOrderButton)) {
                return true;
            }
            if (!clickFirstDisplayedEnabled(nextButton)) {
                return false;
            }
        }
        return isDisplayedNow(overviewPlaceOrderButton)
                || isDisplayedNow(cardPlaceOrderButton)
                || isDisplayedNow(finalPlaceOrderButton);
    }

    public void placeOrder() {
        if (clickFirstDisplayedEnabled(overviewPlaceOrderButton)) {
            return;
        }
        if (clickFirstDisplayedEnabled(cardPlaceOrderButton)) {
            return;
        }
        clickFirstDisplayedEnabled(finalPlaceOrderButton);
    }

    public boolean hasFeedback() {
        return isDisplayed(errorAlert) || isDisplayed(toast);
    }

    public String feedbackText() {
        return (getOptionalText(errorAlert) + " " + getOptionalText(toast)).trim();
    }

    public boolean hasCutoffBlockingFeedback() {
        String feedback = feedbackText().toLowerCase(Locale.ROOT);
        return feedback.contains("cutoff")
                || feedback.contains("cut off")
                || feedback.contains("ordering window")
                || feedback.contains("order window")
                || feedback.contains("after cutoff")
                || feedback.contains("not allowed")
                || feedback.contains("blocked")
                || feedback.contains("closed");
    }

    private boolean clickFirstDisplayedEnabled(By locator) {
        for (WebElement element : findAll(locator)) {
            if (isDisplayedAndEnabled(element)) {
                clickSafely(element);
                return true;
            }
        }
        return false;
    }

    private boolean isDisplayedNow(By locator) {
        for (WebElement element : findAll(locator)) {
            if (isDisplayedSafely(element)) {
                return true;
            }
        }
        return false;
    }

    private boolean isDisplayedAndEnabled(WebElement element) {
        try {
            return element.isDisplayed() && element.isEnabled();
        } catch (StaleElementReferenceException exception) {
            return false;
        }
    }

    private boolean isDisplayedSafely(WebElement element) {
        try {
            return element.isDisplayed();
        } catch (StaleElementReferenceException exception) {
            return false;
        }
    }

    private void clickSafely(WebElement element) {
        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].scrollIntoView({block: 'center', inline: 'center'});", element);
        try {
            element.click();
        } catch (WebDriverException exception) {
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
        }
    }
}
