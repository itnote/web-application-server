package webserver;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import db.DataBase;
import model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.HttpRequestUtils;
import util.IOUtils;
import static util.HttpRequestUtils.Pair;

public class RequestHandler extends Thread {
    private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);

    private Socket connection;

    public RequestHandler(Socket connectionSocket) {
        this.connection = connectionSocket;
    }

    public void run() {
        log.debug("New Client Connect! Connected IP : {}, Port : {}", connection.getInetAddress(),
                connection.getPort());

        try (InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream()) {
            // TODO 사용자 요청에 대한 처리는 이 곳에 구현하면 된다.

            BufferedReader br = new BufferedReader(new InputStreamReader(in));

            Map<String, String> headerList = new HashMap<>();

            String line = br.readLine();
            String[] requestParameter = line.split(" ");

            while(!"".equals(line)) {
                Pair pair = HttpRequestUtils.parseHeader(line);
                if(pair != null) {
                    headerList.put(pair.getKey(), pair.getValue());
                }
                line = br.readLine();
            }

            String requestBody = "";
            if(headerList.containsKey("Content-Length")) {
                requestBody = IOUtils.readData(br, Integer.parseInt(headerList.get("Content-Length")));
            }
            log.debug(headerList.toString());

            DataOutputStream dos = new DataOutputStream(out);
            String param = requestParameter[1];
            if(!requestBody.equals("")) {
                Map<String, String> query = HttpRequestUtils.parseQueryString(requestBody);
                if(param.contains("create")) {
                    if (DataBase.findUserById(query.get("userId")) == null && createUser(query)) {
                        log.debug(DataBase.findUserById(query.get("userId")).toString());
                    } else {
                        log.debug("user 등록 실패거나 이미 아이디가 존재함");
                    }
                    response302(dos, "/index.html");
                } else if(param.contains("login")) {
                    boolean isLogin = loginUser(query);
                    if(isLogin) {
                        response302HeaderWithCookie(dos, "/index.html", "logined=true");
                    } else {
                        response302HeaderWithCookie(dos, "/user/login_failed.html", "logined=false");
                    }
                }
            } else {
                if(param.contains("list")) {
                    boolean isLogin = checkLogin(headerList.get("Cookie"));
                    if(isLogin) {
                        String contentOfListPage = createUserListPage();
                        byte[] body = contentOfListPage.getBytes();
                        response200Header(dos, body.length);
                        responseBody(dos, body);
                    } else {
                        response302(dos, "/user/login.html");
                    }
                } else {
                    log.debug(requestParameter[1]);
                    response200(dos, requestParameter[1]);
                }
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

    private void response200(DataOutputStream dos, String stringOfPath) {
        try {
            Path path = Paths.get("./webapp" + stringOfPath);
            byte[] body = Files.readAllBytes(path);
            response200Header(dos, body.length);
            responseBody(dos, body);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void response302(DataOutputStream dos, String urlOfLocation) {
        try {
            dos.writeBytes("HTTP/1.1 302 Found \r\n");
            dos.writeBytes("Location: " + urlOfLocation + "\r\n");
            dos.writeBytes("\r\n");
            dos.flush();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void response302HeaderWithCookie(DataOutputStream dos, String urlOfLocation, String valueOfCookie) {
        try {
            dos.writeBytes("HTTP/1.1 302 Found \r\n");
            dos.writeBytes("Location: " + urlOfLocation + "\r\n");
            dos.writeBytes("Set-Cookie: " + valueOfCookie + "\r\n");
            dos.writeBytes("\r\n");
            dos.flush();
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

        if(userId != null && password != null && name != null && email != null) {
            User user = new User(userId, password, name, email);
            DataBase.addUser(user);
            log.debug(String.valueOf(user.hashCode()));
            return true;
        } else {
            return false;
        }
    }

    private boolean loginUser(Map<String, String> query) {
        String userId = query.get("userId");
        String password = query.get("password");
        User user = DataBase.findUserById(userId);
        if(user != null && user.getPassword().equals(password)) {
            return true;
        }

        return false;
    }

    private boolean checkLogin(String valueOfCookie) {
        Map<String, String> cookie = HttpRequestUtils.parseCookies(valueOfCookie);
        return Boolean.parseBoolean(cookie.get("logined"));
    }

    private String createUserListPage() {
        StringBuilder sb = new StringBuilder();
        int i = 1;
        sb.append("<!DOCTYPE html>\n" +
                "<html lang=\"kr\">\n +" +
                "<head>\n" +
                "    <meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\">\n" +
                "    <meta charset=\"utf-8\">\n" +
                "    <title>SLiPP Java Web Programming</title>\n" +
                "</head>\n" +
                "<body>\n" +
                "<table class=\"table table-hover\">\n" +
                "              <thead>\n" +
                "                <tr>\n" +
                "                    <th>#</th> <th>사용자 아이디</th> <th>이름</th> <th>이메일</th><th></th>\n" +
                "                </tr>\n" +
                "              </thead>\n" +
                "              <tbody>");

        Iterator<User> iterator = DataBase.findAll().iterator();
        while(iterator.hasNext()) {
            User user = iterator.next();
            sb.append("<tr>\n" +
                    "<th scope='row'>" + i + "</th> ");
            sb.append("<td>" + user.getUserId() + "</td> ");
            sb.append("<td>" + user.getName() + "</td> ");
            sb.append("<td>" + user.getEmail() + "</td>\n");
            sb.append("</tr>");
        }
        sb.append("</tbody>\n" +
                "</table>\n" +
                "<script src=\"../js/jquery-2.2.0.min.js\"></script>\n" +
                "<script src=\"../js/bootstrap.min.js\"></script>\n" +
                "<script src=\"../js/scripts.js\"></script>\n" +
                "\t</body>\n" +
                "</html>");

        return sb.toString();
    }
}
