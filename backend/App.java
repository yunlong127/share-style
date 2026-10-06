import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.InetSocketAddress;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

public class App {
    
    static boolean useDatabase = false;
    
    public static void main(String[] args) throws Exception {
        initDatabase();
        
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/api", new ApiHandler());
        server.createContext("/uploads", new StaticFileHandler());
        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();
        System.out.println("Server started on port 8080");
        System.out.println("Database mode: " + (useDatabase ? "MySQL" : "Memory"));
    }
    
    private static void initDatabase() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection connection = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/cs_login?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true",
                "root",
                "123"
            );
            useDatabase = true;
            System.out.println("Database connected successfully");
            connection.close();
        } catch (Exception e) {
            System.out.println("Database connection failed: " + e.getMessage());
            useDatabase = false;
        }
    }
    
    static class ApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();
            
            System.out.println("Request received: " + method + " " + path);
            
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
            
            if (method.equals("OPTIONS")) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            
            String response = "";
            int statusCode = 200;
            
            try {
                if (path.startsWith("/api/posts")) {
                    System.out.println("Handling /api/posts request, method: " + method);
                    if (method.equals("GET")) {
                        if (path.equals("/api/posts")) {
                            response = getPosts(exchange.getRequestURI().getQuery());
                        } else if (path.matches("/api/posts/\\d+")) {
                            long id = Long.parseLong(path.split("/")[3]);
                            response = getPost(id);
                        } else if (path.matches("/api/posts/type/\\w+")) {
                            String type = path.split("/")[4];
                            response = getPostsByType(type);
                        } else if (path.matches("/api/posts/user/\\d+")) {
                            long userId = Long.parseLong(path.split("/")[4]);
                            response = getPostsByUser(userId);
                        }
                    } else if (method.equals("POST")) {
                        System.out.println("Handling POST /api/posts request");
                        if (path.matches("/api/posts/\\d+/like")) {
                            long id = Long.parseLong(path.split("/")[3]);
                            response = likePost(id);
                        } else {
                            System.out.println("Calling createPost method");
                            response = createPost(exchange);
                        }
                    } else if (method.equals("PUT")) {
                        if (path.matches("/api/posts/\\d+")) {
                            long id = Long.parseLong(path.split("/")[3]);
                            response = updatePost(id, exchange);
                        }
                    } else if (method.equals("DELETE")) {
                        if (path.matches("/api/posts/\\d+")) {
                            long id = Long.parseLong(path.split("/")[3]);
                            response = deletePost(id);
                        }
                    }
                } else if (path.startsWith("/api/users")) {
                    if (method.equals("POST")) {
                        if (path.equals("/api/users/login")) {
                            response = login(exchange);
                        } else if (path.equals("/api/users/register")) {
                            response = register(exchange);
                        }
                    } else if (method.equals("GET")) {
                        if (path.matches("/api/users/\\d+")) {
                            long id = Long.parseLong(path.split("/")[3]);
                            response = getUser(id);
                        }
                    }
                } else if (path.startsWith("/api/comments")) {
                    if (method.equals("GET")) {
                        if (path.matches("/api/comments/post/\\d+")) {
                            long postId = Long.parseLong(path.split("/")[4]);
                            response = getComments(postId);
                        }
                    }
                } else if (path.startsWith("/api/files")) {
                    if (method.equals("POST")) {
                        if (path.equals("/api/files/upload")) {
                            response = uploadFile(exchange);
                        }
                    }
                } else if (path.startsWith("/uploads/")) {
                    // 处理静态文件请求
                    serveStaticFile(exchange, path);
                    return;
                }
            } catch (Exception e) {
                System.out.println("Exception in handle method: " + e.getMessage());
                e.printStackTrace();
                statusCode = 500;
                response = "{\"error\":\"Internal Server Error: " + e.getMessage() + "\"}";
            }
            
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(statusCode, response.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
        
        private String getPosts(String query) {
            if (App.useDatabase) {
                try {
                    Class.forName("com.mysql.cj.jdbc.Driver");
                    Connection connection = DriverManager.getConnection(
                        "jdbc:mysql://localhost:3306/cs_login?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true",
                        "root",
                        "123"
                    );
                    
                    // 解析查询参数
                    String pageStr = extractQueryValue(query, "page");
                    String sizeStr = extractQueryValue(query, "size");
                    int page = (pageStr != null && !pageStr.isEmpty()) ? Integer.parseInt(pageStr) : 0;
                    int size = (sizeStr != null && !sizeStr.isEmpty()) ? Integer.parseInt(sizeStr) : 10;
                    int offset = page * size;
                    
                    // 查询帖子总数
                    String countQuery = "SELECT COUNT(*) FROM posts";
                    Statement countStmt = connection.createStatement();
                    ResultSet countRs = countStmt.executeQuery(countQuery);
                    countRs.next();
                    long totalElements = countRs.getLong(1);
                    
                    // 查询帖子列表
                    String postsQuery = "SELECT p.id, p.title, p.content, p.content_type, p.cover_image, p.user_id, p.view_count, p.like_count, p.comment_count, p.created_at, u.username, u.nickname, u.avatar FROM posts p JOIN users u ON p.user_id = u.id ORDER BY p.created_at DESC LIMIT ? OFFSET ?";
                    PreparedStatement stmt = connection.prepareStatement(postsQuery);
                    stmt.setInt(1, size);
                    stmt.setInt(2, offset);
                    ResultSet rs = stmt.executeQuery();
                    
                    StringBuilder content = new StringBuilder();
                    content.append("[");
                    boolean first = true;
                    
                    while (rs.next()) {
                        if (!first) content.append(",");
                        content.append("{");
                        content.append("\"id\":").append(rs.getLong("id")).append(",");
                        content.append("\"title\":\"").append(rs.getString("title")).append("\",");
                        content.append("\"content\":\"").append(rs.getString("content")).append("\",");
                        content.append("\"contentType\":\"").append(rs.getString("content_type")).append("\",");
                        String coverImage = rs.getString("cover_image");
                        if (coverImage != null && !coverImage.isEmpty()) {
                            content.append("\"coverImage\":\"").append(coverImage).append("\",");
                        }
                        content.append("\"user\":{");
                        content.append("\"id\":").append(rs.getLong("user_id")).append(",");
                        content.append("\"username\":\"").append(rs.getString("username")).append("\",");
                        content.append("\"nickname\":\"").append(rs.getString("nickname")).append("\",");
                        String avatar = rs.getString("avatar");
                        content.append("\"avatar\":\"").append(avatar != null ? avatar : "/uploads/images/user/user.png").append("\"");
                        content.append("},");
                        content.append("\"viewCount\":").append(rs.getInt("view_count")).append(",");
                        content.append("\"likeCount\":").append(rs.getInt("like_count")).append(",");
                        content.append("\"commentCount\":").append(rs.getInt("comment_count")).append(",");
                        content.append("\"createdAt\":\"").append(rs.getTimestamp("created_at")).append("\"");
                        content.append("}");
                        first = false;
                    }
                    content.append("]");
                    
                    long totalPages = (totalElements + size - 1) / size;
                    boolean last = (page + 1) >= totalPages;
                    
                    connection.close();
                    return "{\"content\":" + content.toString() + ",\"totalElements\":" + totalElements + ",\"totalPages\":" + totalPages + ",\"number\":" + page + ",\"last\":" + last + "}";
                } catch (Exception e) {
                    e.printStackTrace();
                    return "{\"content\":[],\"totalElements\":0,\"totalPages\":0,\"number\":0,\"last\":true}";
                }
            } else {
                return "{\"content\":[],\"totalElements\":0,\"totalPages\":0,\"number\":0,\"last\":true}";
            }
        }
        
        private String getPost(long id) {
            if (App.useDatabase) {
                try {
                    Class.forName("com.mysql.cj.jdbc.Driver");
                    Connection connection = DriverManager.getConnection(
                        "jdbc:mysql://localhost:3306/cs_login?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true",
                        "root",
                        "123"
                    );
                    
                    // 查询帖子详情
                    String postQuery = "SELECT p.id, p.title, p.content, p.content_type, p.cover_image, p.user_id, p.view_count, p.like_count, p.comment_count, p.created_at, u.username, u.nickname, u.avatar FROM posts p JOIN users u ON p.user_id = u.id WHERE p.id = ?";
                    PreparedStatement stmt = connection.prepareStatement(postQuery);
                    stmt.setLong(1, id);
                    ResultSet rs = stmt.executeQuery();
                    
                    if (rs.next()) {
                        // 先获取所有需要的数据
                        long postId = rs.getLong("id");
                        String title = rs.getString("title");
                        String content = rs.getString("content");
                        String contentType = rs.getString("content_type");
                        String coverImage = rs.getString("cover_image");
                        long userId = rs.getLong("user_id");
                        String username = rs.getString("username");
                        String nickname = rs.getString("nickname");
                        String postAvatar = rs.getString("avatar");
                        int viewCount = rs.getInt("view_count");
                        int likeCount = rs.getInt("like_count");
                        int commentCount = rs.getInt("comment_count");
                        java.sql.Timestamp createdAt = rs.getTimestamp("created_at");
                        
                        // 增加浏览数
                        String updateViewCountQuery = "UPDATE posts SET view_count = view_count + 1 WHERE id = ?";
                        PreparedStatement updateStmt = connection.prepareStatement(updateViewCountQuery);
                        updateStmt.setLong(1, postId);
                        updateStmt.executeUpdate();
                        
                        // 关闭连接
                        connection.close();
                        
                        // 构建响应
                        StringBuilder post = new StringBuilder();
                        post.append("{");
                        post.append("\"id\":").append(postId).append(",");
                        post.append("\"title\":\"").append(title).append("\",");
                        post.append("\"content\":\"").append(content).append("\",");
                        post.append("\"contentType\":\"").append(contentType).append("\",");
                        if (coverImage != null && !coverImage.isEmpty()) {
                            post.append("\"coverImage\":\"").append(coverImage).append("\",");
                        }
                        post.append("\"user\":{");
                        post.append("\"id\":").append(userId).append(",");
                        post.append("\"username\":\"").append(username).append("\",");
                        post.append("\"nickname\":\"").append(nickname).append("\",");
                        post.append("\"avatar\":\"").append(postAvatar != null ? postAvatar : "/uploads/images/user/user.png").append("\"");
                        post.append("},");
                        post.append("\"viewCount\":").append(viewCount + 1).append(",");
                        post.append("\"likeCount\":").append(likeCount).append(",");
                        post.append("\"commentCount\":").append(commentCount).append(",");
                        post.append("\"createdAt\":\"").append(createdAt).append("\"");
                        post.append("}");
                        
                        return "{\"post\": " + post.toString() + ", \"displayContent\": \"" + content + "\"}";
                    } else {
                        connection.close();
                        return "{\"error\":\"Post not found\"}";
                    }
                    
                } catch (Exception e) {
                    e.printStackTrace();
                    return "{\"error\":\"Database error\"}";
                }
            } else {
                return "{\"post\": {\"id\": " + id + ", \"title\": \"测试文章\", \"content\": \"这是一篇测试文章\", \"contentType\": \"article\", \"user\": {\"id\": 1, \"username\": \"test\", \"nickname\": \"测试用户\"}}, \"displayContent\": \"这是一篇测试文章\"}";
            }
        }
        
        private String getPostsByType(String type) {
            if (App.useDatabase) {
                try {
                    Class.forName("com.mysql.cj.jdbc.Driver");
                    Connection connection = DriverManager.getConnection(
                        "jdbc:mysql://localhost:3306/cs_login?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true",
                        "root",
                        "123"
                    );
                    
                    // 解析查询参数
                    int page = 0;
                    int size = 10;
                    int offset = page * size;
                    
                    // 查询帖子总数
                    String countQuery = "SELECT COUNT(*) FROM posts WHERE content_type = ?";
                    PreparedStatement countStmt = connection.prepareStatement(countQuery);
                    countStmt.setString(1, type);
                    ResultSet countRs = countStmt.executeQuery();
                    countRs.next();
                    long totalElements = countRs.getLong(1);
                    
                    // 查询帖子列表
                    String postsQuery = "SELECT p.id, p.title, p.content, p.content_type, p.cover_image, p.user_id, p.view_count, p.like_count, p.comment_count, p.created_at, u.username, u.nickname, u.avatar FROM posts p JOIN users u ON p.user_id = u.id WHERE p.content_type = ? ORDER BY p.created_at DESC LIMIT ? OFFSET ?";
                    PreparedStatement stmt = connection.prepareStatement(postsQuery);
                    stmt.setString(1, type);
                    stmt.setInt(2, size);
                    stmt.setInt(3, offset);
                    ResultSet rs = stmt.executeQuery();
                    
                    StringBuilder content = new StringBuilder();
                    content.append("[");
                    boolean first = true;
                    
                    while (rs.next()) {
                        if (!first) content.append(",");
                        content.append("{");
                        content.append("\"id\":").append(rs.getLong("id")).append(",");
                        content.append("\"title\":\"").append(rs.getString("title")).append("\",");
                        content.append("\"content\":\"").append(rs.getString("content")).append("\",");
                        content.append("\"contentType\":\"").append(rs.getString("content_type")).append("\",");
                        String coverImage = rs.getString("cover_image");
                        if (coverImage != null && !coverImage.isEmpty()) {
                            content.append("\"coverImage\":\"").append(coverImage).append("\",");
                        }
                        content.append("\"user\":{");
                        content.append("\"id\":").append(rs.getLong("user_id")).append(",");
                        content.append("\"username\":\"").append(rs.getString("username")).append("\",");
                        content.append("\"nickname\":\"").append(rs.getString("nickname")).append("\",");
                        String avatar = rs.getString("avatar");
                        content.append("\"avatar\":\"").append(avatar != null ? avatar : "/uploads/images/user/user.png").append("\"");
                        content.append("},");
                        content.append("\"viewCount\":").append(rs.getInt("view_count")).append(",");
                        content.append("\"likeCount\":").append(rs.getInt("like_count")).append(",");
                        content.append("\"commentCount\":").append(rs.getInt("comment_count")).append(",");
                        content.append("\"createdAt\":\"").append(rs.getTimestamp("created_at")).append("\"");
                        content.append("}");
                        first = false;
                    }
                    content.append("]");
                    
                    long totalPages = (totalElements + size - 1) / size;
                    boolean last = (page + 1) >= totalPages;
                    
                    connection.close();
                    return "{\"content\":" + content.toString() + ",\"totalElements\":" + totalElements + ",\"totalPages\":" + totalPages + ",\"number\":" + page + ",\"last\":" + last + "}";
                    
                } catch (Exception e) {
                    e.printStackTrace();
                    return "{\"content\":[],\"totalElements\":0,\"totalPages\":0,\"number\":0,\"last\":true}";
                }
            } else {
                return "{\"content\":[],\"totalElements\":0,\"totalPages\":0,\"number\":0,\"last\":true}";
            }
        }
        
        private String getPostsByUser(long userId) {
            if (App.useDatabase) {
                try {
                    Class.forName("com.mysql.cj.jdbc.Driver");
                    Connection connection = DriverManager.getConnection(
                        "jdbc:mysql://localhost:3306/cs_login?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true",
                        "root",
                        "123"
                    );
                    
                    // 解析查询参数
                    int page = 0;
                    int size = 10;
                    int offset = page * size;
                    
                    // 查询帖子总数
                    String countQuery = "SELECT COUNT(*) FROM posts WHERE user_id = ?";
                    PreparedStatement countStmt = connection.prepareStatement(countQuery);
                    countStmt.setLong(1, userId);
                    ResultSet countRs = countStmt.executeQuery();
                    countRs.next();
                    long totalElements = countRs.getLong(1);
                    
                    // 查询帖子列表
                    String postsQuery = "SELECT p.id, p.title, p.content, p.content_type, p.cover_image, p.user_id, p.view_count, p.like_count, p.comment_count, p.created_at, u.username, u.nickname, u.avatar FROM posts p JOIN users u ON p.user_id = u.id WHERE p.user_id = ? ORDER BY p.created_at DESC LIMIT ? OFFSET ?";
                    PreparedStatement stmt = connection.prepareStatement(postsQuery);
                    stmt.setLong(1, userId);
                    stmt.setInt(2, size);
                    stmt.setInt(3, offset);
                    ResultSet rs = stmt.executeQuery();
                    
                    StringBuilder content = new StringBuilder();
                    content.append("[");
                    boolean first = true;
                    
                    while (rs.next()) {
                        if (!first) content.append(",");
                        content.append("{");
                        content.append("\"id\":").append(rs.getLong("id")).append(",");
                        content.append("\"title\":\"").append(rs.getString("title")).append("\",");
                        content.append("\"content\":\"").append(rs.getString("content")).append("\",");
                        content.append("\"contentType\":\"").append(rs.getString("content_type")).append("\",");
                        String coverImage = rs.getString("cover_image");
                        if (coverImage != null && !coverImage.isEmpty()) {
                            content.append("\"coverImage\":\"").append(coverImage).append("\",");
                        }
                        content.append("\"user\":{");
                        content.append("\"id\":").append(rs.getLong("user_id")).append(",");
                        content.append("\"username\":\"").append(rs.getString("username")).append("\",");
                        content.append("\"nickname\":\"").append(rs.getString("nickname")).append("\",");
                        String avatar = rs.getString("avatar");
                        content.append("\"avatar\":\"").append(avatar != null ? avatar : "/uploads/images/user/user.png").append("\"");
                        content.append("},");
                        content.append("\"viewCount\":").append(rs.getInt("view_count")).append(",");
                        content.append("\"likeCount\":").append(rs.getInt("like_count")).append(",");
                        content.append("\"commentCount\":").append(rs.getInt("comment_count")).append(",");
                        content.append("\"createdAt\":\"").append(rs.getTimestamp("created_at")).append("\"");
                        content.append("}");
                        first = false;
                    }
                    content.append("]");
                    
                    long totalPages = (totalElements + size - 1) / size;
                    boolean last = (page + 1) >= totalPages;
                    
                    connection.close();
                    return "{\"content\":" + content.toString() + ",\"totalElements\":" + totalElements + ",\"totalPages\":" + totalPages + ",\"number\":" + page + ",\"last\":" + last + "}";
                    
                } catch (Exception e) {
                    e.printStackTrace();
                    return "{\"content\":[],\"totalElements\":0,\"totalPages\":0,\"number\":0,\"last\":true}";
                }
            } else {
                return "{\"content\":[],\"totalElements\":0,\"totalPages\":0,\"number\":0,\"last\":true}";
            }
        }
        
        private String createPost(HttpExchange exchange) throws IOException {
            System.out.println("createPost called, App.useDatabase = " + App.useDatabase);
            if (App.useDatabase) {
                try {
                    Class.forName("com.mysql.cj.jdbc.Driver");
                    Connection connection = DriverManager.getConnection(
                        "jdbc:mysql://localhost:3306/cs_login?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true",
                        "root",
                        "123"
                    );

                    // 读取请求体
                    String body = readBody(exchange);
                    System.out.println("Request body: " + body);
                    
                    String title = extractValue(body, "title");
                    String content = extractValue(body, "content");
                    String contentType = extractValue(body, "contentType");
                    String coverImage = extractValue(body, "coverImage");

                    System.out.println("Title: " + title);
                    System.out.println("Content: " + content);
                    System.out.println("ContentType: " + contentType);
                    System.out.println("CoverImage: " + coverImage);

                    // 从查询参数中获取用户ID
                    String query = exchange.getRequestURI().getQuery();
                    System.out.println("Query: " + query);
                    
                    String userIdStr = extractQueryValue(query, "userId");
                    System.out.println("UserIdStr: " + userIdStr);
                    
                    if (userIdStr == null || userIdStr.isEmpty()) {
                        connection.close();
                        return "{\"error\":\"User ID is required\"}";
                    }
                    
                    long userId = 0;
                    try {
                        userId = Long.parseLong(userIdStr);
                        System.out.println("UserId: " + userId);
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                        connection.close();
                        return "{\"error\":\"Invalid user ID\"}";
                    }

                    // 插入帖子
                    String insertQuery = "INSERT INTO posts (title, content, content_type, cover_image, user_id, view_count, like_count, comment_count, created_at) VALUES (?, ?, ?, ?, ?, 0, 0, 0, NOW())";
                    PreparedStatement stmt = connection.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS);
                    stmt.setString(1, title);
                    stmt.setString(2, content);
                    stmt.setString(3, contentType);
                    stmt.setString(4, coverImage);
                    stmt.setLong(5, userId);
                    
                    int rowsAffected = stmt.executeUpdate();
                    System.out.println("Rows affected: " + rowsAffected);

                    // 获取生成的ID
                    ResultSet generatedKeys = stmt.getGeneratedKeys();
                    long postId = 0;
                    if (generatedKeys.next()) {
                        postId = generatedKeys.getLong(1);
                        System.out.println("Generated post ID: " + postId);
                    }

                    // 获取用户信息
                    String userQuery = "SELECT username, nickname, avatar FROM users WHERE id = ?";
                    PreparedStatement userStmt = connection.prepareStatement(userQuery);
                    userStmt.setLong(1, userId);
                    ResultSet userRs = userStmt.executeQuery();

                    if (userRs.next()) {
                        String username = userRs.getString("username");
                        String nickname = userRs.getString("nickname");
                        String avatar = userRs.getString("avatar");

                        connection.close();

                        StringBuilder post = new StringBuilder();
                        post.append("{");
                        post.append("\"id\":").append(postId).append(",");
                        post.append("\"title\":\"").append(title).append("\",");
                        post.append("\"content\":\"").append(content).append("\",");
                        post.append("\"contentType\":\"").append(contentType).append("\",");
                        post.append("\"coverImage\":\"").append(coverImage).append("\",");
                        post.append("\"user\":{");
                        post.append("\"id\":").append(userId).append(",");
                        post.append("\"username\":\"").append(username).append("\",");
                        post.append("\"nickname\":\"").append(nickname).append("\",");
                        post.append("\"avatar\":\"").append(avatar != null ? avatar : "/uploads/images/user/user.png").append("\"");
                        post.append("},").append("\"viewCount\":0,").append("\"likeCount\":0,").append("\"commentCount\":0,");
                        post.append("\"createdAt\":\"").append(new java.sql.Timestamp(System.currentTimeMillis())).append("\"");
                        post.append("}");

                        System.out.println("Returning post: " + post.toString());
                        return post.toString();
                    } else {
                        connection.close();
                        return "{\"error\":\"User not found\"}";
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    return "{\"error\":\"Database error: " + e.getMessage() + "\"}";
                }
            } else {
                return "{\"id\": 1, \"title\": \"新文章\", \"content\": \"文章内容\", \"contentType\": \"article\", \"user\": {\"id\": 1, \"username\": \"test\", \"nickname\": \"测试用户\"}, \"viewCount\": 0, \"likeCount\": 0, \"commentCount\": 0, \"createdAt\": \"" + new java.util.Date().toString() + "\"}";
            }
        }

        private String updatePost(long id, HttpExchange exchange) throws IOException {
            if (App.useDatabase) {
                try {
                    Class.forName("com.mysql.cj.jdbc.Driver");
                    Connection connection = DriverManager.getConnection(
                        "jdbc:mysql://localhost:3306/cs_login?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true",
                        "root",
                        "123"
                    );

                    // 读取请求体
                    String body = readBody(exchange);
                    String title = extractValue(body, "title");
                    String content = extractValue(body, "content");
                    String contentType = extractValue(body, "contentType");
                    String coverImage = extractValue(body, "coverImage");

                    // 更新帖子
                    String updateQuery = "UPDATE posts SET title = ?, content = ?, content_type = ?, cover_image = ? WHERE id = ?";
                    PreparedStatement stmt = connection.prepareStatement(updateQuery);
                    stmt.setString(1, title);
                    stmt.setString(2, content);
                    stmt.setString(3, contentType);
                    stmt.setString(4, coverImage);
                    stmt.setLong(5, id);
                    stmt.executeUpdate();

                    // 获取更新后的帖子
                    String postQuery = "SELECT p.id, p.title, p.content, p.content_type, p.user_id, p.view_count, p.like_count, p.comment_count, p.created_at, u.username, u.nickname, u.avatar FROM posts p JOIN users u ON p.user_id = u.id WHERE p.id = ?";
                    PreparedStatement postStmt = connection.prepareStatement(postQuery);
                    postStmt.setLong(1, id);
                    ResultSet rs = postStmt.executeQuery();

                    if (rs.next()) {
                        long postId = rs.getLong("id");
                        String postTitle = rs.getString("title");
                        String postContent = rs.getString("content");
                        String postContentType = rs.getString("content_type");
                        long userId = rs.getLong("user_id");
                        String username = rs.getString("username");
                        String nickname = rs.getString("nickname");
                        String postAvatar = rs.getString("avatar");
                        int viewCount = rs.getInt("view_count");
                        int likeCount = rs.getInt("like_count");
                        int commentCount = rs.getInt("comment_count");
                        java.sql.Timestamp createdAt = rs.getTimestamp("created_at");

                        connection.close();

                        StringBuilder post = new StringBuilder();
                        post.append("{");
                        post.append("\"id\":").append(postId).append(",");
                        post.append("\"title\":\"").append(postTitle).append("\",");
                        post.append("\"content\":\"").append(postContent).append("\",");
                        post.append("\"contentType\":\"").append(postContentType).append("\",");
                        post.append("\"coverImage\":\"").append(coverImage).append("\",");
                        post.append("\"user\":{");
                        post.append("\"id\":").append(userId).append(",");
                        post.append("\"username\":\"").append(username).append("\",");
                        post.append("\"nickname\":\"").append(nickname).append("\",");
                        post.append("\"avatar\":\"").append(postAvatar != null ? postAvatar : "/uploads/images/user/user.png").append("\"");
                        post.append("},").append("\"viewCount\":").append(viewCount).append(",");
                        post.append("\"likeCount\":").append(likeCount).append(",");
                        post.append("\"commentCount\":").append(commentCount).append(",");
                        post.append("\"createdAt\":\"").append(createdAt).append("\"");
                        post.append("}");

                        return post.toString();
                    } else {
                        connection.close();
                        return "{\"error\":\"Post not found\"}";
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    return "{\"error\":\"Database error\"}";
                }
            } else {
                return "{\"id\": " + id + ", \"title\": \"更新后的文章\", \"content\": \"更新后的内容\", \"contentType\": \"article\", \"user\": {\"id\": 1, \"username\": \"test\", \"nickname\": \"测试用户\"}, \"viewCount\": 0, \"likeCount\": 0, \"commentCount\": 0, \"createdAt\": \"" + new java.util.Date().toString() + "\"}";
            }
        }

        private String deletePost(long id) {
            if (App.useDatabase) {
                try {
                    Class.forName("com.mysql.cj.jdbc.Driver");
                    Connection connection = DriverManager.getConnection(
                        "jdbc:mysql://localhost:3306/cs_login?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true",
                        "root",
                        "123"
                    );

                    // 删除帖子
                    String deleteQuery = "DELETE FROM posts WHERE id = ?";
                    PreparedStatement stmt = connection.prepareStatement(deleteQuery);
                    stmt.setLong(1, id);
                    int rowsAffected = stmt.executeUpdate();

                    connection.close();

                    if (rowsAffected > 0) {
                        return "{\"success\":true}";
                    } else {
                        return "{\"error\":\"Post not found\"}";
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    return "{\"error\":\"Database error\"}";
                }
            } else {
                return "{\"success\":true}";
            }
        }

        private String likePost(long id) {
            if (App.useDatabase) {
                try {
                    Class.forName("com.mysql.cj.jdbc.Driver");
                    Connection connection = DriverManager.getConnection(
                        "jdbc:mysql://localhost:3306/cs_login?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true",
                        "root",
                        "123"
                    );

                    // 增加点赞数
                    String updateQuery = "UPDATE posts SET like_count = like_count + 1 WHERE id = ?";
                    PreparedStatement stmt = connection.prepareStatement(updateQuery);
                    stmt.setLong(1, id);
                    stmt.executeUpdate();

                    connection.close();

                    return "{\"success\":true}";
                } catch (Exception e) {
                    e.printStackTrace();
                    return "{\"error\":\"Database error\"}";
                }
            } else {
                return "{\"success\":true}";
            }
        }
        
        private String login(HttpExchange exchange) throws IOException {
            String body = readBody(exchange);
            String username = extractValue(body, "username");
            String password = extractValue(body, "password");
            
            if (App.useDatabase) {
                try {
                    Class.forName("com.mysql.cj.jdbc.Driver");
                    Connection connection = DriverManager.getConnection(
                        "jdbc:mysql://localhost:3306/cs_login?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true",
                        "root",
                        "123"
                    );
                    
                    String query = "SELECT id, username, password, nickname, bio, avatar FROM users WHERE username = ?";
                    PreparedStatement stmt = connection.prepareStatement(query);
                    stmt.setString(1, username);
                    ResultSet rs = stmt.executeQuery();
                    
                    if (!rs.next()) {
                        connection.close();
                        return "{\"status\":\"not_registered\",\"message\":\"用户未注册\"}";
                    }
                    
                    String dbPassword = rs.getString("password");
                    if (!dbPassword.equals(password)) {
                        connection.close();
                        return "{\"status\":\"password_error\",\"message\":\"密码错误\"}";
                    }
                    
                    Map<String, Object> user = new HashMap<>();
                    user.put("id", rs.getLong("id"));
                    user.put("username", rs.getString("username"));
                    user.put("nickname", rs.getString("nickname"));
                    user.put("bio", rs.getString("bio"));
                    String avatar = rs.getString("avatar");
                    user.put("avatar", avatar != null ? avatar : "/uploads/images/user/user.png");
                    
                    connection.close();
                    return "{\"status\":\"success\",\"message\":\"登录成功\",\"user\":" + toJson(user) + "}";
                    
                } catch (Exception e) {
                    e.printStackTrace();
                    return "{\"status\":\"error\",\"message\":\"数据库查询失败\"}";
                }
            } else {
                // 内存模式，模拟登录
                if (username.equals("test") && password.equals("123")) {
                    Map<String, Object> user = new HashMap<>();
                    user.put("id", 1L);
                    user.put("username", "test");
                    user.put("nickname", "测试用户");
                    user.put("bio", "这是一个测试用户");
                    user.put("avatar", "/uploads/images/user/user.png");
                    
                    return "{\"status\":\"success\",\"message\":\"登录成功\",\"user\":" + toJson(user) + "}";
                } else if (username.equals("test")) {
                    return "{\"status\":\"password_error\",\"message\":\"密码错误\"}";
                } else {
                    return "{\"status\":\"not_registered\",\"message\":\"用户未注册\"}";
                }
            }
        }
        
        private String register(HttpExchange exchange) throws IOException {
            String body = readBody(exchange);
            String username = extractValue(body, "username");
            String password = extractValue(body, "password");
            String nickname = extractValue(body, "nickname");
            
            if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
                return "{\"error\":\"用户名和密码不能为空\"}";
            }
            
            Map<String, Object> user = new HashMap<>();
            user.put("id", 2L);
            user.put("username", username);
            user.put("nickname", nickname != null && !nickname.isEmpty() ? nickname : username);
            user.put("bio", "");
            user.put("avatar", "/uploads/images/user/user.png");
            
            return "{\"status\":\"success\",\"message\":\"注册成功\",\"user\":" + toJson(user) + "}";
        }
        
        private String getUser(long id) {
            Map<String, Object> user = new HashMap<>();
            user.put("id", id);
            user.put("username", "test");
            user.put("nickname", "测试用户");
            user.put("bio", "这是一个测试用户");
            user.put("avatar", "/uploads/images/user/user.png");
            
            return toJson(user);
        }
        
        private String getComments(long postId) {
            if (App.useDatabase) {
                try {
                    Class.forName("com.mysql.cj.jdbc.Driver");
                    Connection connection = DriverManager.getConnection(
                        "jdbc:mysql://localhost:3306/cs_login?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true",
                        "root",
                        "123"
                    );
                    
                    // 查询评论列表
                    String commentsQuery = "SELECT c.id, c.content, c.user_id, c.parent_id, c.created_at, u.username, u.nickname, u.avatar FROM comments c JOIN users u ON c.user_id = u.id WHERE c.post_id = ? ORDER BY c.created_at ASC";
                    PreparedStatement stmt = connection.prepareStatement(commentsQuery);
                    stmt.setLong(1, postId);
                    ResultSet rs = stmt.executeQuery();
                    
                    StringBuilder comments = new StringBuilder();
                    comments.append("[");
                    boolean first = true;
                    
                    while (rs.next()) {
                        if (!first) comments.append(",");
                        comments.append("{");
                        comments.append("\"id\":").append(rs.getLong("id")).append(",");
                        comments.append("\"content\":\"").append(rs.getString("content")).append("\",");
                        comments.append("\"user\":{");
                        comments.append("\"id\":").append(rs.getLong("user_id")).append(",");
                        comments.append("\"username\":\"").append(rs.getString("username")).append("\",");
                        comments.append("\"nickname\":\"").append(rs.getString("nickname")).append("\",");
                        String commentAvatar = rs.getString("avatar");
                        comments.append("\"avatar\":\"").append(commentAvatar != null ? commentAvatar : "/uploads/images/user/user.png").append("\"");
                        comments.append("},");
                        comments.append("\"parentId\":").append(rs.getLong("parent_id")).append(",");
                        comments.append("\"createdAt\":\"").append(rs.getTimestamp("created_at")).append("\"");
                        comments.append("}");
                        first = false;
                    }
                    comments.append("]");
                    
                    connection.close();
                    
                    return comments.toString();
                    
                } catch (Exception e) {
                    e.printStackTrace();
                    return "[]";
                }
            } else {
                return "[]";
            }
        }
        
        private String uploadFile(HttpExchange exchange) throws IOException {
            // 处理文件上传
            String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
            if (contentType == null || !contentType.startsWith("multipart/form-data")) {
                return "{\"error\":\"Invalid content type\"}";
            }
            
            // 获取查询参数
            String query = exchange.getRequestURI().getQuery();
            
            // 解析查询参数
            String type = extractQueryValue(query, "type");
            String idStr = extractQueryValue(query, "id");
            Long id = idStr != null && !idStr.isEmpty() ? Long.parseLong(idStr) : null;
            
            try {
                // 读取请求体
                InputStream is = exchange.getRequestBody();
                byte[] fileBytes = is.readAllBytes();
                is.close();
                
                System.out.println("File upload received, content length: " + fileBytes.length);
                
                // 解析文件名
                String boundary = contentType.substring(contentType.indexOf("boundary=") + 9);
                String body = new String(fileBytes, "ISO-8859-1");
                
                // 提取文件名
                String filename = "upload.jpg";
                int filenameIndex = body.indexOf("filename=\"");
                if (filenameIndex != -1) {
                    int start = filenameIndex + 10;
                    int end = body.indexOf("\"", start);
                    if (end != -1) {
                        filename = body.substring(start, end);
                    }
                }
                System.out.println("Extracted filename: " + filename);
                
                // 提取文件内容
                int contentStart = body.indexOf("\r\n\r\n") + 4;
                int contentEnd = body.lastIndexOf("\r\n--" + boundary);
                if (contentEnd == -1) contentEnd = body.length();
                
                String extension = filename.substring(filename.lastIndexOf("."));
                System.out.println("File extension: " + extension);
                
                // 构建存储路径
                String uploadPath;
                String fileUrl;
                
                if (type != null && !type.isEmpty() && id != null) {
                    if ("user".equals(type)) {
                        // 用户头像
                        uploadPath = "uploads/images/user/" + id;
                        fileUrl = "/uploads/images/user/" + id + "/avatar" + extension;
                        
                        // 创建目录
                        File uploadDir = new File(uploadPath);
                        if (!uploadDir.exists()) {
                            uploadDir.mkdirs();
                        }
                        
                        // 保存文件
                        String filePath = uploadPath + "/avatar" + extension;
                        FileOutputStream fos = new FileOutputStream(filePath);
                        fos.write(fileBytes, contentStart, contentEnd - contentStart);
                        fos.close();
                        System.out.println("User avatar saved to: " + filePath);
                        
                    } else if ("post".equals(type)) {
                        // 帖子图片
                        uploadPath = "uploads/images/post/" + id;
                        
                        // 创建目录
                        File uploadDir = new File(uploadPath);
                        if (!uploadDir.exists()) {
                            uploadDir.mkdirs();
                        }
                        
                        // 计算自增编号
                        File[] existingFiles = uploadDir.listFiles((dir, name) -> name.matches("image_\\d+\\..*"));
                        int nextNumber = existingFiles != null ? existingFiles.length + 1 : 1;
                        String newFilename = "image_" + nextNumber + extension;
                        fileUrl = "/uploads/images/post/" + id + "/" + newFilename;
                        
                        // 保存文件
                        String filePath = uploadPath + "/" + newFilename;
                        FileOutputStream fos = new FileOutputStream(filePath);
                        fos.write(fileBytes, contentStart, contentEnd - contentStart);
                        fos.close();
                        System.out.println("Post file saved to: " + filePath);
                    } else {
                        fileUrl = "/uploads/images/" + java.util.UUID.randomUUID().toString() + extension;
                    }
                } else {
                    fileUrl = "/uploads/images/" + java.util.UUID.randomUUID().toString() + extension;
                    // 保存文件
                    String filePath = "uploads/images/" + fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
                    File uploadDir = new File("uploads/images");
                    if (!uploadDir.exists()) {
                        uploadDir.mkdirs();
                    }
                    FileOutputStream fos = new FileOutputStream(filePath);
                    fos.write(fileBytes, contentStart, contentEnd - contentStart);
                    fos.close();
                    System.out.println("File saved to: " + filePath);
                }
                
                System.out.println("Returning file URL: " + fileUrl);
                return "{\"url\":\"" + fileUrl + "\"}";
                
            } catch (Exception e) {
                e.printStackTrace();
                return "{\"error\":\"File upload failed: " + e.getMessage() + "\"}";
            }
        }
        
        private void serveStaticFile(HttpExchange exchange, String path) throws IOException {
            // 移除开头的 /uploads/
            String filePath = path.substring(9);
            System.out.println("Request path: " + path);
            System.out.println("File path: " + filePath);
            
            // 获取当前工作目录
            String workingDir = System.getProperty("user.dir");
            System.out.println("Working directory: " + workingDir);
            
            File file = new File("uploads/" + filePath);
            System.out.println("Absolute path: " + file.getAbsolutePath());
            System.out.println("File exists: " + file.exists());
            System.out.println("Is file: " + file.isFile());
            
            if (!file.exists() || !file.isFile()) {
                exchange.sendResponseHeaders(404, 0);
                exchange.close();
                return;
            }
            
            // 设置Content-Type
            System.out.println("Setting Content-Type for path: " + path);
            String contentType = "application/octet-stream";
            if (path.endsWith(".png")) {
                contentType = "image/png";
            } else if (path.endsWith(".jpg") || path.endsWith(".jpeg")) {
                contentType = "image/jpeg";
            } else if (path.endsWith(".gif")) {
                contentType = "image/gif";
            } else if (path.endsWith(".webp")) {
                contentType = "image/webp";
            } else if (path.endsWith(".svg")) {
                contentType = "image/svg+xml";
            } else if (path.endsWith(".mp4")) {
                contentType = "video/mp4";
            } else if (path.endsWith(".webm")) {
                contentType = "video/webm";
            }
            System.out.println("Content-Type set to: " + contentType);
            
            exchange.getResponseHeaders().add("Content-Type", contentType);
            exchange.sendResponseHeaders(200, file.length());
            
            OutputStream os = exchange.getResponseBody();
            FileInputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[8192];
            int count;
            while ((count = fis.read(buffer)) != -1) {
                os.write(buffer, 0, count);
            }
            fis.close();
            os.close();
        }
        
        private String readBody(HttpExchange exchange) throws IOException {
            InputStream is = exchange.getRequestBody();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            br.close();
            return sb.toString();
        }
        
        private String extractValue(String json, String key) {
            if (json == null || json.isEmpty()) {
                return "";
            }
            
            // 尝试使用简单的JSON解析
            try {
                // 找到key的位置
                int keyIndex = json.indexOf("\"" + key + "\":");
                if (keyIndex == -1) {
                    return "";
                }
                
                // 找到值的开始位置
                int valueStart = json.indexOf(':', keyIndex) + 1;
                
                // 跳过空白字符
                while (valueStart < json.length() && Character.isWhitespace(json.charAt(valueStart))) {
                    valueStart++;
                }
                
                // 处理不同类型的值
                if (valueStart < json.length()) {
                    char firstChar = json.charAt(valueStart);
                    
                    // 字符串值
                    if (firstChar == '"') {
                        int valueEnd = json.indexOf('"', valueStart + 1);
                        if (valueEnd != -1) {
                            return json.substring(valueStart + 1, valueEnd);
                        }
                    }
                    // 数字或布尔值
                    else {
                        int valueEnd = valueStart;
                        while (valueEnd < json.length() && 
                               (Character.isDigit(json.charAt(valueEnd)) || 
                                json.charAt(valueEnd) == '.' || 
                                json.charAt(valueEnd) == '-' ||
                                json.charAt(valueEnd) == 't' ||
                                json.charAt(valueEnd) == 'f' ||
                                json.charAt(valueEnd) == 'r' ||
                                json.charAt(valueEnd) == 'u' ||
                                json.charAt(valueEnd) == 'e' ||
                                json.charAt(valueEnd) == 'a' ||
                                json.charAt(valueEnd) == 'l' ||
                                json.charAt(valueEnd) == 's')) {
                            valueEnd++;
                        }
                        return json.substring(valueStart, valueEnd);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            // 回退到正则表达式
            String pattern = "\"" + key + "\":\"([^\"]+)\"";
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(json);
            if (m.find()) {
                return m.group(1);
            }
            
            return "";
        }
        
        private String extractQueryValue(String query, String key) {
            if (query == null || query.isEmpty()) {
                return "";
            }
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=");
                if (keyValue.length >= 1 && keyValue[0].equals(key)) {
                    try {
                        return keyValue.length == 2 ? java.net.URLDecoder.decode(keyValue[1], "UTF-8") : "";
                    } catch (Exception e) {
                        return keyValue.length == 2 ? keyValue[1] : "";
                    }
                }
            }
            return "";
        }
        
        private String toJson(Map<String, Object> map) {
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            boolean first = true;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (!first) sb.append(",");
                sb.append("\"").append(entry.getKey()).append("\":");
                Object value = entry.getValue();
                if (value instanceof String) {
                    sb.append("\"").append(value).append("\"");
                } else if (value instanceof Map) {
                    sb.append(toJson((Map<String, Object>) value));
                } else {
                    sb.append(value);
                }
                first = false;
            }
            sb.append("}");
            return sb.toString();
        }
    }
    
    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            
            System.out.println("Static file request: " + path);
            
            // 移除开头的 /uploads/
            String filePath = path.substring(9);
            System.out.println("File path: " + filePath);
            
            // 获取当前工作目录
            String workingDir = System.getProperty("user.dir");
            System.out.println("Working directory: " + workingDir);
            
            File file = new File("uploads/" + filePath);
            System.out.println("Absolute path: " + file.getAbsolutePath());
            System.out.println("File exists: " + file.exists());
            System.out.println("Is file: " + file.isFile());
            
            if (!file.exists() || !file.isFile()) {
                String response = "File not found";
                exchange.sendResponseHeaders(404, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
                return;
            }
            
            // 设置Content-Type
            String contentType = "application/octet-stream";
            if (path.endsWith(".png")) {
                contentType = "image/png";
            } else if (path.endsWith(".jpg") || path.endsWith(".jpeg")) {
                contentType = "image/jpeg";
            } else if (path.endsWith(".gif")) {
                contentType = "image/gif";
            } else if (path.endsWith(".webp")) {
                contentType = "image/webp";
            } else if (path.endsWith(".svg")) {
                contentType = "image/svg+xml";
            } else if (path.endsWith(".mp4")) {
                contentType = "video/mp4";
            } else if (path.endsWith(".webm")) {
                contentType = "video/webm";
            }
            
            exchange.getResponseHeaders().add("Content-Type", contentType);
            exchange.sendResponseHeaders(200, file.length());
            
            OutputStream os = exchange.getResponseBody();
            FileInputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[8192];
            int count;
            while ((count = fis.read(buffer)) != -1) {
                os.write(buffer, 0, count);
            }
            fis.close();
            os.close();
        }
    }
}