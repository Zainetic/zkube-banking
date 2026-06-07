package com.zkube.notifications;

import io.javalin.Javalin;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.List;

public class NotificationsApplication {

	private static final JedisPool jedisPool = new JedisPool("redis-cache", 6379);

    public static void main(String[] args) {
        
        // 1. Boot up a lightweight web server just so Kubernetes can check if it's alive
        Javalin app = Javalin.create().start(8083);

        app.get("/api/notifications/health", ctx -> {
            ctx.json("{\"status\": \"UP\", \"service\": \"notifications\"}");
        });

        System.out.println("Notifications Service running on port 8083");

        // 2. Start the Asynchronous Background Worker
        startRedisListener();
    }

    private static void startRedisListener() {
        new Thread(() -> {
            System.out.println("Listening for events on Redis 'notification_queue'...");
            
            try (Jedis jedis = jedisPool.getResource()) {
                while (true) {
                    // BRPOP blocks the thread indefinitely (0 seconds timeout) until a message arrives
                    List<String> message = jedis.brpop(0, "notification_queue");
                    
                    // message.get(0) is the queue name, message.get(1) is the actual payload
                    if (message != null && !message.isEmpty()) {
                        String payload = message.get(1);
                        System.out.println("MOCK SMS DISPATCHED -> " + payload);
                    }
                }
            } catch (Exception e) {
                System.err.println("Redis connection failed: " + e.getMessage());
            }
        }).start(); // Starts this loop in a totally separate thread!
    }
}