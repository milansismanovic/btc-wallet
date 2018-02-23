package store.bitcoin;

public class BlockStoreException extends Exception {
 	private static final long serialVersionUID = 4161024895620033350L;

	public BlockStoreException(String message) {
        super(message);
    }

    public BlockStoreException(Throwable t) {
        super(t);
    }

    public BlockStoreException(String message, Throwable t) {
        super(message, t);
    }
}
