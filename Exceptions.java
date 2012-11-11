



class FullException extends Exception {
  String mistake;
  
  public FullException() {
    super ();
    mistake = "this queue is full";
  }


  public FullException(String err) {
    super(err);
    mistake = err;
  }
  
  public String getError() {
    return mistake;
  }

}

class EmptyException extends Exception {
  String mistake;
  
  public EmptyException() {
    super ();
    mistake = "this queue is empty";
  }
  
  public EmptyException(String err) {
    super(err);
    mistake = err;
  }
  public String getError() {
    return mistake;
  }
}




