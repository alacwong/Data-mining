package Database;
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

  private static final String masterTable = "__TABLE";

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

  public Connection start(Connection con) throws ClassNotFoundException, SQLException {
    if (con == null) {
      Class.forName("org.sqlite.JDBC");
      con = DriverManager.getConnection("jdbc:sqlite:twitter.db");

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

      String sql = "CREATE TABLE " + text + " "
          + "(ID INTEGER PRIMARY KEY NOT NULL," 
          + "WORD TEXT NOT NULL,"
          + "FREQUENCY INT NOT NULL,"
          + "PATH TEXT,"
          + "PATHID INT)";
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
   * Inserts word into database in a more efficient method
   * @param text Word to be inserted
   * @param con Connection to db
   * @throws ClassNotFoundException
   * @throws SQLException
   */
  public void InsertWord(String text, Connection con, int frequency, 
      HashMap<String, Integer> tableMap, ArrayList<String> orderedWord) throws ClassNotFoundException, SQLException {

    String table =  searchTable( "_" + text.substring(0, 1), text.substring(1), con);
    String sql;
    PreparedStatement pstmt;
    WordNode word;
    
    if (!tableMap.containsKey(table)) {
      createTable(table, con);
      tableMap.put(table, tableMap.size() + 1);
      orderedWord.add(table);
    }

    try {
      word = getWordNode(table, text, con);
    } catch(WordNotFoundException e) {

      if (text.length() > table.length()) {
        sql = "INSERT INTO " + table + "(WORD, FREQUENCY, PATH, PATHID) VALUES(?,?,?,?)";
        pstmt = con.prepareStatement(sql);
        pstmt.setString(1, text);
        pstmt.setInt(2, frequency);
        pstmt.setString(3, "_" + text.substring(0, table.length()));
        pstmt.setInt(4, tableMap.get(table));
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
    int id = 0;

    if (table.length() > 2) {
      id = getId(table.substring(0, table.length() - 1), table, con);
    } else {
      id = getId(table);
    }

    try {
      word = getWordNode(table, text, con);
    } catch(WordNotFoundException e) {

      if (text.length() > table.length()) {
        sql = "INSERT INTO " + table + "(WORD, FREQUENCY, PATH, PATHID) VALUES(?,?,?,?)";
        int newId = newMaster(con, text.substring(0, table.length()));
        pstmt = con.prepareStatement(sql);
        pstmt.setString(1, text);
        pstmt.setInt(2, frequency);
        pstmt.setString(3, "_" + text.substring(0, table.length()));
        pstmt.setInt(4, newId);
        pstmt.executeUpdate();
        pstmt.close();
        updateMaster(con, id);

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

  public static boolean tableExist(Connection conn, String tableName) throws SQLException {
    boolean tExists = false;
    try (ResultSet rs = conn.getMetaData().getTables(null, null, tableName, null)) {
      while (rs.next()) { 
        String tName = rs.getString("TABLE_NAME");
        if (tName != null && tName.equals(tableName)) {
          tExists = true;
          break;
        }
      }
    }
    return tExists;
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
    String table = "_" + searchTable(text.substring(0,1),text, con);
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
          d.getStringValue(), con));
    }

    return new MasterHead(neighbors);
  }

  private WordNode getTree(WordNode head, String table, Connection con) throws SQLException {
    Statement statement = con.createStatement();
    ResultSet r = statement.executeQuery("SELECT * FROM " + table + ";");

    HashMap<Character, WordNode> neighbors = new HashMap<Character, WordNode>();

    while(r.next()) {
      if (r.getString("PATH") != null) {
        neighbors.put(r.getString("PATH").charAt(r.getString("PATH").length()-1) ,
            getTree(new WordNode(r.getString("WORD"), 
                r.getInt("FREQUENCY")), r.getString("PATH"), con));
      } else {
        neighbors.put(null, new WordNode("", r.getInt("FREQUENCY")));
      }
    }

    head.setNeighbors(neighbors);
    r.close();
    return head;
  }


  private int getMasterEntries(Connection con, int id) throws SQLException {
    String sql = "SELECT * FROM " + masterTable + " WHERE ID = ?";
    PreparedStatement pstmt = con.prepareStatement(sql);
    pstmt.setInt(1, id);
    ResultSet results = pstmt.executeQuery();
    int entry = results.getInt("ENTRIES");
    results.close();
    return entry;
  }
  
  private int getId(String table, String path, Connection con) throws SQLException {
    String sql = "SELECT * FROM " + table + ";";
    PreparedStatement pstmt = con.prepareStatement(sql);
    ResultSet results = pstmt.executeQuery();
    int id = 0;
    while (results.next()) {
      if (results.getString("PATH") != null && 
          results.getString("PATH").equals(path)) {
        id = results.getInt("PATHID");
      }
    }
    results.close();
    return id;
  }

  private int getId(String table) {
    return ((int)table.charAt(1)) - 96;
  }

  private void updateMaster(Connection con, int id) throws SQLException {
    String sql = "UPDATE " + masterTable + " SET ENTRIES = ? "
        + "WHERE ID = ?";
    PreparedStatement pstmt = con.prepareStatement(sql);
    pstmt.setInt(1, getMasterEntries(con, id) + 1);
    pstmt.setInt(2, id);
    pstmt.executeUpdate();
    pstmt.close();
  }
  
  public void updateMaster(Connection con, int id, int newValue) throws SQLException {
    String sql = "UPDATE " + masterTable + " SET ENTRIES = ? "
        + "WHERE ID = ?";
    PreparedStatement pstmt = con.prepareStatement(sql);
    pstmt.setInt(1, newValue);
    pstmt.setInt(2, id);
    pstmt.executeUpdate();
    pstmt.close();
  }
  
  public void setMaster(Connection con, String newTable, int entries) throws SQLException {
    String sql = "INSERT INTO " + masterTable + "(TABLENAME, ENTRIES) VALUES(?,?)";
    PreparedStatement pstmt = con.prepareStatement(sql);
    pstmt.setString(1, newTable);
    pstmt.setInt(2, entries);
    pstmt.executeUpdate();
  }

  private int newMaster(Connection con, String table) throws SQLException {

    String sql = "INSERT INTO " + masterTable + "(TABLENAME, ENTRIES) VALUES(?,?)";
    PreparedStatement pstmt = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
    pstmt.setString(1, table);
    pstmt.setInt(2, 0);
    pstmt.executeUpdate();
    ResultSet uniqueKey =  pstmt.getGeneratedKeys();
    uniqueKey.next();
    int returnValue = uniqueKey.getInt(1);
    uniqueKey.close();
    pstmt.close();
    return returnValue;
  }
  
  public ArrayList<String> setMasterTable(Connection con, 
      HashMap<String, Integer> tableMap) throws SQLException{    
    String sql = "SELECT * FROM " + masterTable + ";";
    PreparedStatement pstmt = con.prepareStatement(sql);
    ArrayList<String> orderedList = new ArrayList<String>();
    ResultSet r = pstmt.executeQuery();
    while (r.next()) {
      tableMap.put(r.getString("TABLENAME"), r.getInt("ENTRIES"));
      orderedList.add(r.getString("TABLENAME"));
    }
    return orderedList;
  }
  
  public void initialize(Connection connection) {
    try {
      Statement stmt = connection.createStatement();

      String sql = "CREATE TABLE " + masterTable + " "
          + "(ID INTEGER PRIMARY KEY NOT NULL," 
          + "TABLENAME TEXT NOT NULL,"
          + "ENTRIES INT NOT NULL)";

      try {
        stmt.execute(sql);
      } catch (Exception e) {
        e.printStackTrace();
      }
      stmt.close();
      for (DICTIONARY d: DICTIONARY.getAll()) {
        createTable(("_" + d.getStringValue()),  connection);
        newMaster(connection, "_" + d.getStringValue());
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}