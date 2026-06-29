package com.cafetron.flows;

import com.cafetron.data.Role;
import com.cafetron.data.TestDataFactory;
import com.cafetron.data.TestUser;
import com.cafetron.pages.LoginPage;
import org.openqa.selenium.WebDriver;
import org.testng.SkipException;

public class AuthFlow {
    private final WebDriver driver;

    public AuthFlow(WebDriver driver) {
        this.driver = driver;
    }

    public TestUser loginAs(Role role) {
        TestUser user = configuredUser(role);

        LoginPage loginPage = new LoginPage(driver);
        loginPage.open();
        if (loginPage.currentUrlContains(user.role().landingPath())) {
            return user;
        }
        loginPage.login(user.employeeId(), user.password());
        loginPage.waitForUrlContains(user.role().landingPath());
        return user;
    }


    public void logoutEmployeeFromMenuIfPossible() {
        try {
            new com.cafetron.pages.MenuPage(driver).logout();
        } catch (RuntimeException ignored) {
            // Best-effort cleanup for tests that intentionally navigate across pages.
        }
    }

    private TestUser configuredUser(Role role) {
        try {
            return TestDataFactory.configuredUser(role);
        } catch (IllegalArgumentException exception) {
            throw new SkipException(exception.getMessage());
        }
    }
}
