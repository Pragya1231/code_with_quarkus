package org.project;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

@Path("/login")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class LoginResource {

    @POST
    public Response login(Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");

        // Simple hardcoded check
        if ("admin".equals(username) && "1234".equals(password)) {
            Map<String, String> result = new HashMap<>();
            result.put("message", "Login successful!");
            return Response.ok(result).build();
        } else {
            Map<String, String> result = new HashMap<>();
            result.put("message", "Invalid credentials");
            return Response.status(Response.Status.UNAUTHORIZED).entity(result).build();
        }
    }
}

