package com.cafetron.listeners;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.MediaEntityBuilder;
import com.aventstack.extentreports.Status;
import com.cafetron.base.BaseTest;
import com.cafetron.reports.ExtentReportManager;
import com.cafetron.utilities.ScreenshotUtils;
import org.openqa.selenium.WebDriver;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

public class TestListener implements ITestListener {
    private static final ThreadLocal<ExtentTest> TEST = new ThreadLocal<>();
    private static final String EXTENT_TEST_ATTRIBUTE = TestListener.class.getName() + ".extentTest";
    private static final String EXTENT_TEST_REPORTED_ATTRIBUTE = TestListener.class.getName() + ".reported";

    @Override
    public void onTestStart(ITestResult result) {
        TEST.set(getOrCreateExtentTest(result));
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        ExtentTest extentTest = getOrCreateExtentTest(result);
        if (markReported(result)) {
            extentTest.log(Status.PASS, "Test passed");
            ExtentReportManager.flushReports();
        }
        TEST.remove();
    }

    @Override
    public void onTestFailure(ITestResult result) {
        ExtentTest extentTest = getOrCreateExtentTest(result);
        if (!markReported(result)) {
            TEST.remove();
            return;
        }

        if (result.getThrowable() != null) {
            extentTest.fail(result.getThrowable());
        } else {
            extentTest.fail("Test failed");
        }

        try {
            String screenshotPath = getExistingScreenshotPath(result);
            WebDriver driver = getDriverFromResult(result);
            if (screenshotPath == null && driver != null) {
                screenshotPath = captureAndStoreScreenshot(result, driver);
            }

            if (screenshotPath != null) {
                extentTest.fail("Failure screenshot",
                        MediaEntityBuilder.createScreenCaptureFromPath(screenshotPath).build());
            } else {
                extentTest.warning("Screenshot was not captured because WebDriver was not available.");
            }
        } catch (RuntimeException exception) {
            extentTest.warning("Screenshot capture failed: " + exception.getMessage());
        }

        ExtentReportManager.flushReports();
        TEST.remove();
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        ExtentTest extentTest = getOrCreateExtentTest(result);
        if (markReported(result)) {
            extentTest.log(Status.SKIP, "Test skipped");
            if (result.getThrowable() != null) {
                extentTest.skip(result.getThrowable());
            }
            ExtentReportManager.flushReports();
        }
        TEST.remove();
    }

    @Override
    public void onFinish(ITestContext context) {
        ExtentReportManager.flushReports();
        TEST.remove();
    }

    private ExtentTest getOrCreateExtentTest(ITestResult result) {
        Object existingTest = result.getAttribute(EXTENT_TEST_ATTRIBUTE);
        if (existingTest instanceof ExtentTest) {
            return (ExtentTest) existingTest;
        }

        synchronized (result) {
            existingTest = result.getAttribute(EXTENT_TEST_ATTRIBUTE);
            if (existingTest instanceof ExtentTest) {
                return (ExtentTest) existingTest;
            }

            ExtentTest extentTest = ExtentReportManager.getExtentReports()
                    .createTest(result.getMethod().getMethodName())
                    .assignCategory(result.getTestClass().getName());
            result.setAttribute(EXTENT_TEST_ATTRIBUTE, extentTest);
            TEST.set(extentTest);
            return extentTest;
        }
    }

    private boolean markReported(ITestResult result) {
        synchronized (result) {
            if (Boolean.TRUE.equals(result.getAttribute(EXTENT_TEST_REPORTED_ATTRIBUTE))) {
                return false;
            }
            result.setAttribute(EXTENT_TEST_REPORTED_ATTRIBUTE, Boolean.TRUE);
            return true;
        }
    }

    private WebDriver getDriverFromResult(ITestResult result) {
        Object testInstance = result.getInstance();
        if (testInstance instanceof BaseTest) {
            return ((BaseTest) testInstance).getDriver();
        }
        return null;
    }

    private String getExistingScreenshotPath(ITestResult result) {
        Object existingPath = result.getAttribute(ScreenshotUtils.SCREENSHOT_PATH_ATTRIBUTE);
        if (existingPath instanceof String && !((String) existingPath).isBlank()) {
            return (String) existingPath;
        }
        return null;
    }

    private String captureAndStoreScreenshot(ITestResult result, WebDriver driver) {
        String screenshotPath = ScreenshotUtils.captureScreenshot(driver, result.getMethod().getMethodName());
        result.setAttribute(ScreenshotUtils.SCREENSHOT_PATH_ATTRIBUTE, screenshotPath);
        return screenshotPath;
    }
}
