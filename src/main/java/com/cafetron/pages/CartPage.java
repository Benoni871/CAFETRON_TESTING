package com.cafetron.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CartPage extends BasePage {
    private static final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
    private static final Pattern FIRST_NUMBER = Pattern.compile("\\b(\\d+)\\b");

    private final By page = By.id("cart-page");
    private final By emptyState = By.id("cart-empty-state");
    private final By itemsList = By.id("cart-items-list");
    private final By summaryCard = By.id("cart-summary-card");
    private final By checkoutButton = By.id("cart-checkout-btn");
    private final By clearButton = By.id("cart-clear-btn");
    private final By backToMenuButton = By.id("cart-back-to-menu-btn");
    private final By quantityInputs = By.cssSelector(
            "input[type='number'], input[id*='qty'], input[id*='quantity'], "
                    + "input[name*='qty'], input[name*='quantity']");
    private final By quantityIndicators = By.cssSelector(
            "[id*='qty'], [id*='quantity'], [class*='qty'], [class*='quantity']");
    private final By incrementButtons = By.xpath("//button[normalize-space(.) = '+' "
            + "or contains(translate(@id, '" + UPPERCASE + "', '" + LOWERCASE + "'), 'increase') "
            + "or contains(translate(@id, '" + UPPERCASE + "', '" + LOWERCASE + "'), 'increment') "
            + "or contains(translate(@id, '" + UPPERCASE + "', '" + LOWERCASE + "'), 'plus') "
            + "or contains(translate(@aria-label, '" + UPPERCASE + "', '" + LOWERCASE + "'), 'increase') "
            + "or contains(translate(@aria-label, '" + UPPERCASE + "', '" + LOWERCASE + "'), 'increment') "
            + "or contains(translate(@aria-label, '" + UPPERCASE + "', '" + LOWERCASE + "'), 'plus') "
            + "or contains(translate(@title, '" + UPPERCASE + "', '" + LOWERCASE + "'), 'increase') "
            + "or contains(translate(@title, '" + UPPERCASE + "', '" + LOWERCASE + "'), 'increment') "
            + "or contains(translate(@title, '" + UPPERCASE + "', '" + LOWERCASE + "'), 'plus')]");
    private final By feedbackMessages = By.cssSelector(
            "[role='alert'], [id*='error'], [class*='error'], [id*='toast'], [class*='toast'], "
                    + "[id*='warning'], [class*='warning'], [id*='validation'], [class*='validation'], "
                    + "[id*='stock'], [class*='stock']");

    public CartPage(WebDriver driver) {
        super(driver);
    }

    public void open() {
        navigateTo("/cart");
    }

    public boolean isDisplayed() {
        return isDisplayed(page);
    }

    public boolean hasCartState() {
        return isDisplayed(emptyState) || isDisplayed(itemsList) || isDisplayed(summaryCard);
    }

    public boolean isCheckoutAvailable() {
        return isDisplayed(checkoutButton);
    }

    public void clickCheckout() {
        click(checkoutButton);
    }

    public boolean isClearAvailable() {
        return isDisplayed(clearButton);
    }

    public void clearIfAvailable() {
        if (isDisplayedNow(clearButton)) {
            click(clearButton);
        }
    }

    public boolean hasCheckoutableItems() {
        return isDisplayed(itemsList) || isDisplayed(checkoutButton);
    }

    public boolean isBackToMenuAvailable() {
        return isDisplayed(backToMenuButton);
    }

    public int increaseFirstItemQuantityTo(int targetQuantity) {
        waitForVisible(page);
        if (setFirstQuantityInput(targetQuantity)) {
            return highestVisibleQuantity();
        }

        int previousQuantity = highestVisibleQuantity();
        int maxAttempts = Math.max(targetQuantity + 2, 3);
        for (int attempts = 0; attempts < maxAttempts && previousQuantity < targetQuantity; attempts++) {
            WebElement incrementButton = firstDisplayedEnabled(incrementButtons);
            if (incrementButton == null) {
                break;
            }
            incrementButton.click();

            int currentQuantity = highestVisibleQuantity();
            if (hasStockLimitFeedback() || currentQuantity <= previousQuantity) {
                break;
            }
            previousQuantity = currentQuantity;
        }
        return highestVisibleQuantity();
    }

    public boolean preventsQuantityAboveStockOrShowsValidation(int requestedQuantity) {
        try {
            wait.until(driver -> hasStockLimitFeedback()
                    || (highestVisibleQuantity() > 0 && highestVisibleQuantity() < requestedQuantity));
            return true;
        } catch (TimeoutException exception) {
            return false;
        }
    }

    public boolean attemptToSetItemQuantity(String itemName, int quantity) {
        waitForVisible(page);
        for (WebElement container : cartItemContainers(itemName)) {
            if (setQuantityByInput(container, quantity)) {
                return true;
            }
            if (clickIncreaseTowardQuantity(container, quantity)) {
                return true;
            }
        }
        return false;
    }

    public boolean waitForStockLimitEnforcement(String itemName, int requestedQuantity) {
        try {
            wait.until(driver -> {
                int currentQuantity = itemQuantity(itemName);
                return hasStockLimitFeedback() || (currentQuantity >= 0 && currentQuantity < requestedQuantity);
            });
            return true;
        } catch (TimeoutException exception) {
            return false;
        }
    }

    public int itemQuantity(String itemName) {
        for (WebElement container : cartItemContainers(itemName)) {
            int quantity = itemQuantityFrom(container);
            if (quantity >= 0) {
                return quantity;
            }
        }
        return -1;
    }

    public boolean hasStockLimitFeedback() {
        String feedbackText = textNow(feedbackMessages).toLowerCase(Locale.ROOT);
        return feedbackText.contains("out of stock")
                || feedbackText.contains("insufficient")
                || feedbackText.contains("not enough")
                || feedbackText.contains("exceed")
                || feedbackText.contains("only")
                || feedbackText.contains("maximum")
                || feedbackText.contains("max ")
                || feedbackText.contains("limit")
                || feedbackText.contains("cannot")
                || feedbackText.contains("unavailable")
                || (feedbackText.contains("stock")
                && (feedbackText.contains("available") || feedbackText.contains("lower")));
    }

    public String visibleTextSnapshot() {
        return getOptionalText(page);
    }

    private boolean setFirstQuantityInput(int quantity) {
        for (WebElement input : findAll(quantityInputs)) {
            if (isDisplayedSafely(input) && isEnabledSafely(input)) {
                input.clear();
                input.sendKeys(String.valueOf(quantity));
                input.sendKeys(Keys.TAB);
                return true;
            }
        }
        return false;
    }

    private boolean setQuantityByInput(WebElement container, int quantity) {
        for (WebElement input : container.findElements(quantityInputs)) {
            if (isDisplayedSafely(input) && isEnabledSafely(input)) {
                input.clear();
                input.sendKeys(String.valueOf(quantity));
                input.sendKeys(Keys.TAB);
                return true;
            }
        }
        return false;
    }

    private boolean clickIncreaseTowardQuantity(WebElement container, int targetQuantity) {
        int currentQuantity = itemQuantityFrom(container);
        int attempts = currentQuantity >= 0
                ? Math.max(targetQuantity - currentQuantity, 1)
                : Math.max(targetQuantity - 1, 1);
        boolean clicked = false;

        for (int attempt = 0; attempt < attempts; attempt++) {
            WebElement increaseButton = firstIncreaseButton(container);
            if (increaseButton == null) {
                return clicked;
            }
            increaseButton.click();
            clicked = true;

            if (hasStockLimitFeedback()) {
                return true;
            }
            int updatedQuantity = itemQuantityFrom(container);
            if (updatedQuantity >= targetQuantity) {
                return true;
            }
        }
        return clicked;
    }

    private WebElement firstIncreaseButton(WebElement container) {
        for (WebElement button : container.findElements(toRelativeXpath(incrementButtons))) {
            if (isDisplayedSafely(button) && isEnabledSafely(button)) {
                return button;
            }
        }
        return null;
    }

    private int highestVisibleQuantity() {
        int highestQuantity = 0;
        for (WebElement input : findAll(quantityInputs)) {
            if (isDisplayedSafely(input)) {
                highestQuantity = Math.max(highestQuantity, parseFirstNumber(input.getDomProperty("value")));
            }
        }
        for (WebElement element : findAll(quantityIndicators)) {
            if (isDisplayedSafely(element)) {
                highestQuantity = Math.max(highestQuantity, parseFirstNumber(textSafely(element)));
            }
        }
        return highestQuantity;
    }

    private int itemQuantityFrom(WebElement container) {
        for (WebElement input : container.findElements(quantityInputs)) {
            if (isDisplayedSafely(input)) {
                int value = parseFirstNumber(input.getDomProperty("value"));
                if (value >= 0) {
                    return value;
                }
            }
        }

        for (WebElement quantityIndicator : container.findElements(quantityIndicators)) {
            if (isDisplayedSafely(quantityIndicator)) {
                int value = parseFirstNumber(textSafely(quantityIndicator));
                if (value >= 0) {
                    return value;
                }
            }
        }
        return -1;
    }

    private List<WebElement> cartItemContainers(String itemName) {
        List<WebElement> namedContainers = displayedElements(cartItemContainersFor(itemName));
        if (!namedContainers.isEmpty()) {
            return namedContainers;
        }

        List<WebElement> containers = new ArrayList<>();
        for (WebElement itemList : findAll(itemsList)) {
            if (isDisplayedSafely(itemList)
                    && normalize(textSafely(itemList)).contains(normalize(itemName))) {
                containers.add(itemList);
            }
        }
        return containers;
    }

    private List<WebElement> displayedElements(By locator) {
        List<WebElement> displayedElements = new ArrayList<>();
        for (WebElement element : findAll(locator)) {
            if (isDisplayedSafely(element)) {
                displayedElements.add(element);
            }
        }
        return displayedElements;
    }

    private WebElement firstDisplayedEnabled(By locator) {
        for (WebElement element : findAll(locator)) {
            if (isDisplayedSafely(element) && isEnabledSafely(element)) {
                return element;
            }
        }
        return null;
    }

    private By cartItemContainersFor(String itemName) {
        String normalizedItemName = xpathLiteral(normalize(itemName));
        return By.xpath("//*[contains(translate(normalize-space(.), '" + UPPERCASE + "', '" + LOWERCASE + "'), "
                + normalizedItemName + ")]"
                + "/ancestor-or-self::*[starts-with(@id, 'cart-item') or self::article or self::li "
                + "or contains(concat(' ', normalize-space(@class), ' '), ' cart-item ')][1]");
    }

    private By toRelativeXpath(By locator) {
        String value = locator.toString();
        String prefix = "By.xpath: //";
        if (value.startsWith(prefix)) {
            return By.xpath(".//" + value.substring(prefix.length()));
        }
        return locator;
    }

    private boolean isDisplayedNow(By locator) {
        return findAll(locator).stream().anyMatch(this::isDisplayedSafely);
    }

    private String textNow(By locator) {
        StringBuilder text = new StringBuilder();
        for (WebElement element : findAll(locator)) {
            if (isDisplayedSafely(element)) {
                String elementText = textSafely(element).trim();
                if (!elementText.isBlank()) {
                    if (!text.isEmpty()) {
                        text.append(' ');
                    }
                    text.append(elementText);
                }
            }
        }
        return text.toString();
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

    private static int parseFirstNumber(String value) {
        Matcher matcher = FIRST_NUMBER.matcher(value == null ? "" : value);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : -1;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private static String xpathLiteral(String value) {
        if (!value.contains("'")) {
            return "'" + value + "'";
        }
        if (!value.contains("\"")) {
            return "\"" + value + "\"";
        }

        String[] parts = value.split("'");
        StringBuilder literal = new StringBuilder("concat(");
        for (int index = 0; index < parts.length; index++) {
            if (index > 0) {
                literal.append(", \"'\", ");
            }
            literal.append("'").append(parts[index]).append("'");
        }
        literal.append(")");
        return literal.toString();
    }
}
