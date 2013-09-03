package net.classicube.launcher;

import java.util.Comparator;

class UserAccountDateComparator implements Comparator<UserAccount> {

    private UserAccountDateComparator() {
    }

    @Override
    public int compare(UserAccount o1, UserAccount o2) {
        Long delta = o2.SignInDate.getTime() - o1.SignInDate.getTime();
        return delta.intValue();
    }
    public static final UserAccountDateComparator instance = new UserAccountDateComparator();
}