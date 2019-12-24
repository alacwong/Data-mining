package Tree;

import java.math.BigDecimal;

public class Data<T> implements Comparable<Data<T>>{
  
  private T data;
  private BigDecimal value;
  
  public Data(T data, BigDecimal value) {
    this.data = data;
    this.value = value;
  }
  
  public T getData() {
    return data;
  }
  
  public BigDecimal getValue() {
    return value;
  }

  @Override
  public int compareTo(Data<T> o) {
    return value.compareTo(o.getValue());
  }
  
  @Override
  public String toString() {
    return "Data: " + data.toString() + " Value: " + value.toString();
  }

}
