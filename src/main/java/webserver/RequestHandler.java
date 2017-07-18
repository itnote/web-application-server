package webserver;

import java.io.*;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

            while(true) {
                int count = in.read();
                if(count == -1 || count == '\n') {
                    System.out.println(request.toString());
                    break;
                }
                request.append((char) count);
            }

            String[] requestParameter = request.toString().split(" ");
            InputStream fis = new FileInputStream("./webapp" + requestParameter[1]);
            DataOutputStream dos = new DataOutputStream(out);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            byte[] b = new byte[1024 * 8];
            int readcount = 0;

            while((readcount = fis.read(b)) != -1) {
                baos.write(b, 0, readcount);
            }

            byte[] response = baos.toByteArray();
            response200Header(dos, response.length);
            responseBody(dos, response);
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
}
