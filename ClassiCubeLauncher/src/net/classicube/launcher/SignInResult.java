package net.classicube.launcher;

// Possible "expected" outcomes of a sign-in process.
// For any unexpected ones, use SignInException.
enum SignInResult {
    SUCCESS,
    WRONG_USER_OR_PASS,
    MIGRATED_ACCOUNT
}
