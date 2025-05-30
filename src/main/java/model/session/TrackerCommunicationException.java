package model.session;

/**
 * Exception class for handling tracker communication errors. Used in
 * {@link TrackerClient#requestTracker()}
 *
 * @author KX
 */
public class TrackerCommunicationException extends Exception {

  /**
   * Constructs a new model.session.TrackerCommunicationException with the specified detail message.
   *
   * @param message the detail message, which is saved for later retrieval by the
   *                {@link #getMessage()} method
   */
  public TrackerCommunicationException(String message) {
    super(message);
  }
}