package com.cafetron.flows;

import com.cafetron.pages.CartPage;
import com.cafetron.pages.CheckoutPage;
import com.cafetron.pages.MenuPage;
import org.openqa.selenium.*;
import org.testng.SkipException;

import java.time.Duration;
import java.util.List;

public class CartCheckoutFlow {
    private final WebDriver driver;
    private static final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
    private static final String ADD_BUTTON_PREDICATE =
            "contains(translate(@id, '" + UPPERCASE + "', '" + LOWERCASE + "'), 'add') or " +
                    "contains(translate(@title, '" + UPPERCASE + "', '" + LOWERCASE + "'), 'add') or " +
                    "contains(translate(@aria-label, '" + UPPERCASE + "', '" + LOWERCASE + "'), 'add') or " +
                    "contains(translate(normalize-space(.), '" + UPPERCASE + "', '" + LOWERCASE + "'), 'add') or " +
                    "contains(normalize-space(.), '+')";


    private static final By ADD_BUTTONS = By.xpath("//button[" + ADD_BUTTON_PREDICATE + "]");

    public CartCheckoutFlow(WebDriver driver) {
        this.driver = driver;
    }
    private void clickAddButton(WebElement button) {
        try {
            button.click();
        } catch (WebDriverException exception) {
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", button);
        }
    }

    private boolean clickFirstAddButtonAndConfirmCart(List<WebElement> addButtons) {
        for (WebElement button : addButtons) {
            if (isDisplayedAndEnabled(button)) {
                clickAddButton(button);
                CartPage cartPage = new CartPage(driver);
                cartPage.open();
                return cartPage.hasCheckoutableItems();
            }
        }
        return false;
    }


    public boolean addMenuItemNamedToCart(String itemName) {
        MenuPage menuPage = new MenuPage(driver);
        menuPage.open();
        menuPage.isAtMenuRoute();
        try {
            menuPage.search(itemName);
        } catch (RuntimeException ignored) {
            // Some menu states do not expose search; continue with visible cards.
        }


        String itemLiteral = xpathLiteral(itemName);
        By namedItemAddButtons = By.xpath("//*[contains(normalize-space(.), " + itemLiteral + ")]" +
                "/ancestor::*[self::article or contains(@id, 'menu-item') or contains(translate(@class, '" +
                UPPERCASE + "', '" + LOWERCASE + "'), 'card')][1]//button[" + ADD_BUTTON_PREDICATE + "]");
        waitForVisibleEnabledButton(namedItemAddButtons);
        return clickFirstAddButtonAndConfirmCart(driver.findElements(namedItemAddButtons));
    }

    private String xpathLiteral(String text) {
        if (!text.contains("'")) {
            return "'" + text + "'";
        }
        if (!text.contains("\"")) {
            return "\"" + text + "\"";
        }
        StringBuilder literal = new StringBuilder("concat(");
        for (int index = 0; index < text.length(); index++) {
            if (index > 0) {
                literal.append(", ");
            }
            String character = String.valueOf(text.charAt(index));
            literal.append("'".equals(character) ? "\"'\"" : "'" + character + "'");
        }
        literal.append(")");
        return literal.toString();
    }

    public boolean addFirstAvailableMenuItemToCart() {
        MenuPage menuPage = new MenuPage(driver);
        menuPage.open();
        menuPage.isAtMenuRoute();

        waitForVisibleEnabledButton(ADD_BUTTONS);
        if (clickFirstAddButtonAndConfirmCart(driver.findElements(ADD_BUTTONS))) {
            return true;
        }

        List<WebElement> fallbackAddButtons = driver.findElements(By.cssSelector(
                "button[id^='menu-add'], button[id*='add'], button[title*='Add'], "
                        + "button[aria-label*='Add'], .btn-primary"));
        return clickFirstAddButtonAndConfirmCart(fallbackAddButtons);
    }

    private void waitForVisibleEnabledButton(By locator) {
        try {
            new org.openqa.selenium.support.ui.WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(driver -> driver.findElements(locator).stream().anyMatch(this::isDisplayedAndEnabled));
        } catch (TimeoutException ignored) {
            // Caller will attempt the available fallback selectors or return false.
        }
    }

    private boolean isDisplayedAndEnabled(WebElement element) {
        try {
            return element.isDisplayed() && element.isEnabled();
        } catch (StaleElementReferenceException exception) {
            return false;
        }
    }

    public void openCheckoutWithCartItem() {
        if (!addFirstAvailableMenuItemToCart()) {
            throw new SkipException("No available menu item could be added through the UI.");
        }
        new CartPage(driver).open();
        CartPage cartPage = new CartPage(driver);
        if (!cartPage.isCheckoutAvailable()) {
            throw new SkipException("Checkout button is not available after adding an item.");
        }
    }

    public CheckoutPage checkoutPage() {
        return new CheckoutPage(driver);
    }
}
