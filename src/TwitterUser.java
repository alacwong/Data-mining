import java.math.BigDecimal;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;
import Database.BaseHandler;
import Database.TweetBase;
import ENUM.TWEETS;
import Exceptions.WordNotFoundException;
import Tree.Data;
import Tree.WordNode;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;

public class TwitterUser {

  private Twitter twitter;
  private static final int shortlen = 50;
  private Stack<Status> testTweets = new Stack<Status>();

  public TwitterUser() {

    TwitterFactory t = new TwitterFactory();
    twitter = t.getInstance();
  }

  public Status tweet(String message) throws TwitterException {
    Status status = twitter.updateStatus(message);
    System.out.println("Status ID:" + status.getId() + "User: " + status.getUser());
    System.out.println(status == null);
    testTweets.add(status);
    return status;
  }

  public List<Status> getTweets(TWEETS tweetMode) throws TwitterException{

    List<Status> x = twitter.getHomeTimeline();
    List<Status> tweets = new ArrayList<>();
    System.out.println("Returning tweets Total: " + x.size());
    for (Status s: x) {
      if (s.getUser().getId() == twitter.getId() || tweetMode.equals(TWEETS.ALL)) {
        System.out.println(shortTweet(s.getText()));
        System.out.println("Tweeted at :" + s.getCreatedAt().toString());
        System.out.println("*************************************");
        tweets.add(s);
      }
    }
    return tweets;
  }

  public String shortTweet(String s) {
    if (s.length() <= shortlen) {
      return s;
    } else {
      char [] str = new char[50];
      for (int i = 0; i < shortlen; i++) {
        str[i] = s.toCharArray()[i];
      }
      return new String(str) + "...";
    }
  }
  
  
  public void deleteTweet(Status status) {
    // TODO Auto-generated method stub
    try {
      twitter.destroyStatus(status.getId());
      System.out.println("Tweet deleted: " + status.getText());
    } catch (TwitterException e) {
      System.out.println("Tweet not found");
      e.printStackTrace();
    }
  }
  
  public List<Status> getQuerySimilarTweets(String [] keywords, TwitterUser user, BaseHandler b, Connection con) throws TwitterException{
    List<Status> allTweets = getTweets(TWEETS.MYTWEETS);
    List<Status> sortedTweets = new ArrayList<Status>();
    BigDecimal frequency = new BigDecimal("0");
    HashMap<String, BigDecimal> stabMap = new HashMap<String, BigDecimal>();
    ArrayList<Data<Status>> dataSet = new ArrayList<Data<Status>>();
    for (String keyword: keywords) {
      try {
        WordNode word = b.getWord(keyword, con);
        frequency = frequency.add(new BigDecimal(word.getFrequnecy()));
        stabMap.put(word.getText(), new BigDecimal(word.getFrequnecy()));
      } catch (WordNotFoundException e) {
        System.out.println("Word not Found");
      }
    }
    for (String keyword: stabMap.keySet()) {
      stabMap.put(keyword, frequency.divide(stabMap.get(keyword)));
    }
    for (Status status: allTweets) {
      Stack<String> text = TweetBase.extractTweet(status.getText());
      Data<Status> data;
      BigDecimal value = new BigDecimal("0");
      for (String word: text) {
        if (stabMap.containsKey(word)) {
          value = value.add(stabMap.get(word));
        }
      }
      if (value.equals(new BigDecimal("0"))) {
        data = new Data<Status>(status, value);
        dataSet.add(data);
      }
      Collections.sort(dataSet);
      for (Data<Status> d: dataSet) {
        sortedTweets.add(d.getData());
      }
    }
    return sortedTweets;
  }
  
  public void clearTweets() {
    for (Status s: testTweets) {
      deleteTweet(s);
    }
  }
}
