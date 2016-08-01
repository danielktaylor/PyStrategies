package backtester.quote;

public enum BookEntryType {
	ASK(Ask.class), BID(Bid.class);

	private final Class<? extends BookEntry> bookEntryClazz;

	BookEntryType(final Class<? extends BookEntry> bookEntryClazz) {
		this.bookEntryClazz = bookEntryClazz;
	}

	public Class<? extends BookEntry> getBookEntryClass() {
		return bookEntryClazz;
	}
}
