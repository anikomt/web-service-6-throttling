package ru.ifmo.web.service;

import com.sun.jersey.spi.container.ResourceFilters;
import com.sun.jersey.multipart.FormDataParam;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import ru.ifmo.web.database.dao.UserDAO;
import ru.ifmo.web.database.dto.UserDTO;
import ru.ifmo.web.database.entity.User;
import ru.ifmo.web.filter.ThrottlingFilter;
import ru.ifmo.web.standalone.App;
import ru.ifmo.web.util.AuthenticationException;
import ru.ifmo.web.util.ForbiddenException;
import ru.ifmo.web.util.UserServiceException;

import javax.jws.WebMethod;
import javax.sql.DataSource;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

@Data
@Slf4j
@ResourceFilters(ThrottlingFilter.class)
@Path("/users")
public class UsersService {
    private UserDAO userDAO;
    private final static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    public UsersService() {
        InputStream dsPropsStream = App.class.getClassLoader().getResourceAsStream("application.properties");
        Properties dsProps = new Properties();
        try {
            dsProps.load(dsPropsStream);

        } catch (IOException e) {
            e.printStackTrace();
        }
        HikariConfig hikariConfig = new HikariConfig(dsProps);
        DataSource ds = new HikariDataSource(hikariConfig);
        userDAO = new UserDAO(ds);
    }

    @GET
    @Path("/")
    @Produces({MediaType.APPLICATION_JSON})
    public List<User> findAll() throws SQLException {
        return userDAO.findAll();
    }

    @GET
    @Path("/filter")
    @Produces({MediaType.APPLICATION_JSON})
    public List<User> findWithFilters(@QueryParam("id") Long id, @QueryParam("login") String login,
                                      @QueryParam("password") String password, @QueryParam("email") String email,
                                      @QueryParam("gender") Boolean gender, @QueryParam("registerDate") String registerDate)
            throws UserServiceException {
        Date date;
        if (registerDate != null) {
            try {
                date = sdf.parse(registerDate);
            } catch (ParseException e) {
                throw new UserServiceException("Couldn't parse date");
            }
        } else {
            date =null;
        }
        try {
            return userDAO.findWithFilters(id, login, password, email, gender, date);
        } catch (SQLException e) {
            throw new UserServiceException("SQL exception: " + e.getMessage() + ". State: " + e.getSQLState());
        }
    }

    @DELETE
    @Path("/{id}")
    @WebMethod
    @Produces(MediaType.TEXT_PLAIN)
    public String delete(@PathParam("id") Long id, @HeaderParam("authorization") String authHeader)
            throws UserServiceException, AuthenticationException, ForbiddenException {
        checkAuthenticated(authHeader);
        try {
            if (id == null ) {
                throw new UserServiceException("Id can't be null");
            }
            int delete = userDAO.delete(id);
            if (delete <= 0) {
                throw new UserServiceException(String
                        .format("Can't delete User. User with specified id: %s not found ", id)
                );
            }
            return String.valueOf(delete);
        } catch (SQLException e) {
            return  "SQL exception: " + e.getMessage() + ". State: " + e.getSQLState();
        }
    }

    @POST
    @WebMethod
    @Produces(MediaType.TEXT_PLAIN)
    public String insert(UserDTO userDTO, @HeaderParam("authorization") String authHeader)
            throws UserServiceException, AuthenticationException, ForbiddenException  {
        checkAuthenticated(authHeader);
        try {
            Date parse = new SimpleDateFormat("yyyy-MM-dd").parse(userDTO.getRegisterDate());
            return String.valueOf(userDAO.insert(userDTO.getLogin(), userDTO.getPassword(),
                    userDTO.getEmail(), userDTO.getGender(), parse)
            );
        } catch (SQLException e) {
            throw new UserServiceException("SQL exception: " + e.getMessage() + ". State: " + e.getSQLState());
        } catch (ParseException e) {
            e.printStackTrace();
            throw new UserServiceException("Couldn't parse date");
        }
    }

    @PUT
    @Path("/{id}")
    @Produces(MediaType.TEXT_PLAIN)
    public String update(@PathParam("id") Long id, @HeaderParam("authorization") String authHeader, UserDTO userDTO)
            throws UserServiceException, AuthenticationException, ForbiddenException {
        int update = 0;
        checkAuthenticated(authHeader);
        try {
            Date parse = null;
            if (userDTO.getRegisterDate() != null) {
                parse = sdf.parse(userDTO.getRegisterDate());
            }
            update = userDAO.update(id, userDTO.getLogin(), userDTO.getPassword(),
                    userDTO.getEmail(), userDTO.getGender(), parse);
            if (update <= 0) {
                throw new UserServiceException(String
                        .format("Can't update User. User with specified id: %s not found ", id)
                );
            }
        } catch (SQLException e) {
            throw new UserServiceException("SQL exception: " + e.getMessage() + ". State: " + e.getSQLState());
        }  catch (ParseException e) {
            throw new UserServiceException("Couldn't parse date");
        }
        return String.valueOf(update);
    }

    @POST
    @Path("/{id}/upload")
    @Consumes({MediaType.MULTIPART_FORM_DATA})
    @Produces(MediaType.TEXT_PLAIN)
    public String uploadFile(
            @PathParam("id") Long id,
            @FormDataParam("file") InputStream fileInputStream,
            @HeaderParam("authorization") String authString)
            throws AuthenticationException, ForbiddenException, SQLException {

        checkAuthenticated(authString);

        List<User> withFilters = userDAO.findWithFilters(id, null, null, null, null, null);
        User user = withFilters.get(0);
        String fileName = null;
        try {
            String FILE_TRASH_BASE = "/tmp/";

            File directory = new File(FILE_TRASH_BASE + user.getLogin());
            if (! directory.exists()) {
                directory.mkdir();
            }

            fileName = new SimpleDateFormat("ddMMyy-hhmmss.SSS").format(new Date());
            File file = new File(FILE_TRASH_BASE + user.getLogin() + "/" +
                    fileName);

            Files.copy(fileInputStream, file.toPath());
        } catch (IOException e) {
            Logger.getLogger(UserDAO.class.getName()).log(Level.SEVERE, null, e);
            return null;
        }
        return fileName;
    }

    private void checkAuthenticated(String authString) throws AuthenticationException, ForbiddenException {
        if (authString == null || authString.equals("")) {
            throw new AuthenticationException("Authorization required for CRUD operations");
        }

        try {
            String[] authParts = authString.split("\\s+");
            String authInfo = authParts[1];

            String decodedString = new String(Base64.getDecoder().decode(authInfo));

            authParts = decodedString.split(":");
            User user = loadUserFromDB(authParts[0], authParts[1]);
            if (user == null ) {
                throw new ForbiddenException("Wrong login/password");
            }
        } catch (IllegalArgumentException e) {
            throw new ForbiddenException("Wrong login/password");
        }
    }

    private User loadUserFromDB(final String username, final String password) {
        User user = null;
        try {
            user = userDAO.findByCredentials(username, password);
        } catch (SQLException e) {
            e.printStackTrace();

        }
        return user;
    }
}
