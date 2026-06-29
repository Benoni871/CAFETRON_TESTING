package com.cafetron.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

public class MenuPage extends BasePage {
    private static final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";

    private final By menuBrowsePage = By.id("menu-browse-page");
    private final By profileLink = By.id("menu-profile-link");
    private final By logoutButton = By.id("menu-logout-btn");
    private final By searchInput = By.id("menu-search-input");
    private final By itemsGrid = By.id("menu-items-grid");
    private final By emptyState = By.id("menu-empty-state");
    private final By cartPreviewButton = By.id("menu-cart-preview-btn");
    private final By floatingCartButton = By.id("menu-floating-cart-btn");
    private final By cartDrawer = By.id("menu-cart-drawer");
    private final By checkoutButton = By.id("menu-cart-checkout-btn");
    private final By manageMenuLink = By.id("menu-manage-link");

    public MenuPage(WebDriver driver) {
        super(driver);
    }

    public void open() {
        navigateTo("/menu");
    }

    public boolean isAtMenuRoute() {
        return waitForUrlContains("/menu");
    }

    public boolean isMenuPageDisplayed() {
        return isDisplayed(menuBrowsePage)
                && isDisplayed(profileLink)
                && isDisplayed(logoutButton);
    }

    public boolean hasMenuResultsArea() {
        return isDisplayed(itemsGrid) || isDisplayed(emptyState);
    }

    public void search(String term) {
        type(searchInput, term);
    }

    public boolean addItemNamedToCart(String itemName) {
        search(itemName);
        if (clickFirstVisibleEnabled(addButtonsForItem(itemName))) {
            return true;
        }
        return clickFirstVisibleEnabled(By.cssSelector(
                "button[id^='menu-add'], button[id*='add'], button[title*='Add'], "
                        + "button[aria-label*='Add'], .btn-primary"));
    }

    public boolean isSearchAvailable() {
        return isDisplayed(searchInput);
    }

    public int visibleMenuCardCount() {
        return count(By.cssSelector("[id^='menu-item-card-'], .menu-card, article"));
    }

    public boolean isCartControlVisible() {
        return isDisplayed(cartPreviewButton) || isDisplayed(floatingCartButton);
    }

    public void openCartPreview() {
        if (isDisplayed(cartPreviewButton)) {
            click(cartPreviewButton);
        } else {
            click(floatingCartButton);
        }
    }

    public boolean isCartDrawerDisplayed() {
        return isDisplayed(cartDrawer);
    }

    public boolean isCheckoutButtonDisplayed() {
        return isDisplayed(checkoutButton);
    }

    public boolean isManageMenuLinkDisplayed() {
        return isDisplayed(manageMenuLink);
    }

    public boolean hasEmployeeOrderingControls() {
        return isCartControlVisible() || isDisplayed(By.cssSelector("button[id*='add'], button[title*='Add']"));
    }

    public void logout() {
        click(logoutButton);
    }

    private boolean clickFirstVisibleEnabled(By locator) {
        try {
            wait.until(driver -> visibleEnabledElements(locator).size() > 0);
        } catch (TimeoutException exception) {
            return false;
        }

        for (WebElement element : visibleEnabledElements(locator)) {
            element.click();
            return true;
        }
        return false;
    }

    private List<WebElement> visibleEnabledElements(By locator) {
        return findAll(locator).stream()
                .filter(element -> element.isDisplayed() && element.isEnabled())
                .toList();
    }

    private By addButtonsForItem(String itemName) {
        String normalizedItemName = xpathLiteral(normalize(itemName));
        return By.xpath("//*[contains(translate(normalize-space(.), '" + UPPERCASE + "', '" + LOWERCASE + "'), "
                + normalizedItemName + ")]"
                + "/ancestor-or-self::*[starts-with(@id, 'menu-item-card-') or self::article "
                + "or contains(concat(' ', normalize-space(@class), ' '), ' menu-card ')][1]"
                + "//button[contains(translate(normalize-space(.), '" + UPPERCASE + "', '" + LOWERCASE + "'), 'add') "
                + "or contains(translate(@id, '" + UPPERCASE + "', '" + LOWERCASE + "'), 'add') "
                + "or contains(translate(@title, '" + UPPERCASE + "', '" + LOWERCASE + "'), 'add') "
                + "or contains(translate(@aria-label, '" + UPPERCASE + "', '" + LOWERCASE + "'), 'add')]");
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ").toLowerCase();
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
