package ru.ifmo.web.standalone;

import com.sun.jersey.api.container.grizzly2.GrizzlyServerFactory;

import com.sun.jersey.api.core.PackagesResourceConfig;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.grizzly.http.server.HttpServer;
import ru.ifmo.web.service.UsersService;
import ru.ifmo.web.util.ThrottlingExceptionMapper;

import javax.xml.datatype.DatatypeConfigurationException;
import java.io.IOException;
import java.sql.SQLException;

@Slf4j
public class App {
    public static void main(String... args) throws DatatypeConfigurationException, SQLException, IOException {
        String url = "http://0.0.0.0:8080/";
        PackagesResourceConfig config = new PackagesResourceConfig(UsersService.class.getPackage().getName(),
                ThrottlingExceptionMapper.class.getPackage().getName()
        );
        log.info("Creating server");
        HttpServer server = GrizzlyServerFactory.createHttpServer(url, config);
        log.info("Starting server");
        server.start();
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        log.info("Application started");
        while (true) {
        }
    }

}
