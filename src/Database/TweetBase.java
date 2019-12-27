package Database;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;
import ENUM.DICTIONARY;
import Exceptions.WordNotFoundException;
import Tree.MasterHead;
import Tree.WordNode;

/**
 * Database class to determine popularity of words
 * @author Alac Wong
 *
 */
public class TweetBase {
  
  public static String dbname = "twitter.db";
  //TODO
  //create database, links to other database
  //word children 

  /**
   * Method extracts special characters, @,# and links from tweets
   */
  public static Stack<String> extractTweet(String text) {
    Stack<String> newWords = new Stack<String>();
    String [] words = text.split(" ");
    for (int i = 0; i < words.length; i++) {
      if (!words[i].replaceAll("[^A-Za-z]","").equals("")) {
        newWords.add(words[i].replaceAll("[^A-Za-z]",""));
      }
    }
    return newWords;
  }

  public Connection start(Connection con, String fileName) throws ClassNotFoundException, SQLException {
    if (con == null) {
      Class.forName("org.sqlite.JDBC");
      con = DriverManager.getConnection("jdbc:sqlite:" + fileName);

    }
    return con;
  }

  /**
   * create tree like table in sql
   * @param text
   * @param level
   * @param table
   * @throws SQLException
   */
  public  String createTable(String text, Connection con) {

    try {
      Statement stmt = con.createStatement();

      String sql = "CREATE TABLE IF NOT EXISTS " + text + " "
          + "(ID INTEGER PRIMARY KEY NOT NULL," 
          + "WORD TEXT NOT NULL,"
          + "FREQUENCY INT NOT NULL,"
          + "PATH TEXT)";
      try {
        stmt.execute(sql);
      } catch (Exception e) {
        e.printStackTrace();
      }
      stmt.close();
    } catch (SQLException e) {

    }
    return text;

  }
  
 

  /**
   * Inserts word into database
   * @param text Word to be inserted
   * @param con Connection to db
   * @throws ClassNotFoundException
   * @throws SQLException
   */
  public void InsertWord(String text, Connection con, int frequency) throws ClassNotFoundException, SQLException {

    String table =  searchTable( "_" + text.substring(0, 1), text.substring(1), con);
    String sql;
    PreparedStatement pstmt;
    WordNode word;
    createTable(table, con);
    
    try {
      word = getWordNode(table, text, con);
    } catch(WordNotFoundException e) {

      if (text.length() > table.length() - 1) {
        sql = "INSERT INTO " + table + "(WORD, FREQUENCY, PATH) VALUES(?,?,?)";
        pstmt = con.prepareStatement(sql);
        pstmt.setString(1, text);
        pstmt.setInt(2, frequency);
        pstmt.setString(3, "_" + text.substring(0, table.length()));
        pstmt.executeUpdate();
        pstmt.close();
      } else {
        sql = "INSERT INTO " + table + "(WORD, FREQUENCY) VALUES(?,?)";
        pstmt = con.prepareStatement(sql);
        pstmt.setString(1, text);
        pstmt.setInt(2, frequency);
        pstmt.executeUpdate();

      }
      return;
    }

    try {
      sql = "UPDATE " + table +" SET FREQUENCY = ? "
          + "WHERE ID = ?;";
      pstmt = con.prepareStatement(sql);
      pstmt.setInt(1, word.getFrequnecy() + frequency);
      pstmt.setInt(2, getWordIndex(table, text, con));
      pstmt.executeUpdate();
      pstmt.close();
    } catch(WordNotFoundException e) {
      e.printStackTrace();
    }
  }

  public static boolean tableExist(Connection con, String tableName) throws SQLException {
    Statement statement = con.createStatement();
    try {
    String sql = "SELECT * FROM sqlite_master WHERE type = 'table' AND name = '" + tableName + "';";
    ResultSet r = statement.executeQuery(sql);
    return r.next();
    } catch (SQLException e) {
      return false;
    } 
  }

  /**
   * 
   * @param table
   * @param word
   * @param con
   * @return
   * @throws SQLException
   * @throws WordNotFoundException
   */
  private int getWordIndex(String table, String word, Connection con) throws SQLException, WordNotFoundException {
    Statement statement = con.createStatement();
    ResultSet r = statement.executeQuery("SELECT * FROM " + table + ";");

    while(r.next()) {
      if (r.getString("WORD").contentEquals(word)) {
        return r.getRow();
      } 
    }
    throw new WordNotFoundException();
  }

  public WordNode getWordNode(String text, Connection con) throws WordNotFoundException, SQLException {
    String table = searchTable("_" + text.substring(0,1),text.substring(1), con);
    return getWordNode(table, text, con);
  }
  
  private WordNode getWordNode(String table, String text, Connection con) throws WordNotFoundException, SQLException {
    Statement statement = con.createStatement();
    ResultSet r = statement.executeQuery("SELECT * FROM " + table + ";");
    while (r.next()) {
      if (r.getString("WORD").equals(text)) {
        return new WordNode(text, r.getInt("FREQUENCY"));
      }
    }
    throw new WordNotFoundException();
  }
  
  private String searchTable(String table, String word, Connection con)  {
    try {
      Statement statement = con.createStatement();
      ResultSet r = statement.executeQuery("SELECT * FROM " + table + ";");
      if (!word.equals("")) {
        String newTable = table + word.charAt(0);
        String newWord = word.substring(1);
        while(r.next()) {
          if (newTable.equals(r.getString("PATH"))) {
            if ((newTable + newWord).substring(1).equals(r.getString("WORD"))) {
              return table;
            } else {
              return searchTable(newTable, newWord, con);
            }
          }
        } 
      }
    } catch (SQLException e) {
      createTable(table, con);
    }
    return table;
  }

  public WordNode getTree(String text, Connection con) throws SQLException, WordNotFoundException {

    String table = "_" + searchTable(text.substring(0), text, con);
    WordNode head = getWordNode(table, text, con);
    return getTree(head, table, con);
  }

  public MasterHead getMasterTree(Connection con) throws SQLException {

    HashMap<Character, WordNode> neighbors = new HashMap<Character, WordNode>();

    for (DICTIONARY d: DICTIONARY.getAll()) {
      neighbors.put(d.getCharValue() ,getTree(new WordNode(d.getStringValue(), 0),
          "_" + d.getStringValue(), con));
    }
    return new MasterHead(neighbors);
  }

  private WordNode getTree(WordNode head, String table, Connection con) throws SQLException {
    Statement statement = con.createStatement();
    ResultSet r = statement.executeQuery("SELECT * FROM " + table + ";");

    HashMap<Character, WordNode> neighbors = new HashMap<Character, WordNode>();

    while(r.next()) {
      if (r.getString("PATH") != null) {
        if (tableExist(con,r.getString("PATH"))) {
        neighbors.put(r.getString("PATH").charAt(r.getString("PATH").length()-1) ,
            getTree(new WordNode(r.getString("WORD"), 
                r.getInt("FREQUENCY")), r.getString("PATH"), con));
        }
      } else {
        neighbors.put(null, new WordNode(r.getString("WORD"), r.getInt("FREQUENCY")));
      }
    }

    head.setNeighbors(neighbors);
    r.close();
    return head;
  }
  
  public static boolean clearDatabase(String fileName) {
    Path path = Paths.get(fileName);
    try {
      Files.deleteIfExists(path);
      return true;
    } catch (IOException e) {
      e.printStackTrace();
    }
    return false;
  }
  
  public void initialize(Connection connection) {
    try {
      for (DICTIONARY d: DICTIONARY.getAll()) {
        createTable(("_" + d.getStringValue()),  connection);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}