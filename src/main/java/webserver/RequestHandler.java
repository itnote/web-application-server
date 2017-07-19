package webserver;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import db.DataBase;
import model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.HttpRequestUtils;

public class RequestHandler extends Thread {
    private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);

    private Socket connection;

    private StringBuffer request = new StringBuffer();

    public RequestHandler(Socket connectionSocket) {
        this.connection = connectionSocket;
    }

    public void run() {
        log.debug("New Client Connect! Connected IP : {}, Port : {}", connection.getInetAddress(),
                connection.getPort());

        try (InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream()) {
            // TODO 사용자 요청에 대한 처리는 이 곳에 구현하면 된다.

            BufferedReader br = new BufferedReader(new InputStreamReader(in));

            String requestHeader = br.readLine();

            String[] requestParameter = requestHeader.split(" ");
            if(requestParameter[1].contains("?")) {
                String[] queryString = requestParameter[1].split("\\?");

                Map<String, String> query = HttpRequestUtils.parseQueryString(queryString[1]);

                if(DataBase.findUserById(query.get("userId")) == null && createUser(query)) {
                    log.debug(DataBase.findUserById(query.get("userId")).toString());
                } else {
                    log.debug("user 등록 실패거나 이미 아이디가 존재함");
                }
            } else {
                Path path = Paths.get("./webapp" + requestParameter[1]);
                byte[] body = Files.readAllBytes(path);
                DataOutputStream dos = new DataOutputStream(out);
                response200Header(dos, body.length);
                responseBody(dos, body);
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void response200Header(DataOutputStream dos, int lengthOfBodyContent) {
        try {
            dos.writeBytes("HTTP/1.1 200 OK \r\n");
            dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void responseBody(DataOutputStream dos, byte[] body) {
        try {
            dos.write(body, 0, body.length);
            dos.flush();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private boolean createUser(Map<String, String> query) {
        String userId = query.get("userId");
        String password = query.get("password");
        String name = query.get("name");
        String email = query.get("email");

        if(!userId.isEmpty() && !password.isEmpty() && !name.isEmpty() && !email.isEmpty()) {
            User user = new User(userId, password, name, email);
            DataBase.addUser(user);
            log.debug(String.valueOf(user.hashCode()));
            return true;
        } else {
            return false;
        }
    }
}
