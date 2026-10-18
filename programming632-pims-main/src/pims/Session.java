package pims;

/** Holds the currently logged-in user. */
public final class Session {
    public static int userId;
    public static String username;
    public static String role;
    public static String fullName;

    private Session() { }

    public static boolean isAdmin() { return "Admin".equals(role); }

    public static void clear() { userId = 0; username = null; role = null; fullName = null; }
}
