package backtester.quote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;

import org.junit.Test;

import backtester.quote.Ask;
import backtester.quote.Bid;


public class BookEntryTest {
	private static int idCounter = 0;
	private static final String DEFAULT_SYMBOL = "AAPL";
	private static final int DEFAULT_QUANTITY = 100;
	private static final BigDecimal DEFAULT_PRICE = new BigDecimal(200.00);
	private static final Date DEFAULT_DATE = new Date();

	private Ask createAnonymousAsk() {
		return new Ask(idCounter++, DEFAULT_SYMBOL, DEFAULT_QUANTITY, DEFAULT_PRICE, DEFAULT_DATE, false, DEFAULT_QUANTITY);
	}

	private Ask createAnonymousAsk(final Date timestamp) {
		return new Ask(idCounter++, DEFAULT_SYMBOL, DEFAULT_QUANTITY, DEFAULT_PRICE, timestamp, false, DEFAULT_QUANTITY);
	}

	private Ask createAnonymousAsk(final BigDecimal price) {
		return new Ask(idCounter++, DEFAULT_SYMBOL, DEFAULT_QUANTITY, price, DEFAULT_DATE, false, DEFAULT_QUANTITY);
	}

	private Bid createAnonymousBid() {
		return new Bid(idCounter++, DEFAULT_SYMBOL, DEFAULT_QUANTITY, DEFAULT_PRICE, DEFAULT_DATE, false, DEFAULT_QUANTITY);
	}

	private Bid createAnonymousBid(final Date timestamp) {
		return new Bid(idCounter++, DEFAULT_SYMBOL, DEFAULT_QUANTITY, DEFAULT_PRICE, timestamp, false, DEFAULT_QUANTITY);
	}

	private Bid createAnonymousBid(final BigDecimal price) {
		return new Bid(idCounter++, DEFAULT_SYMBOL, DEFAULT_QUANTITY, price, DEFAULT_DATE, false, DEFAULT_QUANTITY);
	}

	@Test
	public void creatingAskWithZeroPriceShouldCreateMarketOrder() {
		final Ask ask = createAnonymousAsk(new BigDecimal(0));
		assertTrue("Expected isMarketOrder property to be true.", ask.isMarketOrder());
	}

	@Test
	public void creatingAskWithNonzeroPriceShouldCreateLimitOrder() {
		final Ask ask = createAnonymousAsk(new BigDecimal(1));
		assertTrue("Expected isMarketOrder property to be false.", !ask.isMarketOrder());
	}

	@Test
	public void creatingBidWithZeroPriceShouldCreateMarketOrder() {
		final Bid bid = createAnonymousBid(new BigDecimal(0));
		assertTrue("Expected isMarketOrder property to be true.", bid.isMarketOrder());
	}

	@Test
	public void creatingBidWithNonzeroPriceShouldCreateLimitOrder() {
		final Bid bid = createAnonymousBid(new BigDecimal(1));
		assertTrue("Expected isMarketOrder property to be false.", !bid.isMarketOrder());
	}

	@Test
	public void testQuoteBookEntryEquals() {
		final Bid bid0 = new Bid("2838535", "X", 100, DEFAULT_PRICE, new Date(34216598), false, DEFAULT_QUANTITY);
		final Bid bid0Copy = new Bid("2838535", "X", 0, DEFAULT_PRICE, new Date(34216653), false, DEFAULT_QUANTITY);

		assertEquals(bid0, bid0Copy);

		assertEquals(bid0.compareTo(bid0Copy), 0);
	}

	@Test
	public void compareAsksWithDifferentPricesAndSameTimestamps() {
		final Ask firstAsk = createAnonymousAsk();
		final Ask secondAsk = createAnonymousAsk(new BigDecimal(150.00));
		assertEquals(1, firstAsk.compareTo(secondAsk));
	}

	@Test
	public void compareAsksWithSamePricesAndSameTimestamps() {
		final Ask firstAsk = createAnonymousAsk();
		final Ask secondAsk = createAnonymousAsk();
		assertEquals(0, firstAsk.compareTo(secondAsk));
	}

	@Test
	public void compareAsksWithSamePricesAndDifferentTimesamps() {
		final Ask firstAsk = createAnonymousAsk();
		final Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) - 1);
		final Ask secondAsk = createAnonymousAsk(calendar.getTime());
		assertEquals(1, firstAsk.compareTo(secondAsk));
	}

	@Test
	public void compareBidsWithDifferentPricesAndSameTimestamps() {
		final Bid firstBid = createAnonymousBid();
		final Bid secondBid = createAnonymousBid(new BigDecimal(150.00));
		assertEquals(-1, firstBid.compareTo(secondBid));
	}

	@Test
	public void compareBidsWithSamePricesAndSameTimestamps() {
		final Bid firstBid = createAnonymousBid();
		final Bid secondBid = createAnonymousBid();
		assertEquals(0, firstBid.compareTo(secondBid));
	}

	@Test
	public void compareBidsWithSamePricesAndDifferentTimesamps() {
		final Bid firstBid = createAnonymousBid();
		final Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) - 1);
		final Bid secondBid = createAnonymousBid(calendar.getTime());
		assertEquals(1, firstBid.compareTo(secondBid));
	}

	@Test
	public void equateIdenticalBids() {
		final Bid firstBid = createAnonymousBid();
		final Bid secondBid = new Bid(firstBid);
		assertTrue(firstBid.equals(secondBid));
	}

	@Test
	public void equateIdenticalAsks() {
		final Ask firstAsk = createAnonymousAsk();
		final Ask secondAsk = new Ask(firstAsk);
		assertTrue(firstAsk.equals(secondAsk));
	}

	@Test
	public void equateBidsWithDifferentIDs() {
		final Bid firstBid = createAnonymousBid();
		final Bid secondBid = createAnonymousBid();
		assertFalse(firstBid.equals(secondBid));
	}

	@Test
	public void equateBidWithAsk() {
		final Bid aBid = createAnonymousBid();
		final Ask anAsk = createAnonymousAsk();
		assertFalse(anAsk.equals(aBid));
	}

	@Test
	public void equateAsksWithDifferentIDs() {
		final Ask firstAsk = createAnonymousAsk();
		final Ask secondAsk = createAnonymousAsk();
		assertFalse(firstAsk.equals(secondAsk));
	}

	@Test(expected = IllegalArgumentException.class)
	public void createAskWithNegativeQuantity() {
		new Ask(idCounter++, DEFAULT_SYMBOL, -1, DEFAULT_PRICE, DEFAULT_DATE, false, DEFAULT_QUANTITY);
	}

	@Test(expected = IllegalArgumentException.class)
	public void createBidWithNegativeQuantity() {
		new Bid(idCounter++, DEFAULT_SYMBOL, -1, DEFAULT_PRICE, DEFAULT_DATE, false, DEFAULT_QUANTITY);
	}

	@Test(expected = IllegalArgumentException.class)
	public void createAskWithNegativePrice() {
		new Ask(idCounter++, DEFAULT_SYMBOL, DEFAULT_QUANTITY, new BigDecimal(-5.00), DEFAULT_DATE, false, DEFAULT_QUANTITY);
	}

	@Test(expected = IllegalArgumentException.class)
	public void createBidWithNegativePrice() {
		new Bid(idCounter++, DEFAULT_SYMBOL, DEFAULT_QUANTITY, new BigDecimal(-5.00), DEFAULT_DATE, false, DEFAULT_QUANTITY);
	}
}
