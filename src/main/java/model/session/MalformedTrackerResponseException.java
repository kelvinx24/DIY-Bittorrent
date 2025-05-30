package model.session;

/**
 * Exception thrown when {@link TrackerResponse} from a tracker is malformed.
 *
 * @author KX
 */
public class MalformedTrackerResponseException extends Exception {
  /**
   * Constructs a new model.session.MalformedTrackerResponseException with the specified detail message.
   *
   * @param message the detail message
   */
  public MalformedTrackerResponseException(String message) {
    super(message);
  }
}