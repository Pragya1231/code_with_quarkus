package org.project;

import io.quarkus.hibernate.orm.panache.Panache;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.util.HashMap;
import java.util.Map;

@Path("/update")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UpdateUserResource {

    private static final Logger log = Logger.getLogger(UpdateUserResource.class);

    @PUT
    @Transactional
    public Response updateUser(Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");

        log.infov("Received update request for username: {0}", username);

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            log.warn("Username or password missing in update request");
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "Username and password are required"))
                    .build();
        }

        User user = User.find("username", username).firstResult();

        if (user == null) {
            log.warnv("User not found for username: {0}", username);
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("message", "User not found"))
                    .build();
        }

        if (!user.active) {
            log.warnv("Attempt to update inactive user: {0}", username);
            return Response.status(Response.Status.CONFLICT)
                    .entity(Map.of("message", "User is inactive. Reactivate to update details."))
                    .build();
        }

        if (!user.password.equals(password)) {
            log.errorv("Invalid password for username: {0}", username);
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("message", "Invalid password"))
                    .build();
        }

        String newFirstName = request.get("firstName");
        String newLastName = request.get("lastName");
        String newEmail = request.get("email");

        log.debugv("Updating user {0} with new values -> firstName: {1}, lastName: {2}, email: {3}",
                username, newFirstName, newLastName, newEmail);

        // Validate email uniqueness if provided
        if (newEmail != null && !newEmail.equalsIgnoreCase(user.email)) {
            User existingEmailUser = User.find("email", newEmail).firstResult();
            if (existingEmailUser != null) {
                log.warnv("Email conflict during update for username: {0}, attempted new email: {1}", username, newEmail);
                return Response.status(Response.Status.CONFLICT)
                        .entity(Map.of("message", "Email already in use"))
                        .build();
            }
            user.email = newEmail;
        }

        if (newFirstName != null && !newFirstName.isBlank()) {
            user.firstName = newFirstName;
        }

        if (newLastName != null && !newLastName.isBlank()) {
            user.lastName = newLastName;
        }

        try {
            user.persist();
            log.infov("User updated successfully: {0}", username);
        } catch (Exception e) {
            log.errorv(e, "Database error while updating user: {0}", username);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("message", "Database error while updating user"))
                    .build();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("message", "User updated successfully");
        response.put("username", user.username);
        response.put("firstName", user.firstName);
        response.put("lastName", user.lastName);
        response.put("email", user.email);

        return Response.ok(response).build();
    }
}
