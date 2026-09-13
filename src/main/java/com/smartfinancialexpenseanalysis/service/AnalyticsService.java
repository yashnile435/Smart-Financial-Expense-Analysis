package com.smartfinancialexpenseanalysis.service;

import com.smartfinancialexpenseanalysis.dto.AdvancedAnalyticsResponse;
import com.smartfinancialexpenseanalysis.dto.CategoryComparisonResponse;
import com.smartfinancialexpenseanalysis.dto.ComparisonDirection;
import com.smartfinancialexpenseanalysis.dto.SpendingComparisonResponse;
import com.smartfinancialexpenseanalysis.entity.User;
import com.smartfinancialexpenseanalysis.exception.BadRequestException;
import com.smartfinancialexpenseanalysis.exception.UnauthorizedException;
import com.smartfinancialexpenseanalysis.repository.ExpenseRepository;
import com.smartfinancialexpenseanalysis.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Service providing spending comparison between periods and advanced financial metrics.
 */
@Service
public class AnalyticsService {

    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;

    public AnalyticsService(ExpenseRepository expenseRepository, UserRepository userRepository) {
        this.expenseRepository = expenseRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public SpendingComparisonResponse compareSpending(Integer month, Integer year,
                                                      Integer comparisonMonth, Integer comparisonYear,
                                                      String userEmail) {
        User user = getUserByEmail(userEmail);
        Long userId = user.getId();

        YearMonth currentYm;
        if (month == null || year == null) {
            currentYm = YearMonth.now();
        } else {
            validateMonthAndYear(month, year);
            currentYm = YearMonth.of(year, month);
        }

        YearMonth compYm;
        if (comparisonMonth == null || comparisonYear == null) {
            compYm = currentYm.minusMonths(1);
        } else {
            validateMonthAndYear(comparisonMonth, comparisonYear);
            compYm = YearMonth.of(comparisonYear, comparisonMonth);
        }

        LocalDate start1 = currentYm.atDay(1);
        LocalDate end1 = currentYm.atEndOfMonth();
        LocalDate start2 = compYm.atDay(1);
        LocalDate end2 = compYm.atEndOfMonth();

        BigDecimal currentTotal = expenseRepository.sumAmountByUserIdAndDateBetween(userId, start1, end1);
        currentTotal = currentTotal != null ? currentTotal.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        BigDecimal compTotal = expenseRepository.sumAmountByUserIdAndDateBetween(userId, start2, end2);
        compTotal = compTotal != null ? compTotal.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        BigDecimal diff = currentTotal.subtract(compTotal).setScale(2, RoundingMode.HALF_UP);

        ComparisonDirection direction;
        if (diff.compareTo(BigDecimal.ZERO) > 0) {
            direction = ComparisonDirection.INCREASED;
        } else if (diff.compareTo(BigDecimal.ZERO) < 0) {
            direction = ComparisonDirection.DECREASED;
        } else {
            direction = ComparisonDirection.UNCHANGED;
        }

        BigDecimal pctChange;
        if (compTotal.compareTo(BigDecimal.ZERO) == 0) {
            pctChange = currentTotal.compareTo(BigDecimal.ZERO) > 0
                    ? new BigDecimal("100.00")
                    : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        } else {
            pctChange = diff.abs()
                    .multiply(BigDecimal.valueOf(100))
                    .divide(compTotal, 2, RoundingMode.HALF_UP);
        }

        // Monthly average over past 6 consecutive months
        BigDecimal sixMonthTotal = BigDecimal.ZERO;
        YearMonth pastSixStart = currentYm.minusMonths(5);
        for (int i = 0; i < 6; i++) {
            YearMonth ym = pastSixStart.plusMonths(i);
            BigDecimal mSpend = expenseRepository.sumAmountByUserIdAndDateBetween(userId, ym.atDay(1), ym.atEndOfMonth());
            if (mSpend != null) {
                sixMonthTotal = sixMonthTotal.add(mSpend);
            }
        }
        BigDecimal monthlyAvg = sixMonthTotal.divide(BigDecimal.valueOf(6), 2, RoundingMode.HALF_UP);
        BigDecimal currentVsAvgDiff = currentTotal.subtract(monthlyAvg).setScale(2, RoundingMode.HALF_UP);

        // Category breakdown comparison
        List<Object[]> rows1 = expenseRepository.sumAmountAndCountByUserIdAndDateBetweenGroupByCategory(userId, start1, end1);
        List<Object[]> rows2 = expenseRepository.sumAmountAndCountByUserIdAndDateBetweenGroupByCategory(userId, start2, end2);

        Map<Long, String> categoryNames = new HashMap<>();
        Map<Long, BigDecimal> catAmounts1 = new HashMap<>();
        for (Object[] row : rows1) {
            Long catId = (Long) row[0];
            String catName = (String) row[1];
            BigDecimal amt = ((BigDecimal) row[2]).setScale(2, RoundingMode.HALF_UP);
            categoryNames.put(catId, catName);
            catAmounts1.put(catId, amt);
        }

        Map<Long, BigDecimal> catAmounts2 = new HashMap<>();
        for (Object[] row : rows2) {
            Long catId = (Long) row[0];
            String catName = (String) row[1];
            BigDecimal amt = ((BigDecimal) row[2]).setScale(2, RoundingMode.HALF_UP);
            categoryNames.put(catId, catName);
            catAmounts2.put(catId, amt);
        }

        Set<Long> allCatIds = new HashSet<>();
        allCatIds.addAll(catAmounts1.keySet());
        allCatIds.addAll(catAmounts2.keySet());

        List<CategoryComparisonResponse> catComparisons = new ArrayList<>();
        for (Long catId : allCatIds) {
            String catName = categoryNames.get(catId);
            BigDecimal cAmt = catAmounts1.getOrDefault(catId, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            BigDecimal pAmt = catAmounts2.getOrDefault(catId, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            BigDecimal cDiff = cAmt.subtract(pAmt).setScale(2, RoundingMode.HALF_UP);

            ComparisonDirection cDir;
            if (cDiff.compareTo(BigDecimal.ZERO) > 0) {
                cDir = ComparisonDirection.INCREASED;
            } else if (cDiff.compareTo(BigDecimal.ZERO) < 0) {
                cDir = ComparisonDirection.DECREASED;
            } else {
                cDir = ComparisonDirection.UNCHANGED;
            }

            BigDecimal cPct;
            if (pAmt.compareTo(BigDecimal.ZERO) == 0) {
                cPct = cAmt.compareTo(BigDecimal.ZERO) > 0 ? new BigDecimal("100.00") : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            } else {
                cPct = cDiff.abs().multiply(BigDecimal.valueOf(100)).divide(pAmt, 2, RoundingMode.HALF_UP);
            }

            catComparisons.add(new CategoryComparisonResponse(catId, catName, cAmt, pAmt, cDiff, cPct, cDir));
        }

        catComparisons.sort((a, b) -> b.getCurrentAmount().compareTo(a.getCurrentAmount()));

        return new SpendingComparisonResponse(
                currentYm.getMonthValue(),
                currentYm.getYear(),
                compYm.getMonthValue(),
                compYm.getYear(),
                currentTotal,
                compTotal,
                diff,
                pctChange,
                direction,
                monthlyAvg,
                currentVsAvgDiff,
                catComparisons
        );
    }

    @Transactional(readOnly = true)
    public AdvancedAnalyticsResponse getAdvancedAnalytics(String userEmail) {
        User user = getUserByEmail(userEmail);
        Long userId = user.getId();

        long totalCount = expenseRepository.countByUserId(userId);
        BigDecimal totalAmount = expenseRepository.sumAmountByUserId(userId);
        totalAmount = totalAmount != null ? totalAmount.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        BigDecimal maxExpense = expenseRepository.findMaxAmountByUserId(userId);
        maxExpense = maxExpense != null ? maxExpense.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        // Highest and lowest spending categories
        List<Object[]> catRows = expenseRepository.sumAmountByUserIdGroupByCategory(userId);
        String highestCat = "None";
        BigDecimal highestCatAmt = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        String lowestCat = "None";
        BigDecimal lowestCatAmt = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        if (!catRows.isEmpty()) {
            Object[] highestRow = catRows.get(0);
            highestCat = (String) highestRow[1];
            highestCatAmt = ((BigDecimal) highestRow[2]).setScale(2, RoundingMode.HALF_UP);

            Object[] lowestRow = catRows.get(catRows.size() - 1);
            lowestCat = (String) lowestRow[1];
            lowestCatAmt = ((BigDecimal) lowestRow[2]).setScale(2, RoundingMode.HALF_UP);
        }

        // Daily and monthly averages
        LocalDate earliest = expenseRepository.findEarliestDateByUserId(userId);
        BigDecimal avgDaily = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal avgMonthly = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        if (earliest != null && totalAmount.compareTo(BigDecimal.ZERO) > 0) {
            long days = Math.max(1, ChronoUnit.DAYS.between(earliest, LocalDate.now()) + 1);
            avgDaily = totalAmount.divide(BigDecimal.valueOf(days), 2, RoundingMode.HALF_UP);

            long months = Math.max(1, (days + 29) / 30);
            avgMonthly = totalAmount.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);
        }

        return new AdvancedAnalyticsResponse(
                highestCat,
                highestCatAmt,
                lowestCat,
                lowestCatAmt,
                avgDaily,
                avgMonthly,
                maxExpense,
                totalCount,
                totalAmount
        );
    }

    private User getUserByEmail(String email) {
        if (email == null || email.trim().isEmpty() || "anonymousUser".equalsIgnoreCase(email)) {
            throw new UnauthorizedException("User is not authenticated");
        }
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("User not found"));
    }

    private void validateMonthAndYear(Integer month, Integer year) {
        if (month == null || month < 1 || month > 12) {
            throw new BadRequestException("Month must be between 1 and 12");
        }
        if (year == null || year < 2000) {
            throw new BadRequestException("Year must be 2000 or later");
        }
    }
}
