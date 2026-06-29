package com.cafetron.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.stream.Collectors;

public class WalletPage extends BasePage {
    private final By page = By.id("wallet-page");
    private final By balanceCard = By.id("wallet-balance-card");
    private final By balanceValue = By.id("wallet-balance-value");
    private final By topUpInput = By.id("wallet-topup-amount-input");
    private final By topUpButton = By.id("wallet-topup-submit-btn");
    private final By transactionList = By.id("wallet-transaction-list");
    private final By emptyTransactions = By.id("wallet-transactions-empty-state");
    private final By successMessage = By.id("wallet-success-message");
    private final By errorMessage = By.id("wallet-error-message");
    private final By logoutButton = By.id("profile-logout-btn");
    private final By transactionTimestampCandidates = By.cssSelector(
            "#wallet-transaction-list time, #wallet-transaction-list [datetime], "
                    + "#wallet-transaction-list [id*='time'], #wallet-transaction-list [id*='date'], "
                    + "#wallet-transaction-list [class*='time'], #wallet-transaction-list [class*='date'], "
                    + "#wallet-transaction-list [data-testid*='time'], #wallet-transaction-list [data-testid*='date']");


    public WalletPage(WebDriver driver) {
        super(driver);
    }

    public void open() {
        navigateTo("/wallet");
    }

    public boolean isDisplayed() {
        return isDisplayed(page) && isDisplayed(balanceCard);
    }

    public boolean hasBalance() {
        return isDisplayed(balanceValue);
    }

    public boolean isLogoutDisplayed(){
        return isDisplayed(logoutButton);
    }

    public boolean hasAuthenticatedNavigation() {
        return isLogoutDisplayed();
    }

    public void topUp(String amount) {
        type(topUpInput, amount);
        click(topUpButton);
    }

    public boolean hasTransactionArea() {
        return isDisplayed(transactionList) || isDisplayed(emptyTransactions);
    }

    public boolean hasFeedback() {
        return isDisplayed(successMessage) || isDisplayed(errorMessage);
    }

    public boolean waitForTransactionTimestampText() {
        try {
            wait.until(driver -> !transactionTimestampEvidenceTextNow().isBlank());
            return true;
        } catch (TimeoutException exception) {
            return false;
        }
    }

    public String transactionTimestampEvidenceText() {
        String evidenceText = transactionTimestampEvidenceTextNow();
        return evidenceText.isBlank() ? getOptionalText(page) : evidenceText;
    }

    private String transactionTimestampEvidenceTextNow() {
        String timestampCandidateText = findAll(transactionTimestampCandidates).stream()
                .filter(WebElement::isDisplayed)
                .map(this::timestampTextFrom)
                .filter(text -> !text.isBlank())
                .collect(Collectors.joining(" "));
        if (!timestampCandidateText.isBlank()) {
            return timestampCandidateText;
        }

        return findAll(transactionList).stream()
                .filter(WebElement::isDisplayed)
                .map(WebElement::getText)
                .map(String::trim)
                .filter(text -> !text.isBlank())
                .collect(Collectors.joining(" "));
    }

    private String timestampTextFrom(WebElement element) {
        String text = element.getText().trim();
        if (!text.isBlank()) {
            return text;
        }

        String datetime = element.getDomAttribute("datetime");
        if (datetime != null && !datetime.isBlank()) {
            return datetime.trim();
        }

        String title = element.getDomAttribute("title");
        return title == null ? "" : title.trim();
    }
}
