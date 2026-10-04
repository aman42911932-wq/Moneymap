package com.moneymap.service;

import com.moneymap.dao.CategoryDAO;
import com.moneymap.model.Category;
import com.moneymap.model.ImportTransaction;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;

public class StatementParserService {

    private static class Rule {
        final Pattern pattern;
        final String categoryName;
        final String type; // 'expense' or 'income'

        Rule(String regex, String categoryName, String type) {
            this.pattern = Pattern.compile("(?i)\\b(" + regex + ")\\b");
            this.categoryName = categoryName;
            this.type = type;
        }
    }

    private static final List<Rule> EXPENSE_RULES = new ArrayList<>();
    private static final List<Rule> INCOME_RULES = new ArrayList<>();

    static {
        // Expense Rules (Word boundary regex to prevent false positives like 'parent' matching 'rent' or 'chocolate' matching 'ola')
        EXPENSE_RULES.add(new Rule("swiggy|zomato|starbucks|mcdonalds|burger|pizza|kfc|cafe|restaurant|food|dining|lunch|dinner", "Food & Dining", "expense"));
        EXPENSE_RULES.add(new Rule("blinkit|zepto|dmart|supermarket|groceries|grocery|instamart|bigbasket|nature's basket", "Groceries", "expense"));
        EXPENSE_RULES.add(new Rule("uber|ola|rapido|petrol|fuel|shell|metro|taxi|cab|bus|toll|fastag|transportation", "Transportation", "expense"));
        EXPENSE_RULES.add(new Rule("electricity|bescom|water|wifi|broadband|airtel|jio|vi|bill|utilities", "Utilities & Bills", "expense"));
        EXPENSE_RULES.add(new Rule("rent|rental|house rent|flat rent|apartment rent|housing|landlord", "Rent & Housing", "expense"));
        EXPENSE_RULES.add(new Rule("netflix|prime|spotify|cinema|movie|hotstar|pvr|bookmyshow|entertainment|games", "Entertainment", "expense"));
        EXPENSE_RULES.add(new Rule("pharmacy|apollo|hospital|medicine|clinic|doctor|medicines|healthcare|pharma", "Healthcare", "expense"));
        EXPENSE_RULES.add(new Rule("amazon|flipkart|myntra|zara|h&m|clothing|mall|shopping", "Shopping", "expense"));
        EXPENSE_RULES.add(new Rule("tuition|school|college|udemy|coursera|books|education|course", "Education", "expense"));
        EXPENSE_RULES.add(new Rule("flight|hotel|airbnb|makemytrip|indigo|irctc|train|travel|trip", "Travel", "expense"));
        EXPENSE_RULES.add(new Rule("salon|spa|barber|haircut|grooming|personal care", "Personal Care", "expense"));

        // Income Rules
        INCOME_RULES.add(new Rule("salary|payroll|wages|stipend|monthly salary", "Salary", "income"));
        INCOME_RULES.add(new Rule("freelance|consulting|contract|upwork|fiverr|client|project payment", "Freelance & Consulting", "income"));
        INCOME_RULES.add(new Rule("dividend|interest|stocks|zerodha|groww|mutual fund|investment", "Investments & Dividends", "income"));
        INCOME_RULES.add(new Rule("rental income|tenant rent|property rent", "Rental Income", "income"));
        INCOME_RULES.add(new Rule("gift|grant|cashback|refund|rewards", "Other Income", "income"));
    }

    private final CategoryDAO categoryDAO = new CategoryDAO();

    public CategoryMatch matchCategory(String description, String txnType) {
        if (description == null || description.trim().isEmpty()) {
            return new CategoryMatch(null, "Uncategorized");
        }

        List<Rule> rules = "credit".equalsIgnoreCase(txnType) ? INCOME_RULES : EXPENSE_RULES;
        for (Rule rule : rules) {
            if (rule.pattern.matcher(description).find()) {
                try {
                    Category cat = categoryDAO.findByNameAndType(rule.categoryName, rule.type);
                    if (cat != null) {
                        return new CategoryMatch(cat.getCategoryId(), cat.getName());
                    }
                    return new CategoryMatch(null, rule.categoryName);
                } catch (Exception ignored) {
                    return new CategoryMatch(null, rule.categoryName);
                }
            }
        }

        // Default category lookup if no specific rule matched
        try {
            String defaultCat = "credit".equalsIgnoreCase(txnType) ? "Other Income" : "Miscellaneous";
            String catType = "credit".equalsIgnoreCase(txnType) ? "income" : "expense";
            Category cat = categoryDAO.findByNameAndType(defaultCat, catType);
            if (cat != null) {
                return new CategoryMatch(cat.getCategoryId(), cat.getName());
            }
        } catch (Exception ignored) {}

        return new CategoryMatch(null, "Uncategorized");
    }

    public List<ImportTransaction> parseCsv(String csvContent, int userId) {
        List<ImportTransaction> list = new ArrayList<>();
        if (csvContent == null || csvContent.trim().isEmpty()) return list;

        String[] lines = csvContent.split("\\r?\\n");
        if (lines.length <= 1) return list;

        // Header detection
        int dateCol = 0, descCol = 1, amtCol = 2, typeCol = 3;
        String header = lines[0].toLowerCase();
        List<String> headerCols = parseCsvLine(header);
        for (int c = 0; c < headerCols.size(); c++) {
            String h = headerCols.get(c).trim();
            if (h.contains("date")) dateCol = c;
            else if (h.contains("desc") || h.contains("particular") || h.contains("detail")) descCol = c;
            else if (h.contains("amount") || h.contains("amt")) amtCol = c;
            else if (h.contains("type") || h.contains("dr/cr") || h.contains("txn")) typeCol = c;
        }

        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;

            List<String> cols = parseCsvLine(line);
            if (cols.size() > Math.max(dateCol, Math.max(descCol, amtCol))) {
                try {
                    String dateStr = cols.get(dateCol).trim();
                    String desc = cols.get(descCol).trim();
                    String amtStr = cols.get(amtCol).replaceAll("[^0-9.]", "").trim();
                    String typeStr = (typeCol < cols.size()) ? cols.get(typeCol).trim().toLowerCase() : "debit";

                    Date sqlDate = parseDate(dateStr);
                    if (sqlDate == null) continue;

                    BigDecimal amount = new BigDecimal(amtStr);
                    String txnType = (typeStr.contains("credit") || typeStr.contains("cr") || typeStr.equals("+")) ? "credit" : "debit";

                    CategoryMatch match = matchCategory(desc, txnType);

                    ImportTransaction tx = new ImportTransaction();
                    tx.setUserId(userId);
                    tx.setDate(sqlDate);
                    tx.setDescription(desc);
                    tx.setAmount(amount);
                    tx.setTxnType(txnType);
                    tx.setSuggestedCategory(match.name);
                    tx.setFinalCategoryId(match.id);
                    tx.setStatus("pending");

                    list.add(tx);
                } catch (Exception e) {
                    System.err.println("[StatementParserService] Skipping invalid CSV row " + i + ": " + e.getMessage());
                }
            }
        }
        return list;
    }

    private Date parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return null;
        String[] formats = {"yyyy-MM-dd", "dd-MM-yyyy", "dd/MM/yyyy", "MM/dd/yyyy", "yyyy/MM/dd"};
        for (String fmt : formats) {
            try {
                LocalDate ld = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern(fmt));
                return Date.valueOf(ld);
            } catch (Exception ignored) {}
        }
        return null;
    }

    private List<String> parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        result.add(cur.toString());
        return result;
    }

    public static class CategoryMatch {
        public final Integer id;
        public final String name;

        public CategoryMatch(Integer id, String name) {
            this.id = id;
            this.name = name;
        }
    }
}
