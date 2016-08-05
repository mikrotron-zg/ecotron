package org.vrspace.server;

public class ReferentialIntegrityException extends RuntimeException{
  public ReferentialIntegrityException( String description ) {
    super( description );
  }
}
