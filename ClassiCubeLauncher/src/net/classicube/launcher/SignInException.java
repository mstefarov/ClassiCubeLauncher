package net.classicube.launcher;

// Very basic exception, for GameService implementations to throw.
// Made this into a separate class just to simplify catching.
public class SignInException extends Exception {
    public SignInException(String message){
        super(message);
    }
    private static final long serialVersionUID = 1L;
}
