package ru.ifmo.web.client.util;

import com.sun.jersey.multipart.FormDataMultiPart;
import com.sun.jersey.multipart.MultiPart;
import com.sun.jersey.multipart.file.FileDataBodyPart;
import lombok.extern.slf4j.Slf4j;


import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;
import ru.ifmo.web.client.Exception;
import ru.ifmo.web.database.dto.UserDTO;
import ru.ifmo.web.database.entity.User;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.List;
import javax.ws.rs.core.MediaType;


@Slf4j
public class UsersResourceIntegration {
    private final static String BASE_URL = "http://localhost:8080/users/";
    private final static String FIND_ALL_URL = BASE_URL;
    private final static String FILTER_URL = BASE_URL + "filter";
    private final static String UPDATE_URL = BASE_URL + "%d";
    private final static String DELETE_URL = BASE_URL + "%d";
    private final static String UPLOAD_URL = BASE_URL + "%d/upload"  ;

    private final static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    private String login = "bleizard";
    private String password = "1234";

    public List<User> findAll() {
        Client client = Client.create();
        WebResource webResource = client.resource(FIND_ALL_URL);
        ClientResponse response =
                webResource.accept(MediaType.APPLICATION_JSON_TYPE).get(ClientResponse.class);
        if (response.getStatus() != ClientResponse.Status.OK.getStatusCode()) {
            throw new IllegalStateException("Request failed. HTTP code: " + response.getStatus() + " "
                    + response.getEntity(new GenericType<String>() {}));
        }
        GenericType<List<User>> type = new GenericType<List<User>>() {
        };
        return response.getEntity(type);
    }

    public List<User> findWithFilters(Long id, String login, String password, String email, Boolean gender, String registerDate) {
        Client client = Client.create();
        WebResource webResource = client.resource(FILTER_URL);
        if (id != null) {
            webResource = webResource.queryParam("id", id + "");
        }
        if (login != null) {
            webResource = webResource.queryParam("login", login);
        }
        if (password != null) {
            webResource = webResource.queryParam("password", password);
        }
        if (email != null) {
            webResource = webResource.queryParam("email", email);
        }
        if (gender != null) {
            webResource = webResource.queryParam("gender", gender.toString());
        }
        if (registerDate != null) {
            webResource = webResource.queryParam("registerDate", registerDate);
        }
        ClientResponse response = webResource.accept(MediaType.APPLICATION_JSON_TYPE).get(ClientResponse.class);
        if (response.getStatus() != ClientResponse.Status.OK.getStatusCode()) {
            throw new IllegalStateException("Request failed. HTTP code: " + response.getStatus() + " "
                    + response.getEntity(new GenericType<String>() {}));
        }
        GenericType<List<User>> type = new GenericType<List<User>>() {
        };
        return response.getEntity(type);
    }

    public Long insert(UserDTO userDTO) {
        Client client = Client.create();
        WebResource webResource = client.resource(BASE_URL);
        ClientResponse response = webResource.accept(MediaType.TEXT_PLAIN)
                .header("Authorization", getAuthHeader())
                .entity(userDTO)
                .post(ClientResponse.class);
        if (response.getStatus() != ClientResponse.Status.OK.getStatusCode()) {
            throw new IllegalStateException("Request failed. HTTP code: " + response.getStatus() + " "
                    + response.getEntity(new GenericType<String>() {}));
        }
        GenericType<String> type = new GenericType<String>() {
        };
        return Long.parseLong(response.getEntity(type));
    }

    public int update(Long id, UserDTO userDTO) {
        Client client = Client.create();
        if (id == null) {
            return -1;
        }
        WebResource webResource = client.resource(String.format(UPDATE_URL, id));
        ClientResponse response = webResource
                .header("Authorization", getAuthHeader())
                .accept(MediaType.TEXT_PLAIN)
                .entity(userDTO)
                .put(ClientResponse.class);
        if (response.getStatus() != ClientResponse.Status.OK.getStatusCode()) {
            throw new IllegalStateException("Request failed. HTTP code: " + response.getStatus() + " "
                    + response.getEntity(new GenericType<String>() {}));
        }
        GenericType<String> type = new GenericType<String>() {
        };
        return Integer.parseInt(response.getEntity(type));
    }

    public int delete(Long id) {
        Client client = Client.create();
        if (id == null) {
            return -1;
        }
        WebResource webResource = client.resource(String.format(DELETE_URL, id) );
        ClientResponse response = webResource
                .header("Authorization", getAuthHeader())
                .accept(MediaType.TEXT_PLAIN)
                .delete(ClientResponse.class);
        if (response.getStatus() != ClientResponse.Status.OK.getStatusCode()) {
            throw new IllegalStateException("Request failed. HTTP code: " + response.getStatus() + " "
                    + response.getEntity(new GenericType<String>() {}));
        }
        GenericType<String> type = new GenericType<String>() {
        };
        return Integer.parseInt(response.getEntity(type));
    }

    public String uploadFile(Long id, File file) {
        Client client = Client.create();
        FileDataBodyPart filePart = new FileDataBodyPart("file", file);
        MultiPart multipartEntity = new FormDataMultiPart().bodyPart(filePart);

        WebResource webResource = client.resource(String.format(UPLOAD_URL, id));
        ClientResponse response = webResource
                .header("Authorization", getAuthHeader())
                .type(MediaType.MULTIPART_FORM_DATA_TYPE)
                .post(ClientResponse.class, multipartEntity);

        GenericType<String> type = new GenericType<String>() {};
        String entity = response.getEntity(type);
        return entity == null ? "Файл не добалвен" : entity;
    }

    private String getAuthHeader() {
        String authString = login + ":" + password;
        String authStringEnc = Base64.getEncoder().encodeToString(authString.getBytes());
        return "Basic " + authStringEnc;
    }
}