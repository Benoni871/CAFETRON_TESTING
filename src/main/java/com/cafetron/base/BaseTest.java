package com.cafetron.base;

import com.cafetron.config.ConfigReader;
import com.cafetron.listeners.TestListener;
import com.cafetron.utilities.ScreenshotUtils;
import org.openqa.selenium.WebDriver;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;

import java.lang.reflect.Method;

@Listeners(TestListener.class)
public abstract class BaseTest {

    @BeforeMethod(alwaysRun = true)
    public void setUp(Method method) {
        WebDriver driver = DriverFactory.createDriver();
        driver.get(ConfigReader.get("baseUrl"));
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown(ITestResult result) {
        WebDriver driver = DriverFactory.getDriver();
        if (result.getStatus() == ITestResult.FAILURE && driver != null
                && result.getAttribute(ScreenshotUtils.SCREENSHOT_PATH_ATTRIBUTE) == null) {
            try {
                String screenshotPath = ScreenshotUtils.captureScreenshot(driver, result.getMethod().getMethodName());
                result.setAttribute(ScreenshotUtils.SCREENSHOT_PATH_ATTRIBUTE, screenshotPath);
            } catch (RuntimeException exception) {
                System.err.println("Screenshot capture failed: " + exception.getMessage());
            }
        }
        DriverFactory.quitDriver();
    }

    public WebDriver getDriver() {
        return DriverFactory.getDriver();
    }
}
