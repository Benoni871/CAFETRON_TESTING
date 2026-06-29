package com.cafetron.utilities;

import com.cafetron.reports.ExtentReportManager;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class ScreenshotUtils {
    public static final String SCREENSHOT_PATH_ATTRIBUTE = "failureScreenshotPath";
    private static final Path SCREENSHOT_DIR = Path.of("test-output", "Screenshots");
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    private ScreenshotUtils() {
    }

    public static String captureScreenshot(WebDriver driver, String screenshotName) {
        try {
            Files.createDirectories(SCREENSHOT_DIR);
            File sourceFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            String fileName = sanitize(screenshotName) + "_" + LocalDateTime.now().format(TIMESTAMP_FORMAT) + ".png";
            Path destination = SCREENSHOT_DIR.resolve(fileName);
            Files.copy(sourceFile.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);
            return ExtentReportManager.getReportDirectory().relativize(destination).toString().replace("\\", "/");
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("Unable to capture screenshot", exception);
        }
    }

    private static String sanitize(String value) {
        return value == null ? "screenshot" : value.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
