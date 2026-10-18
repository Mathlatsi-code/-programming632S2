package pims;

/** Small id/label holder used in combo boxes. */
public final class Item {
    public final int id;
    public final String label;

    public Item(int id, String label) { this.id = id; this.label = label; }

    @Override public String toString() { return label; }
}
