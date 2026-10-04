package com.moneymap;

import com.moneymap.servlet.*;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppContext;

import java.io.File;

public class Server {
    public static void main(String[] args) throws Exception {
        int port = 8080;
        String portEnv = System.getenv("PORT");
        if (portEnv != null && !portEnv.isEmpty()) {
            try {
                port = Integer.parseInt(portEnv);
            } catch (NumberFormatException ignored) {}
        }

        org.eclipse.jetty.server.Server jettyServer = new org.eclipse.jetty.server.Server(port);

        WebAppContext webAppContext = new WebAppContext();
        webAppContext.setContextPath("/");

        String webappDir = "src/main/webapp";
        File dir = new File(webappDir);
        if (!dir.exists()) {
            webappDir = ".";
        }
        webAppContext.setResourceBase(webappDir);
        webAppContext.setWelcomeFiles(new String[]{"dashboard.html", "index.html"});

        // Register All Servlets
        webAppContext.addServlet(new ServletHolder(new AuthServlet()), "/api/auth/*");
        webAppContext.addServlet(new ServletHolder(new AuthServlet()), "/api/me");
        webAppContext.addServlet(new ServletHolder(new ExpenseServlet()), "/api/expenses/*");
        webAppContext.addServlet(new ServletHolder(new IncomeServlet()), "/api/income/*");
        webAppContext.addServlet(new ServletHolder(new ReportServlet()), "/api/reports/*");
        webAppContext.addServlet(new ServletHolder(new AccountServlet()), "/api/accounts/*");
        webAppContext.addServlet(new ServletHolder(new BudgetServlet()), "/api/budgets/*");
        webAppContext.addServlet(new ServletHolder(new GoalServlet()), "/api/goals/*");
        webAppContext.addServlet(new ServletHolder(new RecurringServlet()), "/api/recurring/*");
        webAppContext.addServlet(new ServletHolder(new CategoryServlet()), "/api/categories/*");
        webAppContext.addServlet(new ServletHolder(new NotificationServlet()), "/api/notifications/*");
        webAppContext.addServlet(new ServletHolder(new ImportServlet()), "/api/imports/*");
        webAppContext.addServlet(new ServletHolder(new AdminServlet()), "/api/admin/*");

        jettyServer.setHandler(webAppContext);

        System.out.println("=================================================");
        System.out.println("  MoneyMap Server started on http://localhost:" + port);
        System.out.println("  Access Dashboard: http://localhost:" + port + "/dashboard.html");
        System.out.println("  Access Login:     http://localhost:" + port + "/index.html");
        System.out.println("=================================================");

        jettyServer.start();
        jettyServer.join();
    }
}
