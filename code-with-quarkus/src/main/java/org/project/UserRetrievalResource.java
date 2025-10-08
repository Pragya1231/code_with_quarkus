package org.project;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.jboss.logging.Logger;


@Path("/users/retrieve")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserRetrievalResource {

    private static final Logger log = Logger.getLogger(UserRetrievalResource.class);
    private static final String SECRET_KEY = "MySuperSecretKey";

    private static SecretKeySpec getKeySpec() {
        return new SecretKeySpec(SECRET_KEY.getBytes(), "AES");
    }

    @POST
    public Response retrieveUsername(Map<String, String> request) {
        log.info("UserName Retrieval is called with request : {}"+request);
        String password = request.get("password");
        Map<String, String> response = new HashMap<>();

        if (password == null || password.isBlank()) {

            response.put("message", "Password is required");
            return Response.status(Response.Status.BAD_REQUEST).entity(response).build();
        }

        try {
            // Decrypt password to get username
            String username = EncryptionUtil.decrypt(password);

            // Check if this user exists in DB (optional)
            User user = User.find("username", username).firstResult();
            if (user == null) {
                response.put("message", "No user found with this password");
                return Response.status(Response.Status.NOT_FOUND).entity(response).build();
            }

            response.put("username", username);
            response.put("message", "Username retrieved successfully");
            return Response.ok(response).build();

        } catch (Exception e) {
            e.printStackTrace();
            response.put("message", "Invalid password or decryption error");
            return Response.status(Response.Status.BAD_REQUEST).entity(response).build();
        }


    }
}
