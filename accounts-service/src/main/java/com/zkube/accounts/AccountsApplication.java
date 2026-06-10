package com.zkube.accounts;

import io.javalin.Javalin;
import org.jooq.DSLContext;
import org.jooq.Record;
import static org.jooq.impl.DSL.*;

import java.math.BigDecimal;
import java.util.UUID;

public class AccountsApplication {

    public static void main(String[] args) {
        
        // 1. Boot up the database connection pool
        Database.init();

        // 2. Start the web server
        Javalin app = Javalin.create().start(8081);

        app.get("/api/accounts/health", ctx -> {
            ctx.json("{\"status\": \"UP\", \"service\": \"accounts\"}");
        });

        // 3. Endpoint: Creating a new bank account
        app.post("/api/accounts", ctx -> {
            // Parse incoming JSON
            String ownerName = ctx.bodyAsClass(AccountRequest.class).ownerName();
            BigDecimal initialBalance = ctx.bodyAsClass(AccountRequest.class).initialBalance();

            DSLContext dsl = Database.getContext();
            
            // Execute jOOQ Insert
            UUID newId = dsl.insertInto(table("accounts"), field("owner_name"), field("balance"))
               .values(ownerName, initialBalance)
               .returningResult(field("id", UUID.class))
               .fetchOneInto(UUID.class);

            ctx.status(201).json("{\"accountId\": \"" + newId + "\", \"status\": \"CREATED\"}");
        });

        // 4. Endpoint: Fetch an account's balance
        app.get("/api/accounts/{id}", ctx -> {
            UUID id = UUID.fromString(ctx.pathParam("id"));
            
            DSLContext dsl = Database.getContext();
            
            // Execute jOOQ Select
            Record record = dsl.select(field("id"), field("owner_name"), field("balance"))
               .from(table("accounts"))
               .where(field("id").eq(id))
               .fetchOne();

            if (record != null) {
                ctx.json(record.intoMap());
            } else {
                ctx.status(404).json("{\"error\": \"Account not found\"}");
            }
        });

        System.out.println("Accounts Service running with DB Connection on port 8081");
    }

    record AccountRequest(String ownerName, BigDecimal initialBalance) {}
}