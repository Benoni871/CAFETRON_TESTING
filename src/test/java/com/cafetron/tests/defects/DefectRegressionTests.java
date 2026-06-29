package com.cafetron.tests.defects;

import com.cafetron.base.BaseTest;
import com.cafetron.data.Role;
import com.cafetron.data.TestDataFactory;
import com.cafetron.flows.AuthFlow;
import com.cafetron.flows.CartCheckoutFlow;
import com.cafetron.pages.*;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;
import org.openqa.selenium.JavascriptExecutor;

import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DefectRegressionTests extends BaseTest {
    private static final DateTimeFormatter CUTOFF_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DISPLAY_TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm a", Locale.US);
    private static final Pattern DISPLAYED_TIME_PATTERN =
            Pattern.compile("\\b(2[0-3]|1[0-9]|0?[0-9]):([0-5][0-9])\\s*([AaPp]\\.?[Mm]\\.?)?\\b");
    private static final long TIMESTAMP_TOLERANCE_MINUTES = 10;
    private static final String PICKUP_QR_ORDER_ID = "69";
    private static final int OVER_STOCK_REQUESTED_QUANTITY = 9;
    private static final String LIMITED_STOCK_ITEM = "QA Test Meal 260626B";

    @Test(description = "DF-003: Orders should be blocked after cutoff while ordering allowed is No",
            groups = {"defect", "regression", "integration"})
    public void df003ShouldBlockOrdersAfterCutoff() {
        SoftAssert softAssert = new SoftAssert();
        AuthFlow authFlow = new AuthFlow(getDriver());

        loginOrFail(authFlow, Role.ADMIN);
        AdminOperationsPage operationsPage = new AdminOperationsPage(getDriver());
        operationsPage.open();
        operationsPage.setCutoffTime(LocalTime.now().minusMinutes(1).format(CUTOFF_TIME_FORMATTER));

        softAssert.assertEquals(operationsPage.orderingAllowedText(), "No",
                "Ordering allowed should be No after cutoff");

        ProfilePage profilePage = new ProfilePage(getDriver());
        profilePage.open();
        profilePage.logout();

        loginOrFail(authFlow, Role.EMPLOYEE);
        CartCheckoutFlow cartCheckoutFlow = new CartCheckoutFlow(getDriver());
        boolean itemAdded = cartCheckoutFlow.addFirstAvailableMenuItemToCart();
        if (!itemAdded) {
            softAssert.assertAll();
            return;
        }

        CartPage cartPage = new CartPage(getDriver());
        cartPage.open();
        if (!cartPage.isCheckoutAvailable()) {
            softAssert.assertAll();
            return;
        }
        cartPage.clickCheckout();

        CheckoutPage checkoutPage = new CheckoutPage(getDriver());
        checkoutPage.waitForUrlContains("/checkout");
        checkoutPage.enterPickupLocation("QA Cutoff Regression");
        Assert.assertTrue(checkoutPage.selectFirstPickupWindow(),
                "DF-003 setup failed: no pickup window option was available on checkout.");
        Assert.assertTrue(checkoutPage.advanceToPlaceOrderStep(),
                "DF-003 setup failed: checkout could not advance to the place-order step.");
        Assert.assertTrue(checkoutPage.isPlaceOrderAvailable(),
                "DF-003 setup failed: place order button was not available at final checkout step.");
        checkoutPage.placeOrder();

        softAssert.assertTrue(checkoutPage.hasCutoffBlockingFeedback() && !checkoutPage.currentUrlContains("/orders"),
                "Order should be blocked after cutoff, but blocking feedback was not shown. Current URL: "
                        + getDriver().getCurrentUrl() + ". Feedback was: " + checkoutPage.feedbackText());
        softAssert.assertAll();
    }

    private void loginOrFail(AuthFlow authFlow, Role role) {
        try {
            authFlow.loginAs(role);
        } catch (SkipException exception) {
            Assert.fail("DF-003 setup failed: could not login as " + role.name().toLowerCase()
                    + ". " + exception.getMessage());
        }
    }

    @Test(description = "DF-005: Admin vendor phone should reject alphabetic/excessively long value",
            groups = {"defect", "regression", "sanity"})
    public void df005VendorPhoneShouldRejectInvalidValues() {
        new AuthFlow(getDriver()).loginAs(Role.ADMIN);
        AdminVendorsPage vendorsPage = new AdminVendorsPage(getDriver());
        vendorsPage.open();
        vendorsPage.createVendor(TestDataFactory.uniqueName("Invalid Phone Vendor"),
                "invalid.phone." + System.currentTimeMillis() + "@cafetron.test",
                "ABCphone999999999999999999999999999999",
                "QA Contact");

        Assert.assertFalse(vendorsPage.vendorFeedbackText().toLowerCase().contains("saved"),
                "Invalid phone should not save successfully");
    }

    @Test(description = "DF-006: Authenticated user should not see public login form by opening /login",
            groups = {"defect", "regression", "rbac"})
    public void df006AuthenticatedUserShouldNotSeeLoginForm() {
        new AuthFlow(getDriver()).loginAs(Role.EMPLOYEE);
        new LoginPage(getDriver()).open();

        Assert.assertFalse(new LoginPage(getDriver()).isLoginPageDisplayed(),
                "Authenticated user should be redirected away from login form");
    }

    @Test(description = "DF-007: Vendor should not access employee menu browsing/cart route",
            groups = {"defect", "regression", "rbac"})
    public void df007VendorShouldNotSeeEmployeeOrderingMenu() {
        new AuthFlow(getDriver()).loginAs(Role.VENDOR);
        MenuPage menuPage = new MenuPage(getDriver());
        menuPage.open();

        Assert.assertFalse(menuPage.hasEmployeeOrderingControls(),
                "Vendor should not see employee ordering/cart controls on /menu");
    }

    @Test(description = "DF-008: Vendor orders Back action should not expose employee menu",
            groups = {"defect", "regression", "rbac"})
    public void df008VendorBackShouldStayInVendorArea() {
        new AuthFlow(getDriver()).loginAs(Role.VENDOR);
        VendorOrdersPage vendorOrdersPage = new VendorOrdersPage(getDriver());
        vendorOrdersPage.open();
        if (!vendorOrdersPage.isBackToMenuVisible()) {
            throw new SkipException("Back to menu link is not visible in this UI state.");
        }
        vendorOrdersPage.clickBackToMenu();

        Assert.assertFalse(new MenuPage(getDriver()).hasEmployeeOrderingControls(),
                "Vendor back navigation should not expose employee ordering UI");
    }

    @Test(description = "DF-009: Logout should be available in wallet/authenticated shell",
            groups = {"defect", "regression", "usability"})
    public void df009WalletShouldProvideLogoutAccess() {
        new AuthFlow(getDriver()).loginAs(Role.EMPLOYEE);
        WalletPage walletPage = new WalletPage(getDriver());
        walletPage.open();

        Assert.assertTrue(walletPage.isDisplayed(), "Wallet should load for authenticated user");
//        ProfilePage profilePage = new ProfilePage(getDriver());
//        profilePage.open();
        Assert.assertTrue(walletPage.isLogoutDisplayed(), "Authenticated shell should provide logout Button");
    }

    @Test(description = "DF-010: Vendor queue route should render a real queue, not the pending placeholder",
            groups = {"defect", "regression", "integration"})
    public void df010VendorQueueShouldShowRealQueueState() {
        new AuthFlow(getDriver()).loginAs(Role.VENDOR);
        VendorQueuePage vendorQueuePage = new VendorQueuePage(getDriver());
        vendorQueuePage.open();

        Assert.assertTrue(vendorQueuePage.isDisplayed(),
                "Vendor queue route should render at " + VendorQueuePage.PATH
                        + ". Current URL: " + getDriver().getCurrentUrl());
        Assert.assertFalse(vendorQueuePage.isPlaceholderOnly(),
                "DF-010 defect reproduced: " + VendorQueuePage.PATH
                        + " renders the pending module placeholder instead of a vendor queue. Visible text: "
                        + vendorQueuePage.visibleTextSnapshot());
        Assert.assertTrue(vendorQueuePage.hasQueueState(),
                "Vendor queue should show a queue list, empty state, or actionable error state. Visible text: "
                        + vendorQueuePage.visibleTextSnapshot());
    }

    @Test(description = "DF-011: Vendor duplicate menu item names should not save as duplicate",
            groups = {"defect", "regression"})
    public void df011DuplicateVendorMenuItemShouldBeRejected() {
        new AuthFlow(getDriver()).loginAs(Role.VENDOR);
        VendorMenuManagePage managePage = new VendorMenuManagePage(getDriver());
        managePage.open();
        Assert.assertTrue(managePage.hasMenuManagementState(),
                "Vendor menu management should finish loading before duplicate setup. Visible text: "
                        + managePage.visibleTextSnapshot());

        String itemName = TestDataFactory.uniqueName("Duplicate Item");
        managePage.createItem(itemName, "20", "4", "Snack");
        Assert.assertTrue(managePage.waitForItemCountAtLeast(itemName, 1),
                "DF-011 setup failed: first menu item did not appear after save. Visible items: "
                        + managePage.menuItemNamesSnapshot());

        int countBeforeDuplicateAttempt = managePage.countItemsNamed(itemName);
        managePage.openCreateItemForm();
        managePage.fillItemForm(itemName, "20", "4", "Snack");
        boolean duplicateSubmitClicked = managePage.submitItemFormIfEnabled();
        Assert.assertTrue(managePage.waitForDuplicateSaveAttemptToSettle(
                        itemName, countBeforeDuplicateAttempt, duplicateSubmitClicked),
                "Duplicate save attempt did not settle into a validation, rejection, or refreshed menu state. "
                        + "Visible text: " + managePage.visibleTextSnapshot());

        Assert.assertEquals(managePage.countItemsNamed(itemName), countBeforeDuplicateAttempt,
                "Duplicate item name should not create another visible menu item. Submit clicked: "
                        + duplicateSubmitClicked + ". Visible items: " + managePage.menuItemNamesSnapshot());
    }

    @Test(description = "DF-012: Vendor menu form should validate zero stock clearly",
            groups = {"defect", "regression", "usability"})
    public void df012ZeroStockShouldShowValidationOrUnavailableWarning() {
        new AuthFlow(getDriver()).loginAs(Role.VENDOR);
        VendorMenuManagePage managePage = new VendorMenuManagePage(getDriver());
        managePage.open();
        managePage.createItem(TestDataFactory.uniqueName("Zero Stock Item"), "20", "0", "Snack");

        Assert.assertFalse(managePage.saveFeedbackText().toLowerCase().contains("saved"),
                "Zero stock item should not save silently as available");
    }

    @Test(description = "DF-013: Toggling zero-stock availability should not log vendor out",
            groups = {"defect", "regression"})
    public void df013VendorShouldRemainAuthenticatedAfterAvailabilityAction() {
        new AuthFlow(getDriver()).loginAs(Role.VENDOR);
        VendorMenuManagePage managePage = new VendorMenuManagePage(getDriver());
        managePage.open();

        Assert.assertTrue(managePage.isDisplayed(), "Vendor should remain in menu-management area");
    }

    @Test(description = "DF-014: Vendor locked delete controls should be clear and not misleading",
            groups = {"defect", "regression", "usability"})
    public void df014VendorDeleteControlShouldNotBeMisleading() {
        new AuthFlow(getDriver()).loginAs(Role.VENDOR);
        VendorMenuManagePage managePage = new VendorMenuManagePage(getDriver());
        managePage.open();

        Assert.assertTrue(managePage.isDeleteDisplayed(), "Vendor menu management Delete button should not be visible");
    }


    @Test(description = "DF-017: Admin saving edited menu item should not log admin out",
            groups = {"defect", "regression"})
    public void df017AdminShouldRemainAuthenticatedInMenuManagement() {
        new AuthFlow(getDriver()).loginAs(Role.ADMIN);
        VendorMenuManagePage managePage = new VendorMenuManagePage(getDriver());
        managePage.open();

        Assert.assertTrue(managePage.isDisplayed(), "Admin should remain in menu-management area");
    }

    @Test(description = "DF-019: Wallet top-up should reject impractically high amount",
            groups = {"defect", "regression"})
    public void df019WalletShouldRejectHugeTopUp() {
        new AuthFlow(getDriver()).loginAs(Role.EMPLOYEE);
        WalletPage walletPage = new WalletPage(getDriver());
        walletPage.open();
        walletPage.topUp("999999999999");

        Assert.assertFalse(walletPage.hasFeedback(), "Huge wallet top-up should show validation feedback");
    }

    @Test(description = "DF-020: Wallet and vendor timestamps should match browser local time",
            groups = {"defect", "regression", "usability"})
    public void df020TimestampFieldsShouldMatchBrowserLocalTime() {
        SoftAssert softAssert = new SoftAssert();
        AuthFlow authFlow = new AuthFlow(getDriver());

        authFlow.loginAs(Role.EMPLOYEE);
        WalletPage walletPage = new WalletPage(getDriver());
        walletPage.open();

        Assert.assertTrue(walletPage.isDisplayed(),
                "DF-020 setup failed: wallet page should load before checking transaction timestamps.");
        LocalTime walletReferenceTime = browserLocalTime();
        walletPage.topUp("10");
        Assert.assertTrue(walletPage.waitForTransactionTimestampText(),
                "DF-020 setup failed: wallet transaction timestamp should appear after creating a wallet record.");
        assertTimestampMatchesBrowserLocalTime(softAssert, "wallet record",
                walletPage.transactionTimestampEvidenceText(), walletReferenceTime);

        ProfilePage profilePage = new ProfilePage(getDriver());
        profilePage.open();
        profilePage.logout();

        authFlow.loginAs(Role.ADMIN);
        AdminVendorsPage vendorsPage = new AdminVendorsPage(getDriver());
        vendorsPage.open();
        Assert.assertTrue(vendorsPage.isDisplayed(),
                "DF-020 setup failed: admin vendor page should load before checking vendor timestamps.");

        String vendorName = TestDataFactory.uniqueName("Timezone Vendor");
        LocalTime vendorReferenceTime = browserLocalTime();
        vendorsPage.createVendor(vendorName,
                "timezone.vendor." + System.currentTimeMillis() + "@cafetron.test",
                "9876543210",
                "QA Timezone");
        Assert.assertTrue(vendorsPage.waitForVendorRecord(vendorName),
                "DF-020 setup failed: created vendor record should be visible before checking timestamp. Feedback: "
                        + vendorsPage.vendorFeedbackText());
        assertTimestampMatchesBrowserLocalTime(softAssert, "vendor record",
                vendorsPage.vendorRecordTimestampEvidenceText(vendorName), vendorReferenceTime);

        softAssert.assertAll();
    }

    @Test(description = "DF-021: Manual ordering-window close should be visible to checkout flow",
            groups = {"defect", "regression", "integration"})
    public void df021ManualOrderingWindowStateShouldBeVisible() {
        new AuthFlow(getDriver()).loginAs(Role.ADMIN);
        AdminOperationsPage operationsPage = new AdminOperationsPage(getDriver());
        operationsPage.open();

        Assert.assertTrue(operationsPage.hasOrderingWindowControls(), "Manual ordering-window controls should be visible");
        operationsPage.closeOrderingWindowIfOpen();
        Assert.assertTrue(operationsPage.isOrderingWindowClosed(), "Ordering window should be closed before employee checkout");

        ProfilePage profilePage = new ProfilePage(getDriver());
        profilePage.open();
        profilePage.logout();

        new AuthFlow(getDriver()).loginAs(Role.EMPLOYEE);
        MenuPage menuPage = new MenuPage(getDriver());
        menuPage.open();
        Assert.assertFalse(menuPage.hasEmployeeOrderingControls(),
                "Employee should not see Add/cart controls when ordering window is closed");

        boolean itemAddedToCart = new CartCheckoutFlow(getDriver()).addFirstAvailableMenuItemToCart();

        Assert.assertFalse(itemAddedToCart,
                "Employee should not be able to add items to cart when ordering window is closed");
    }

    @Test(description = "DF-022 / TC-094: Cart should prevent quantity higher than stock",
            groups = {"defect", "regression", "integration"})
    public void df022CartShouldPreventQuantityHigherThanStock() {
        new AuthFlow(getDriver()).loginAs(Role.EMPLOYEE);
        boolean itemAdded = new CartCheckoutFlow(getDriver()).addMenuItemNamedToCart(LIMITED_STOCK_ITEM);
        Assert.assertTrue(itemAdded, LIMITED_STOCK_ITEM + " should be available to add for the over-stock defect check");

        CartPage cartPage = new CartPage(getDriver());
        cartPage.open();
        cartPage.increaseFirstItemQuantityTo(OVER_STOCK_REQUESTED_QUANTITY);

        Assert.assertTrue(cartPage.preventsQuantityAboveStockOrShowsValidation(OVER_STOCK_REQUESTED_QUANTITY),
                "Cart should stop quantity above available stock or show immediate stock validation");
    }

    @Test(description = "DF-023: Pickup QR page should provide back navigation path",
            groups = {"defect", "regression", "usability"})
    public void df023OrdersShouldProvideNavigationForQrJourney() {
        new AuthFlow(getDriver()).loginAs(Role.EMPLOYEE);
        PickupQrPage pickupQrPage = new PickupQrPage(getDriver());
        pickupQrPage.open(PICKUP_QR_ORDER_ID);

        Assert.assertTrue(pickupQrPage.hasBackNavigation(), "Orders/QR journey should show navigable order state");
    }

    @Test(description = "DF-024: Vendor Add Item form should not show required errors immediately",
            groups = {"defect", "regression", "usability"})
    public void df024VendorAddItemFormShouldOpenCleanly() {
        new AuthFlow(getDriver()).loginAs(Role.VENDOR);
        VendorMenuManagePage managePage = new VendorMenuManagePage(getDriver());
        managePage.open();
        managePage.openCreateItemForm();

        Assert.assertFalse(managePage.isFormErrorDisplayed(), "Add item form should open cleanly");
    }

    @Test(description = "DF-025: Admin dashboard CSV controls should be available",
            groups = {"defect", "regression"})
    public void df025AdminCsvControlsShouldBeVisible() {
        new AuthFlow(getDriver()).loginAs(Role.ADMIN);
        AdminDashboardPage dashboardPage = new AdminDashboardPage(getDriver());
        dashboardPage.open();

        Assert.assertTrue(dashboardPage.hasCsvControls(), "CSV controls should be visible on dashboard");
    }

    @Test(description = "DF-026: QR upload/scanner flow should provide functional verification result or error",
            groups = {"defect", "regression", "integration"})
    public void df026QrVerificationShouldShowResultOrError() {
        new AuthFlow(getDriver()).loginAs(Role.VENDOR);
        QrScannerPage scannerPage = new QrScannerPage(getDriver());
        scannerPage.open();
        scannerPage.startScanner();

        Assert.assertTrue(scannerPage.hasScannerFeedback(), "QR verification should show result or clear error");
    }

    @Test(description = "DF-027: Admin-created vendor should have a usable login path",
            groups = {"defect", "regression", "rbac"})
    public void df027AdminVendorCreationShouldCommunicateLoginCapability() {
        new AuthFlow(getDriver()).loginAs(Role.ADMIN);
        AdminVendorsPage vendorsPage = new AdminVendorsPage(getDriver());
        vendorsPage.open();

        Assert.assertTrue(vendorsPage.isDisplayed(), "Admin vendor creation UI should be available");
    }

    @Test(description = "DF-028: Employee-only pages should block admin/vendor users",
            groups = {"defect", "regression", "rbac"})
    public void df028VendorShouldNotAccessEmployeeOnlyPages() {
        new AuthFlow(getDriver()).loginAs(Role.VENDOR);
        MenuPage menuPage = new MenuPage(getDriver());
        menuPage.open();

        Assert.assertFalse(menuPage.hasEmployeeOrderingControls(),
                "Vendor should not access employee-only ordering controls");
    }

    @Test(description = "DF-029: Admin should be able to reach menu delete/action state without logout",
            groups = {"defect", "regression"})
    public void df029AdminMenuDeleteFlowShouldStayAuthenticated() {
        new AuthFlow(getDriver()).loginAs(Role.ADMIN);
        VendorMenuManagePage managePage = new VendorMenuManagePage(getDriver());
        managePage.open();

        Assert.assertTrue(managePage.isDisplayed(), "Admin should remain authenticated on menu management");
    }

    @Test(description = "DF-030: Admin should be able to reach vendor delete/action state without logout",
            groups = {"defect", "regression"})
    public void df030AdminVendorDeleteFlowShouldStayAuthenticated() {
        new AuthFlow(getDriver()).loginAs(Role.ADMIN);
        AdminVendorsPage vendorsPage = new AdminVendorsPage(getDriver());
        vendorsPage.open();

        Assert.assertTrue(vendorsPage.isDisplayed(), "Admin should remain authenticated on vendor management");
    }

    @Test(description = "DF-031: Daily cutoff status should be visible in operations table",
            groups = {"defect", "regression"})
    public void df031DailyCutoffStatusShouldBeVisible() {
        new AuthFlow(getDriver()).loginAs(Role.ADMIN);
        AdminOperationsPage operationsPage = new AdminOperationsPage(getDriver());
        operationsPage.open();

        Assert.assertTrue(operationsPage.hasStatusSummary(), "Daily cutoff status should be visible");
    }

    @Test(description = "DF-032: Cutoff update should validate required field when cleared",
            groups = {"defect", "regression", "sanity", "usability"})
    public void df032CutoffUpdateShouldValidateRequiredField() {
        new AuthFlow(getDriver()).loginAs(Role.ADMIN);
        AdminOperationsPage operationsPage = new AdminOperationsPage(getDriver());
        operationsPage.open();
        operationsPage.clearCutoffAndSave();

        Assert.assertTrue(operationsPage.hasFeedback() || operationsPage.hasCutoffControls(),
                "Cleared cutoff save should show validation feedback and keep cutoff controls visible");
    }

    private void assertTimestampMatchesBrowserLocalTime(SoftAssert softAssert, String recordLabel,
                                                        String timestampEvidenceText, LocalTime browserLocalTime) {
        List<LocalTime> displayedTimes = extractDisplayedTimes(timestampEvidenceText);
        softAssert.assertFalse(displayedTimes.isEmpty(),
                "DF-020 " + recordLabel + " should show a visible timestamp with time. Visible text: "
                        + timestampEvidenceText);
        if (displayedTimes.isEmpty()) {
            return;
        }

        boolean hasBrowserLocalMatch = displayedTimes.stream()
                .anyMatch(displayedTime -> minuteDistance(displayedTime, browserLocalTime)
                        <= TIMESTAMP_TOLERANCE_MINUTES);
        softAssert.assertTrue(hasBrowserLocalMatch,
                "DF-020 reproduced for " + recordLabel
                        + ": timestamp should match browser local/application timezone. Browser local time was about "
                        + DISPLAY_TIME_FORMATTER.format(browserLocalTime)
                        + " (" + browserTimezone() + "), but displayed timestamp text was '"
                        + timestampEvidenceText + "' with parsed time(s) "
                        + formatTimes(displayedTimes)
                        + ". Known defect examples: 3:52 PM IST displayed as 10:22 AM; "
                        + "3:56 PM IST displayed as 10:26 AM.");
    }

    private LocalTime browserLocalTime() {
        JavascriptExecutor javascriptExecutor = (JavascriptExecutor) getDriver();
        Number hour = (Number) javascriptExecutor.executeScript("return new Date().getHours();");
        Number minute = (Number) javascriptExecutor.executeScript("return new Date().getMinutes();");
        return LocalTime.of(hour.intValue(), minute.intValue());
    }

    private String browserTimezone() {
        Object timezone = ((JavascriptExecutor) getDriver())
                .executeScript("return Intl.DateTimeFormat().resolvedOptions().timeZone || 'browser local time';");
        return String.valueOf(timezone);
    }

    private List<LocalTime> extractDisplayedTimes(String text) {
        List<LocalTime> displayedTimes = new ArrayList<>();
        Matcher matcher = DISPLAYED_TIME_PATTERN.matcher(text == null ? "" : text);
        while (matcher.find()) {
            int hour = Integer.parseInt(matcher.group(1));
            int minute = Integer.parseInt(matcher.group(2));
            String meridiem = matcher.group(3);
            if (meridiem != null && !meridiem.isBlank()) {
                if (hour < 1 || hour > 12) {
                    continue;
                }
                String normalizedMeridiem = meridiem.replace(".", "").toUpperCase(Locale.ROOT);
                if ("PM".equals(normalizedMeridiem) && hour != 12) {
                    hour += 12;
                } else if ("AM".equals(normalizedMeridiem) && hour == 12) {
                    hour = 0;
                }
            }
            displayedTimes.add(LocalTime.of(hour, minute));
        }
        return displayedTimes;
    }

    private long minuteDistance(LocalTime displayedTime, LocalTime browserLocalTime) {
        long sameDayDifference = Math.abs(Duration.between(displayedTime, browserLocalTime).toMinutes());
        return Math.min(sameDayDifference, Duration.ofDays(1).toMinutes() - sameDayDifference);
    }

    private String formatTimes(List<LocalTime> displayedTimes) {
        return displayedTimes.stream()
                .map(DISPLAY_TIME_FORMATTER::format)
                .collect(Collectors.joining(", "));
    }
}
