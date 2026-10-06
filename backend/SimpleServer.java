import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

public class SimpleServer {
    
    private static Connection connection;
    private static boolean useDatabase = false;
    
    public static void main(String[] args) throws Exception {
        initDatabase();
        
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/api", new ApiHandler());
        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();
        System.out.println("Server started on port 8080");
        System.out.println("Database mode: " + (useDatabase ? "MySQL" : "Memory"));
    }
    
    private static void initDatabase() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/cs_login?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true",
                "root",
                "123"
            );
            useDatabase = true;
            System.out.println("Database connected successfully");
            
            // 创建评论表
            createCommentsTable();
        } catch (Exception e) {
            System.out.println("Database connection failed: " + e.getMessage());
            useDatabase = false;
        }
    }
    
    private static void createCommentsTable() {
        try {
            // 先检查 posts 和 users 表是否存在
            String checkTablesSQL = "SHOW TABLES LIKE 'posts'";
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(checkTablesSQL);
            if (!rs.next()) {
                System.out.println("Posts table does not exist, cannot create comments table");
                return;
            }
            
            // 检查 users 表
            checkTablesSQL = "SHOW TABLES LIKE 'users'";
            rs = stmt.executeQuery(checkTablesSQL);
            if (!rs.next()) {
                System.out.println("Users table does not exist, cannot create comments table");
                return;
            }
            
            // 创建评论表，不使用外键约束
            String createTableSQL = "CREATE TABLE IF NOT EXISTS comments (id BIGINT PRIMARY KEY AUTO_INCREMENT, content TEXT NOT NULL, post_id BIGINT NOT NULL, user_id BIGINT NOT NULL, parent_id BIGINT, created_at DATETIME DEFAULT CURRENT_TIMESTAMP)";
            stmt.executeUpdate(createTableSQL);
            System.out.println("Comments table created or already exists");
        } catch (SQLException e) {
            System.out.println("Error creating comments table: " + e.getMessage());
        }
    }
    
    static class ApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();
            
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
                    if (method.equals("GET")) {
                        if (path.equals("/api/posts")) {
                            response = getPosts(exchange.getRequestURI().getQuery());
                        } else if (path.matches("/api/posts/\\d+")) {
                            long id = Long.parseLong(path.split("/")[3]);
                            response = getPost(id);
                        } else if (path.matches("/api/posts/type/\\w+")) {
                            String type = path.split("/")[4];
                            response = getPostsByType(type);
                        }
                    } else if (method.equals("POST")) {
                        response = createPost(exchange);
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
                }
            } catch (Exception e) {
                e.printStackTrace();
                statusCode = 500;
                response = "{\"error\":\"Internal Server Error\"}";
            }
            
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(statusCode, response.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
        
        private String getPosts(String query) {
            int page = 0;
            int size = 10;
            
            if (query != null && !query.isEmpty()) {
                String[] pairs = query.split("&");
                for (String pair : pairs) {
                    String[] keyValue = pair.split("=");
                    if (keyValue.length == 2) {
                        if (keyValue[0].equals("page")) {
                            try { page = Integer.parseInt(keyValue[1]); } catch (Exception e) {}
                        } else if (keyValue[0].equals("size")) {
                            try { size = Integer.parseInt(keyValue[1]); } catch (Exception e) {}
                        }
                    }
                }
            }
            
            int offset = page * size;
            
            if (useDatabase && connection != null) {
                try {
                    String countQuery = "SELECT COUNT(*) as total FROM posts";
                    Statement countStmt = connection.createStatement();
                    ResultSet countRs = countStmt.executeQuery(countQuery);
                    int total = 0;
                    if (countRs.next()) {
                        total = countRs.getInt("total");
                    }
                    
                    String dataQuery = "SELECT p.*, u.username, u.nickname, u.avatar FROM posts p JOIN users u ON p.user_id = u.id ORDER BY p.created_at DESC LIMIT ? OFFSET ?";
                    PreparedStatement stmt = connection.prepareStatement(dataQuery);
                    stmt.setInt(1, size);
                    stmt.setInt(2, offset);
                    ResultSet rs = stmt.executeQuery();
                    
                    StringBuilder sb = new StringBuilder();
                    sb.append("{\"content\":[");
                    boolean first = true;
                    
                    while (rs.next()) {
                        if (!first) sb.append(",");
                        
                        Map<String, Object> post = new HashMap<>();
                        post.put("id", rs.getLong("id"));
                        post.put("title", rs.getString("title"));
                        post.put("content", rs.getString("content"));
                        post.put("contentType", rs.getString("content_type"));
                        post.put("viewCount", rs.getInt("view_count"));
                        post.put("likeCount", rs.getInt("like_count"));
                        post.put("commentCount", rs.getInt("comment_count"));
                        post.put("createdAt", rs.getTimestamp("created_at").toString());
                        
                        Map<String, Object> user = new HashMap<>();
                        user.put("id", rs.getLong("user_id"));
                        user.put("username", rs.getString("username"));
                        user.put("nickname", rs.getString("nickname"));
                        user.put("avatar", rs.getString("avatar"));
                        post.put("user", user);
                        
                        sb.append(toJson(post));
                        first = false;
                    }
                    
                    boolean isLast = (offset + size) >= total;
                    sb.append("],\"totalElements\":").append(total);
                    sb.append(",\"totalPages\":").append((int) Math.ceil((double) total / size));
                    sb.append(",\"number\":").append(page);
                    sb.append(",\"last\":").append(isLast);
                    sb.append("}");
                    
                    return sb.toString();
                    
                } catch (SQLException e) {
                    e.printStackTrace();
                    return "{\"error\":\"数据库查询失败\"}";
                }
            } else {
                return "{\"error\":\"数据库连接失败\"}";
            }
        }
        
        private String getPost(long id) {
            if (useDatabase && connection != null) {
                try {
                    String query = "SELECT p.*, u.username, u.nickname, u.avatar FROM posts p JOIN users u ON p.user_id = u.id WHERE p.id = ?";
                    PreparedStatement stmt = connection.prepareStatement(query);
                    stmt.setLong(1, id);
                    ResultSet rs = stmt.executeQuery();
                    
                    if (!rs.next()) {
                        return "{\"error\":\"Post not found\"}";
                    }
                    
                    Map<String, Object> post = new HashMap<>();
                    post.put("id", rs.getLong("id"));
                    post.put("title", rs.getString("title"));
                    post.put("content", rs.getString("content"));
                    post.put("contentType", rs.getString("content_type"));
                    post.put("viewCount", rs.getInt("view_count"));
                    post.put("likeCount", rs.getInt("like_count"));
                    post.put("commentCount", rs.getInt("comment_count"));
                    post.put("createdAt", rs.getTimestamp("created_at").toString());
                    
                    Map<String, Object> user = new HashMap<>();
                    user.put("id", rs.getLong("user_id"));
                    user.put("username", rs.getString("username"));
                    user.put("nickname", rs.getString("nickname"));
                    user.put("avatar", rs.getString("avatar"));
                    post.put("user", user);
                    
                    return "{\"post\": " + toJson(post) + ", \"displayContent\": \"" + rs.getString("content") + "\"}";
                    
                } catch (SQLException e) {
                    e.printStackTrace();
                    return "{\"error\":\"数据库查询失败\"}";
                }
            } else {
                return "{\"error\":\"数据库连接失败\"}";
            }
        }
        
        private String getPostsByType(String type) {
            if (useDatabase && connection != null) {
                try {
                    String query = "SELECT p.*, u.username, u.nickname, u.avatar FROM posts p JOIN users u ON p.user_id = u.id WHERE p.content_type = ? ORDER BY p.created_at DESC";
                    PreparedStatement stmt = connection.prepareStatement(query);
                    stmt.setString(1, type);
                    ResultSet rs = stmt.executeQuery();
                    
                    StringBuilder sb = new StringBuilder();
                    sb.append("{\"content\":[");
                    boolean first = true;
                    
                    while (rs.next()) {
                        if (!first) sb.append(",");
                        
                        Map<String, Object> post = new HashMap<>();
                        post.put("id", rs.getLong("id"));
                        post.put("title", rs.getString("title"));
                        post.put("content", rs.getString("content"));
                        post.put("contentType", rs.getString("content_type"));
                        post.put("viewCount", rs.getInt("view_count"));
                        post.put("likeCount", rs.getInt("like_count"));
                        post.put("commentCount", rs.getInt("comment_count"));
                        post.put("createdAt", rs.getTimestamp("created_at").toString());
                        
                        Map<String, Object> user = new HashMap<>();
                        user.put("id", rs.getLong("user_id"));
                        user.put("username", rs.getString("username"));
                        user.put("nickname", rs.getString("nickname"));
                        user.put("avatar", rs.getString("avatar"));
                        post.put("user", user);
                        
                        sb.append(toJson(post));
                        first = false;
                    }
                    
                    sb.append("]}");
                    return sb.toString();
                    
                } catch (SQLException e) {
                    e.printStackTrace();
                    return "{\"error\":\"数据库查询失败\"}";
                }
            } else {
                return "{\"error\":\"数据库连接失败\"}";
            }
        }
        
        private String createPost(HttpExchange exchange) throws IOException {
            String body = readBody(exchange);
            String query = exchange.getRequestURI().getQuery();
            String userId = extractQueryValue(query, "userId");
            
            String title = extractValue(body, "title");
            String content = extractValue(body, "content");
            String contentType = extractValue(body, "contentType");
            
            if (contentType == null || contentType.isEmpty()) {
                contentType = "article";
            }
            
            if (useDatabase && connection != null) {
                try {
                    String dbUserId = userId;
                    if (dbUserId == null || dbUserId.isEmpty()) {
                        dbUserId = "1";
                    }
                    
                    String insertQuery = "INSERT INTO posts (title, content, content_type, user_id, view_count, like_count, comment_count) VALUES (?, ?, ?, ?, 0, 0, 0)";
                    PreparedStatement stmt = connection.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS);
                    stmt.setString(1, title);
                    stmt.setString(2, content);
                    stmt.setString(3, contentType);
                    stmt.setLong(4, Long.parseLong(dbUserId));
                    stmt.executeUpdate();
                    
                    ResultSet rs = stmt.getGeneratedKeys();
                    if (rs.next()) {
                        long postId = rs.getLong(1);
                        
                        String userQuery = "SELECT id, username, nickname, bio, avatar FROM users WHERE id = ?";
                        PreparedStatement userStmt = connection.prepareStatement(userQuery);
                        userStmt.setLong(1, Long.parseLong(dbUserId));
                        ResultSet userRs = userStmt.executeQuery();
                        
                        Map<String, Object> user = new HashMap<>();
                        if (userRs.next()) {
                            user.put("id", userRs.getLong("id"));
                            user.put("username", userRs.getString("username"));
                            user.put("nickname", userRs.getString("nickname"));
                            user.put("bio", userRs.getString("bio"));
                            user.put("avatar", userRs.getString("avatar"));
                        }
                        
                        Map<String, Object> post = new HashMap<>();
                        post.put("id", postId);
                        post.put("title", title);
                        post.put("content", content);
                        post.put("contentType", contentType);
                        post.put("user", user);
                        post.put("viewCount", 0);
                        post.put("likeCount", 0);
                        post.put("commentCount", 0);
                        post.put("createdAt", new java.util.Date().toString());
                        
                        return toJson(post);
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    return "{\"error\":\"数据库插入失败\"}";
                }
            } else {
                return "{\"error\":\"数据库连接失败\"}";
            }
            return "{\"error\":\"创建帖子失败\"}";
        }
        
        private String login(HttpExchange exchange) throws IOException {
            String body = readBody(exchange);
            String username = extractValue(body, "username");
            String password = extractValue(body, "password");
            
            if (useDatabase && connection != null) {
                try {
                    String query = "SELECT id, username, password, nickname, bio, avatar FROM users WHERE username = ?";
                    PreparedStatement stmt = connection.prepareStatement(query);
                    stmt.setString(1, username);
                    ResultSet rs = stmt.executeQuery();
                    
                    if (!rs.next()) {
                        return "{\"status\":\"not_registered\",\"message\":\"用户未注册\"}";
                    }
                    
                    String dbPassword = rs.getString("password");
                    if (!dbPassword.equals(password)) {
                        return "{\"status\":\"password_error\",\"message\":\"密码错误\"}";
                    }
                    
                    Map<String, Object> user = new HashMap<>();
                    user.put("id", rs.getLong("id"));
                    user.put("username", rs.getString("username"));
                    user.put("nickname", rs.getString("nickname"));
                    user.put("bio", rs.getString("bio"));
                    user.put("avatar", rs.getString("avatar"));
                    
                    return "{\"status\":\"success\",\"message\":\"登录成功\",\"user\":" + toJson(user) + "}";
                    
                } catch (SQLException e) {
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
                    user.put("avatar", null);
                    
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
            
            if (useDatabase && connection != null) {
                try {
                    String checkQuery = "SELECT id FROM users WHERE username = ?";
                    PreparedStatement checkStmt = connection.prepareStatement(checkQuery);
                    checkStmt.setString(1, username);
                    ResultSet checkRs = checkStmt.executeQuery();
                    
                    if (checkRs.next()) {
                        return "{\"error\":\"用户名已存在\"}";
                    }
                    
                    String insertQuery = "INSERT INTO users (username, password, nickname, bio, avatar) VALUES (?, ?, ?, ?, ?)";
                    PreparedStatement stmt = connection.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS);
                    stmt.setString(1, username);
                    stmt.setString(2, password);
                    stmt.setString(3, nickname != null && !nickname.isEmpty() ? nickname : username);
                    stmt.setString(4, "");
                    stmt.setString(5, null);
                    stmt.executeUpdate();
                    
                    ResultSet rs = stmt.getGeneratedKeys();
                    if (rs.next()) {
                        long userId = rs.getLong(1);
                        
                        Map<String, Object> user = new HashMap<>();
                        user.put("id", userId);
                        user.put("username", username);
                        user.put("nickname", nickname != null && !nickname.isEmpty() ? nickname : username);
                        user.put("bio", "");
                        user.put("avatar", null);
                        
                        return "{\"status\":\"success\",\"message\":\"注册成功\",\"user\":" + toJson(user) + "}";
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    return "{\"error\":\"数据库插入失败\"}";
                }
            } else {
                return "{\"error\":\"数据库连接失败\"}";
            }
            return "{\"error\":\"注册失败\"}";
        }
        
        private String getUser(long id) {
            if (useDatabase && connection != null) {
                try {
                    String query = "SELECT id, username, nickname, bio, avatar FROM users WHERE id = ?";
                    PreparedStatement stmt = connection.prepareStatement(query);
                    stmt.setLong(1, id);
                    ResultSet rs = stmt.executeQuery();
                    
                    if (!rs.next()) {
                        return "{\"error\":\"User not found\"}";
                    }
                    
                    Map<String, Object> user = new HashMap<>();
                    user.put("id", rs.getLong("id"));
                    user.put("username", rs.getString("username"));
                    user.put("nickname", rs.getString("nickname"));
                    user.put("bio", rs.getString("bio"));
                    user.put("avatar", rs.getString("avatar"));
                    
                    return toJson(user);
                } catch (SQLException e) {
                    e.printStackTrace();
                    return "{\"error\":\"数据库查询失败\"}";
                }
            } else {
                return "{\"error\":\"数据库连接失败\"}";
            }
        }
        
        private String getComments(long postId) {
            if (useDatabase && connection != null) {
                try {
                    String query = "SELECT c.*, u.username, u.nickname, u.avatar FROM comments c JOIN users u ON c.user_id = u.id WHERE c.post_id = ? ORDER BY c.created_at DESC";
                    PreparedStatement stmt = connection.prepareStatement(query);
                    stmt.setLong(1, postId);
                    ResultSet rs = stmt.executeQuery();
                    
                    StringBuilder sb = new StringBuilder();
                    sb.append("[");
                    boolean first = true;
                    
                    while (rs.next()) {
                        if (!first) sb.append(",");
                        
                        Map<String, Object> comment = new HashMap<>();
                        comment.put("id", rs.getLong("id"));
                        comment.put("content", rs.getString("content"));
                        comment.put("createdAt", rs.getTimestamp("created_at").toString());
                        
                        Map<String, Object> user = new HashMap<>();
                        user.put("id", rs.getLong("user_id"));
                        user.put("username", rs.getString("username"));
                        user.put("nickname", rs.getString("nickname"));
                        user.put("avatar", rs.getString("avatar"));
                        comment.put("user", user);
                        
                        sb.append(toJson(comment));
                        first = false;
                    }
                    
                    sb.append("]");
                    return sb.toString();
                } catch (SQLException e) {
                    e.printStackTrace();
                    return "{\"error\":\"数据库查询失败\"}";
                }
            } else {
                return "{\"error\":\"数据库连接失败\"}";
            }
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
                if (keyValue.length == 2 && keyValue[0].equals(key)) {
                    try {
                        return java.net.URLDecoder.decode(keyValue[1], "UTF-8");
                    } catch (Exception e) {
                        return keyValue[1];
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
}