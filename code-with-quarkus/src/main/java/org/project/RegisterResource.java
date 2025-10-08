package org.project;

import io.quarkus.logging.Log;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Path("/register")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RegisterResource {

    private static final Object USERNAME_LOCK = new Object();
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(8);

    @POST
    @Transactional
    public Response register(Map<String, String> request) {
        Log.info("Received registration request: " + request);

        String firstName = request.get("firstName");
        String lastName = request.get("lastName");
        String email = request.get("email");

        if (firstName == null || firstName.isEmpty() || email == null || email.isEmpty()) {
            Log.warn("Invalid request: missing firstName or email");
            return error("firstName and email are required", Response.Status.BAD_REQUEST);
        }

        User existing = User.find("email", email).firstResult();
        if (existing != null) {
            Log.warn("Registration failed: Email already exists -> " + email);
            return error("Email already exists", Response.Status.CONFLICT);
        }

        String username = generateUniqueUsername(firstName, lastName, email);
        Log.debug("Generated unique username: " + username);

        String password;
        try {
            password = EncryptionUtil.encrypt(username);
        } catch (Exception e) {
            Log.error("Password encryption failed", e);
            return error("Error generating password", Response.Status.INTERNAL_SERVER_ERROR);
        }

        User user = new User();
        user.firstName = firstName;
        user.lastName = lastName;
        user.email = email;
        user.username = username;
        user.password = password;

        user.persist();

        Log.infof("User registered successfully: username=%s, email=%s", username, email);

        Map<String, String> response = new HashMap<>();
        response.put("username", username);
        response.put("password", password);

        return Response.ok(response).build();
    }


    @POST
    @Path("/bulk")
    @Transactional
    public Response registerBulk(List<Map<String, String>> users) {
        Log.info("Received bulk registration request. Total users: " + users.size());

        List<CompletableFuture<Map<String, String>>> futures = users.stream()
                .map(user -> CompletableFuture.supplyAsync(() -> registerUser(user), EXECUTOR))
                .collect(Collectors.toList());

        List<Map<String, String>> results = futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());

        Log.info("Bulk registration completed. Successfully processed " + results.size() + " users.");
        return Response.ok(results).build();
    }

    @Transactional
    protected Map<String, String> registerUser(Map<String, String> request) {
        Map<String, String> result = new HashMap<>();
        try {
            String firstName = request.get("firstName");
            String lastName = request.get("lastName");
            String email = request.get("email");

            if (firstName == null || firstName.isEmpty() || email == null || email.isEmpty()) {
                Log.warn("Invalid data in bulk request: " + request);
                result.put("status", "failed");
                result.put("error", "Missing firstName or email");
                return result;
            }

            if (User.find("email", email).firstResult() != null) {
                Log.warn("Skipping duplicate email in bulk upload: " + email);
                result.put("email", email);
                result.put("status", "duplicate");
                return result;
            }

            String username = generateUniqueUsername(firstName, lastName, email);
            String password = EncryptionUtil.encrypt(username);

            User user = new User();
            user.firstName = firstName;
            user.lastName = lastName;
            user.email = email;
            user.username = username;
            user.password = password;
            user.persist();

            Log.infof("User registered in bulk: username=%s, email=%s", username, email);

            result.put("username", username);
            result.put("password", password);
            result.put("email", email);
            result.put("status", "success");

        } catch (Exception e) {
            Log.error("Error during bulk user registration", e);
            result.put("status", "failed");
            result.put("error", e.getMessage());
        }
        return result;
    }


    private String generateUniqueUsername(String firstName, String lastName, String email) {
        synchronized (USERNAME_LOCK) {
            String baseUsername = firstName.toLowerCase();
            if (lastName != null && !lastName.isBlank()) {
                baseUsername += lastName.toLowerCase();
            } else if (email != null && email.contains("@")) {
                baseUsername += email.split("@")[0].toLowerCase();
            }

            String username = baseUsername;
            int counter = 1;
            while (User.find("username", username).firstResult() != null) {
                username = baseUsername + counter;
                counter++;
            }
            return username;
        }
    }


    private Response error(String message, Response.Status status) {
        Map<String, String> err = new HashMap<>();
        err.put("message", message);
        return Response.status(status).entity(err).build();
    }
}
