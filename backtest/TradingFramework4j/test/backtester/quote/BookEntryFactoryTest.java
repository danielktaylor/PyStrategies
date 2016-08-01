package backtester.quote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.Date;

import org.junit.Test;

import backtester.quote.Ask;
import backtester.quote.Bid;
import backtester.quote.BookEntry;
import backtester.quote.BookEntryFactory;
import backtester.simulation.SimulationClOrdId;
import backtester.trade.ClOrdId;




public class BookEntryFactoryTest {
	private static final Object DEFAULT_UNIQUE_ID = "uniqueId";
	private static final String DEFAULT_SYMBOL = "ZVZZT";
	private static final int DEFAULT_SIZE = 10;
	private static final BigDecimal DEFAULT_PRICE = new BigDecimal(2.50);
	private static final Date DEFAULT_DATE = new Date();
	private static final int DEFAULT_ORIGINAL_SIZE = 20;
	private static final ClOrdId DEFAULT_CLORDID = new SimulationClOrdId(0L);

	@Test
	public void creatingActualAskWithoutSpecifyingOriginalSizeShouldSetOriginalSizeToSize() {
		final Ask ask = BookEntryFactory.createActualAsk(DEFAULT_UNIQUE_ID, DEFAULT_SYMBOL, DEFAULT_SIZE, DEFAULT_PRICE, DEFAULT_DATE);
		assertEquals("Expected ask size to equal ask original quantity.", ask.getRemainingQuantity(), ask.getOriginalQuantity());
	}

	@Test
	public void creatingActualBidWithoutSpecifyingOriginalQuantityShouldSetOriginalQuantityToSize() {
		final Bid bid = BookEntryFactory.createActualBid(DEFAULT_UNIQUE_ID, DEFAULT_SYMBOL, DEFAULT_SIZE, DEFAULT_PRICE, DEFAULT_DATE);
		assertEquals("Expected bid size to equal bid original quantity.", bid.getRemainingQuantity(), bid.getOriginalQuantity());
	}

	@Test
	public void creatingActualAskShouldCreateNonsimulatedBookEntry() {
		final Ask ask = BookEntryFactory.createActualAsk(DEFAULT_UNIQUE_ID, DEFAULT_SYMBOL, DEFAULT_SIZE, DEFAULT_PRICE, DEFAULT_DATE);
		assertTrue("Expected isSimulated property to be false.", !ask.isSimulated());
	}

	@Test
	public void creatingActualBidShouldCreateNonsimulatedBookEntry() {
		final Bid bid = BookEntryFactory.createActualBid(DEFAULT_UNIQUE_ID, DEFAULT_SYMBOL, DEFAULT_SIZE, DEFAULT_PRICE, DEFAULT_DATE);
		assertTrue("Expected isSimulated property to be false.", !bid.isSimulated());
	}

	@Test
	public void creatingActualAskBySpecifyingOriginalQuantityShouldSetOriginalQuantity() {
		final Ask ask = BookEntryFactory.createActualAsk(DEFAULT_UNIQUE_ID, DEFAULT_SYMBOL, DEFAULT_SIZE, DEFAULT_PRICE, DEFAULT_DATE,
				DEFAULT_ORIGINAL_SIZE);
		assertEquals("Expected originalQuantity property to match provided original quantity argument.", DEFAULT_ORIGINAL_SIZE, ask
				.getOriginalQuantity());
	}

	@Test
	public void creatingActualBidBySpecifyingOriginalQuantityShouldSetOriginalQuantity() {
		final Bid bid = BookEntryFactory.createActualBid(DEFAULT_UNIQUE_ID, DEFAULT_SYMBOL, DEFAULT_SIZE, DEFAULT_PRICE, DEFAULT_DATE,
				DEFAULT_ORIGINAL_SIZE);
		assertEquals("Expected originalQuantity property to match provided original quantity argument.", DEFAULT_ORIGINAL_SIZE, bid
				.getOriginalQuantity());
	}

	@Test
	public void creatingSimulatedAskShouldSetProvidedClOrdId() {
		final Ask ask = BookEntryFactory.createSimulatedAsk(DEFAULT_CLORDID, DEFAULT_UNIQUE_ID, DEFAULT_SYMBOL, DEFAULT_SIZE, DEFAULT_PRICE,
				DEFAULT_DATE);
		assertEquals("Expected ClOrdId to be " + DEFAULT_CLORDID + ".", DEFAULT_CLORDID, ask.getClOrdId());
	}

	@Test
	public void creatingSimulatedBidShouldSetProvidedClOrdId() {
		final Bid bid = BookEntryFactory.createSimulatedBid(DEFAULT_CLORDID, DEFAULT_UNIQUE_ID, DEFAULT_SYMBOL, DEFAULT_SIZE, DEFAULT_PRICE,
				DEFAULT_DATE);
		assertEquals("Expected ClOrdId to be " + DEFAULT_CLORDID + ".", DEFAULT_CLORDID, bid.getClOrdId());
	}

	@Test
	public void creatingSimulatedAskWithoutSpecifyingOriginalSizeShouldSetOriginalSizeToSize() {
		final Ask ask = BookEntryFactory.createSimulatedAsk(DEFAULT_CLORDID, DEFAULT_UNIQUE_ID, DEFAULT_SYMBOL, DEFAULT_SIZE, DEFAULT_PRICE,
				DEFAULT_DATE);
		assertEquals("Expected ask size to equal ask original quantity.", ask.getRemainingQuantity(), ask.getOriginalQuantity());
	}

	@Test
	public void creatingSimulatedBidWithoutSpecifyingOriginalQuantityShouldSetOriginalQuantityToSize() {
		final Bid bid = BookEntryFactory.createSimulatedBid(DEFAULT_CLORDID, DEFAULT_UNIQUE_ID, DEFAULT_SYMBOL, DEFAULT_SIZE, DEFAULT_PRICE,
				DEFAULT_DATE);
		assertEquals("Expected bid size to equal bid original quantity.", bid.getRemainingQuantity(), bid.getOriginalQuantity());
	}

	@Test
	public void creatingSimulatedAskShouldCreateSimulatedBookEntry() {
		final Ask ask = BookEntryFactory.createSimulatedAsk(DEFAULT_CLORDID, DEFAULT_UNIQUE_ID, DEFAULT_SYMBOL, DEFAULT_SIZE, DEFAULT_PRICE,
				DEFAULT_DATE);
		assertTrue("Expected isSimulated property to be true.", ask.isSimulated());
	}

	@Test
	public void creatingSimulatedBidShouldCreateSimulatedBookEntry() {
		final Bid bid = BookEntryFactory.createSimulatedBid(DEFAULT_CLORDID, DEFAULT_UNIQUE_ID, DEFAULT_SYMBOL, DEFAULT_SIZE, DEFAULT_PRICE,
				DEFAULT_DATE);
		assertTrue("Expected isSimulated property to be true.", bid.isSimulated());
	}

	@Test
	public void creatingSimulatedAskBySpecifyingOriginalQuantityShouldSetOriginalQuantity() {
		final Ask ask = BookEntryFactory.createSimulatedAsk(DEFAULT_CLORDID, DEFAULT_UNIQUE_ID, DEFAULT_SYMBOL, DEFAULT_SIZE, DEFAULT_PRICE,
				DEFAULT_DATE, DEFAULT_ORIGINAL_SIZE);
		assertEquals("Expected originalQuantity property to match provided original quantity argument.", DEFAULT_ORIGINAL_SIZE, ask
				.getOriginalQuantity());
	}

	@Test
	public void creatingSimulatedBidBySpecifyingOriginalQuantityShouldSetOriginalQuantity() {
		final Bid bid = BookEntryFactory.createSimulatedBid(DEFAULT_CLORDID, DEFAULT_UNIQUE_ID, DEFAULT_SYMBOL, DEFAULT_SIZE, DEFAULT_PRICE,
				DEFAULT_DATE, DEFAULT_ORIGINAL_SIZE);
		assertEquals("Expected originalQuantity property to match provided original quantity argument.", DEFAULT_ORIGINAL_SIZE, bid
				.getOriginalQuantity());
	}

	@Test
	public void creatingSimulatedBidWithoutOriginalQuantityWithCreateBookEntryFactoryMethodShouldReturnBid() {
		final BookEntry bookEntry = BookEntryFactory.createSimulatedBookEntry(Bid.class, DEFAULT_CLORDID, DEFAULT_UNIQUE_ID, DEFAULT_SYMBOL,
				DEFAULT_SIZE, DEFAULT_PRICE, DEFAULT_DATE);
		assertTrue("Expected instance of Bid.", bookEntry instanceof Bid);
	}

	@Test
	public void creatingSimulatedBidWithOriginalQuantityWithCreateBookEntryFactoryMethodShouldReturnBid() {
		final BookEntry bookEntry = BookEntryFactory.createSimulatedBookEntry(Bid.class, DEFAULT_CLORDID, DEFAULT_UNIQUE_ID, DEFAULT_SYMBOL,
				DEFAULT_SIZE, DEFAULT_PRICE, DEFAULT_DATE, DEFAULT_ORIGINAL_SIZE);
		assertTrue("Expected instance of Bid.", bookEntry instanceof Bid);
	}

	@Test
	public void creatingSimulatedAskWithoutOriginalQuantityWithCreateBookEntryFactoryMethodShouldReturnAsk() {
		final BookEntry bookEntry = BookEntryFactory.createSimulatedBookEntry(Ask.class, DEFAULT_CLORDID, DEFAULT_UNIQUE_ID, DEFAULT_SYMBOL,
				DEFAULT_SIZE, DEFAULT_PRICE, DEFAULT_DATE);
		assertTrue("Expected instance of Ask.", bookEntry instanceof Ask);
	}

	@Test
	public void creatingSimulatedAskWithOriginalQuantityWithCreateBookEntryFactoryMethodShouldReturnAsk() {
		final BookEntry bookEntry = BookEntryFactory.createSimulatedBookEntry(Ask.class, DEFAULT_CLORDID, DEFAULT_UNIQUE_ID, DEFAULT_SYMBOL,
				DEFAULT_SIZE, DEFAULT_PRICE, DEFAULT_DATE, DEFAULT_ORIGINAL_SIZE);
		assertTrue("Expected instance of Ask.", bookEntry instanceof Ask);
	}

	@Test
	public void creatingActualAskWithCreateBookEntryFactoryMethodShouldReturnAsk() {
		final BookEntry bookEntry = BookEntryFactory.createActualBookEntry(Ask.class, DEFAULT_UNIQUE_ID, DEFAULT_SYMBOL, DEFAULT_SIZE,
				DEFAULT_PRICE, DEFAULT_DATE, DEFAULT_ORIGINAL_SIZE);
		assertTrue("Expected instance of Ask.", bookEntry instanceof Ask);
	}

	@Test
	public void creatingActualBidWithCreateBookEntryFactoryMethodShouldReturnBid() {
		final BookEntry bookEntry = BookEntryFactory.createActualBookEntry(Bid.class, DEFAULT_UNIQUE_ID, DEFAULT_SYMBOL, DEFAULT_SIZE,
				DEFAULT_PRICE, DEFAULT_DATE, DEFAULT_ORIGINAL_SIZE);
		assertTrue("Expected instance of Bid.", bookEntry instanceof Bid);
	}

	@Test(expected = IllegalArgumentException.class)
	public void creatingInvalidSimulatedBookEntryWithOriginalQuantityWithCreateBookEntryFactoryMethodShouldThrowException() {
		BookEntryFactory.createSimulatedBookEntry(BookEntry.class, DEFAULT_CLORDID, DEFAULT_UNIQUE_ID, DEFAULT_SYMBOL, DEFAULT_SIZE,
				DEFAULT_PRICE, DEFAULT_DATE, DEFAULT_ORIGINAL_SIZE);
	}

	@Test(expected = IllegalArgumentException.class)
	public void creatingInvalidSimulatedBookEntryWithoutOriginalQuantityWithCreateBookEntryFactoryMethodShouldThrowException() {
		BookEntryFactory.createSimulatedBookEntry(BookEntry.class, DEFAULT_CLORDID, DEFAULT_UNIQUE_ID, DEFAULT_SYMBOL, DEFAULT_SIZE,
				DEFAULT_PRICE, DEFAULT_DATE);
	}

	@Test(expected = IllegalArgumentException.class)
	public void creatingInvalidActualBookEntryWithCreateBookEntryFactoryMethodShouldThrowException() {
		BookEntryFactory.createActualBookEntry(BookEntry.class, DEFAULT_UNIQUE_ID, DEFAULT_SYMBOL, DEFAULT_SIZE, DEFAULT_PRICE, DEFAULT_DATE,
				DEFAULT_ORIGINAL_SIZE);
	}
}
