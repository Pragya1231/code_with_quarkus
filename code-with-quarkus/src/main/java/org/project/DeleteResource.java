package org.project;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.util.HashMap;
import java.util.Map;

@Path("/delete")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DeleteResource {

    private static final Logger log = Logger.getLogger(DeleteResource.class);

    @DELETE
    @Transactional
    public Response deactivateUser(Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");

        log.infov("Received delete request for username: {0}", username);

        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            log.warn("Delete request missing username or password");
            Map<String, String> err = new HashMap<>();
            err.put("message", "username and password are required");
            return Response.status(Response.Status.BAD_REQUEST).entity(err).build();
        }

        User user = User.find("username", username).firstResult();

        if (user == null) {
            log.warnv("Delete attempt failed — user not found: {0}", username);
            Map<String, String> err = new HashMap<>();
            err.put("message", "User not found");
            return Response.status(Response.Status.NOT_FOUND).entity(err).build();
        }

        if (!user.active) {
            log.warnv("Delete attempt on inactive user: {0}", username);
            Map<String, String> err = new HashMap<>();
            err.put("message", "User is already inactive");
            return Response.status(Response.Status.CONFLICT).entity(err).build();
        }

        // Verify password
        if (!user.password.equals(password)) {
            log.errorv("Invalid password attempt for user: {0}", username);
            Map<String, String> err = new HashMap<>();
            err.put("message", "Invalid password");
            return Response.status(Response.Status.UNAUTHORIZED).entity(err).build();
        }

        try {
            user.active = false;
            user.persist();
            log.infov("User deactivated successfully: {0}", username);
        } catch (Exception e) {
            log.errorv(e, "Database error while deactivating user: {0}", username);
            Map<String, String> err = new HashMap<>();
            err.put("message", "Database error while deactivating user");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(err).build();
        }

        Map<String, String> response = new HashMap<>();
        response.put("message", "User deleted successfully");
        response.put("username", username);

        return Response.ok(response).build();
    }
}
