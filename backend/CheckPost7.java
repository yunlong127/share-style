import java.sql.*;

public class CheckPost7 {
    public static void main(String[] args) throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection connection = DriverManager.getConnection(
            "jdbc:mysql://localhost:3306/cs_login?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true",
            "root",
            "123"
        );
        
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT id, title, content FROM posts WHERE id = 7");
        
        if (rs.next()) {
            System.out.println("ID: " + rs.getInt("id"));
            System.out.println("Title: " + rs.getString("title"));
            String content = rs.getString("content");
            System.out.println("Content length: " + content.length());
            System.out.println("Content bytes: " + bytesToHex(content.getBytes("UTF-8")));
            System.out.println("Content: [" + content + "]");
        }
        
        connection.close();
    }
    
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString();
    }
}
