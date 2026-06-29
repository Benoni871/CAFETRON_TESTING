package com.cafetron.reports;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;

import java.nio.file.Files;
import java.nio.file.Path;

public final class ExtentReportManager {
    private static final Path REPORT_PATH = Path.of("test-output", "ExtentReports", "Cafetron-Automation-Report.html");
    private static ExtentReports extentReports;

    private ExtentReportManager() {
    }

    public static synchronized ExtentReports getExtentReports() {
        if (extentReports == null) {
            createReport();
        }
        return extentReports;
    }

    public static Path getReportDirectory() {
        return REPORT_PATH.getParent();
    }

    public static synchronized void flushReports() {
        if (extentReports != null) {
            extentReports.flush();
        }
    }

    private static void createReport() {
        try {
            Files.createDirectories(getReportDirectory());
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to create report directory", exception);
        }

        ExtentSparkReporter sparkReporter = new ExtentSparkReporter(REPORT_PATH.toString());
        sparkReporter.config().setDocumentTitle("Cafetron Automation Report");
        sparkReporter.config().setReportName("Cafetron Hybrid Framework");
        sparkReporter.config().setTheme(Theme.STANDARD);

        extentReports = new ExtentReports();
        extentReports.attachReporter(sparkReporter);
        extentReports.setSystemInfo("Application", "Cafetron");
        extentReports.setSystemInfo("Framework", "Selenium Java TestNG Hybrid");
        extentReports.setSystemInfo("Environment", "QA");
    }
}
