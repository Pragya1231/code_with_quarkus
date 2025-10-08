package org.project;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;


@Path("/register")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RegisterResource {

    private static final Logger log = Logger.getLogger(RegisterResource.class);
    private static final String SECRET_KEY = "MySuperSecretKey";
    private static final int THREAD_POOL_SIZE = 5;

    private static SecretKeySpec getKeySpec() {
        return new SecretKeySpec(SECRET_KEY.getBytes(), "AES");
    }

    @POST
    @Transactional
    public Response register(Map<String, String> request) {

        return handleSingleRegistration(request);
    }

    @POST
    @Path("/bulk")
    @Transactional
    public Response bulkRegister(List<Map<String, String>> users) {
        if (users == null || users.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "User list cannot be empty"))
                    .build();
        }

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        List<CompletableFuture<Map<String, Object>>> futures = new ArrayList<>();

        for (Map<String, String> userRequest : users) {
            CompletableFuture<Map<String, Object>> future = CompletableFuture.supplyAsync(() -> {
                try {
                    Response response = handleSingleRegistration(userRequest);
                    return Map.of("user", userRequest.get("email"), "status", response.getStatus());
                } catch (Exception e) {
                    log.error("Error processing user: " + userRequest.get("email"), e);
                    return Map.of("user", userRequest.get("email"), "status", 500, "error", e.getMessage());
                }
            }, executor);
            futures.add(future);
        }

        List<Map<String, Object>> results = futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());

        executor.shutdown();
        return Response.ok(results).build();
    }



    // ───────────────────────────────
    // 🔹 Helper - Register a Single User
    // ───────────────────────────────
    @Transactional
    public Response handleSingleRegistration(Map<String, String> request) {
        String firstName = request.get("firstName");
        String lastName = request.get("lastName");
        String email = request.get("email");

        if (firstName == null || firstName.isEmpty() || email == null || email.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "firstName and email are required"))
                    .build();
        }

        User existing = User.find("email", email).firstResult();
        if (existing != null) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(Map.of("message", "Email already exists"))
                    .build();
        }

        String baseUsername = firstName.toLowerCase();
        String username = baseUsername;
        int counter = 1;
        while (User.find("username", username).firstResult() != null) {
            username = baseUsername + counter;
            counter++;
        }

        String password;
        try {
            password = encrypt(username);
        } catch (Exception e) {
            log.error("Error generating password", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("message", "Error generating password"))
                    .build();
        }

        User user = new User();
        user.firstName = firstName;
        user.lastName = lastName;
        user.email = email;
        user.username = username;
        user.password = password;
        user.persist();

        log.infov("Registered user: {0}", email);

        return Response.ok(Map.of("username", username, "password", password)).build();
    }

    // ───────────────────────────────
    // 🔹 Encryption Logic
    // ───────────────────────────────
    public static String encrypt(String input) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, getKeySpec());
        byte[] encryptedBytes = cipher.doFinal(input.getBytes());
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }
}
