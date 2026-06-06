package com.zkube.accounts;

import io.javalin.Javalin;

public class AccountsApplication {
    public static void main(String[] args) {
        
        Javalin app = Javalin.create().start(8081);

        app.get("/api/accounts/health", ctx -> {
            ctx.json("{\"status\": \"UP\", \"service\": \"accounts\"}");
        });
        
        System.out.println(" Accounts Service started on port 8081");
    }
}