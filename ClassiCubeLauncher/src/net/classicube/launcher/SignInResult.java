package net.classicube.launcher;

// Possible "expected" outcomes of a sign-in process.
// For any unexpected ones, use SignInException.
public enum SignInResult {

    SUCCESS,
    WRONG_USER_OR_PASS,
    MIGRATED_ACCOUNT,
    CONNECTION_ERROR,
    CHALLENGE_FAILED,
    EMAIL_UNACCEPTABLE;

    public static String getMessage(SignInResult result) {
        switch (result) {
            case WRONG_USER_OR_PASS:
                return "Wrong username or password.";
            case MIGRATED_ACCOUNT:
                return "Your account has been migrated. "
                        + "Use your Mojang account (email) to sign in.";
            case CONNECTION_ERROR:
                return "Connection problem. The website may be down.";
            case CHALLENGE_FAILED:
                return "Wrong answer to the security question. "
                        + "Try again, or reset the security question at "
                        + "<a href=\"https://account.mojang.com/me/changeSecretQuestions\">Mojang.com</a>";
            case EMAIL_UNACCEPTABLE:
                return "Cannot sign in to ClassiCube.net with an email address.<br><br>"
                        + "If you are trying to play on ClassiCube.net servers: "
                        + "Please use your ClassiCube username to sign in, instead of your email address. "
                        + "Your Mojang account won't work -- you have to "
                        + "<a href=\"http://www.classicube.net/acc/register/\">register a ClassiCube.net account</a>, "
                        + "if you haven't done so already.<br><br>"
                        + "If you are trying to play on Minecraft.net's Classic servers: "
                        + "Click [Switch to Minecraft.net] and then use your Minecraft username or Mojang account.";
            default:
                return result.name();
        }
    }
}
