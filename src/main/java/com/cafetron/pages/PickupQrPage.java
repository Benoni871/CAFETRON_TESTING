package com.cafetron.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class PickupQrPage extends BasePage {
    private final By qrContent = By.xpath("//*[contains(translate(@id, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'qr') " +
            "or self::canvas or self::svg]");
    private final By backNavigation = By.xpath("//*[self::a or self::button][" +
            "contains(translate(@id, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'back') or " +
            "contains(translate(@title, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'back') or " +
            "contains(translate(@aria-label, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'back') or " +
            "contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'back') or " +
            "contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'orders') or " +
            "contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'history')]");

    public PickupQrPage(WebDriver driver) {
        super(driver);
    }

    public void open(String orderId) {
        navigateTo("/pickup/qr/" + orderId);
    }

    public boolean isDisplayed() {
        return currentUrlContains("/pickup/qr/") && firstDisplayed(qrContent) != null;
    }

    public boolean hasBackNavigation() {
        return firstDisplayed(backNavigation) != null;
    }

    private WebElement firstDisplayed(By locator) {
        for (WebElement element : findAll(locator)) {
            try {
                if (element.isDisplayed()) {
                    return element;
                }
            } catch (RuntimeException ignored) {
                // Element was redrawn while checking QR page controls.
            }
        }
        return null;
    }
}
