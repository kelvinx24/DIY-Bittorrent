package model.session;

/**
 * Exception class for handling errors during piece download operations including, hash mismatches,
 * invalid blocks, and other download-related issues Used in
 * {@link PeerSession#downloadPiece(int, int, byte[], int)}
 *
 * @author KX
 */
public class PieceDownloadException extends Exception {

  /**
   * Constructs a new model.session.PieceDownloadException with the specified detail message.
   *
   * @param message the detail message, which is saved for later retrieval by the
   *                {@link #getMessage()} method
   */
  public PieceDownloadException(String message) {
    super(message);
  }

  /**
   * Constructs a new model.session.PieceDownloadException with the specified detail message and cause.
   *
   * @param message the detail message, which is saved for later retrieval by the
   *                {@link #getMessage()} method
   * @param cause   the cause of the exception, which is saved for later retrieval by the
   *                {@link #getCause()} method
   */
  public PieceDownloadException(String message, Throwable cause) {
    super(message, cause);
  }

}
