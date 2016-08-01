package backtester.quote;

import java.math.BigDecimal;
import java.util.Date;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import backtester.quote.Ask;
import backtester.quote.Bid;
import backtester.quote.BookEntryFactory;
import backtester.quote.QuoteBook;


public class QuoteBookTest {
	private static final String TEST_SYMBOL = "ZVZZT";
	private QuoteBook book;

	@Before
	public void runfirst() {
		book = new QuoteBook();
	}

	@Test
	public void testSpreadSimple() {
		final BigDecimal askPrice = new BigDecimal(15);
		final int size = 100;
		book.addAsk(new Ask(0, TEST_SYMBOL, size, askPrice, new Date(), false, size));

		final BigDecimal bidPrice = new BigDecimal(10);
		book.addBid(new Bid(0, TEST_SYMBOL, size, bidPrice, new Date(), false, size));

		Assert.assertEquals(askPrice.subtract(bidPrice), book.getSpread());
	}

	@Test
	public void testSpreadComplex() {
		book.addAsk(new Ask(0, TEST_SYMBOL, 100, new BigDecimal(15), new Date(), false, 100));
		book.addAsk(new Ask(1, TEST_SYMBOL, 100, new BigDecimal(25), new Date(), false, 100));
		book.addAsk(new Ask(2, TEST_SYMBOL, 100, new BigDecimal(13), new Date(), false, 100));

		book.addBid(new Bid(0, TEST_SYMBOL, 100, new BigDecimal(10), new Date(), false, 100));
		book.addBid(new Bid(1, TEST_SYMBOL, 100, new BigDecimal(11), new Date(), false, 100));
		book.addBid(new Bid(2, TEST_SYMBOL, 100, new BigDecimal(8), new Date(), false, 100));

		Assert.assertEquals(new BigDecimal(2), book.getSpread());
	}

	@Test
	public void testOrdering() {
		final int size = 100;
		final int newSize = 0;
		final Bid bid0 = new Bid(0, TEST_SYMBOL, size, new BigDecimal(15), new Date(), false, size);
		final Bid bid1 = new Bid(1, TEST_SYMBOL, size, new BigDecimal(14), new Date(), false, size);
		final Bid bid2 = new Bid(2, TEST_SYMBOL, size, new BigDecimal(13), new Date(), false, size);
		final Bid bid3 = new Bid(3, TEST_SYMBOL, size, new BigDecimal(12), new Date(), false, size);
		final Bid bid4 = new Bid(0, TEST_SYMBOL, newSize, new BigDecimal(15), new Date(), false, 100);

		final Ask ask0 = new Ask(0, TEST_SYMBOL, size, new BigDecimal(23), new Date(), false, size);
		final Ask ask1 = new Ask(1, TEST_SYMBOL, size, new BigDecimal(24), new Date(), false, size);
		final Ask ask2 = new Ask(2, TEST_SYMBOL, size, new BigDecimal(25), new Date(), false, size);
		final Ask ask3 = new Ask(3, TEST_SYMBOL, size, new BigDecimal(26), new Date(), false, size);
		final Ask ask4 = new Ask(0, TEST_SYMBOL, newSize, new BigDecimal(23), new Date(), false, size);

		book.addBid(bid0);
		book.addBid(bid1);
		book.addBid(bid2);
		book.addBid(bid3);
		book.addBid(bid4);

		book.addAsk(ask0);
		book.addAsk(ask1);
		book.addAsk(ask2);
		book.addAsk(ask3);
		book.addAsk(ask4);

		Assert.assertEquals(book.getTopOfBook().getBid().getPrice(), new BigDecimal(14));
		Assert.assertEquals(book.getTopOfBook().getAsk().getPrice(), new BigDecimal(24));
	}

	@Test
	public void testBidChanging() {
		final int size = 100;
		final Bid bid0 = new Bid(0, TEST_SYMBOL, size, new BigDecimal(15), new Date(), false, size);
		final Bid bid0Changed = new Bid(0, TEST_SYMBOL, size, new BigDecimal(14), new Date(), false, size);

		book.addBid(bid0);
		Assert.assertEquals(1, book.getBids().size());

		book.addBid(bid0Changed);
		Assert.assertEquals(1, book.getBids().size());

		final Bid retrieved = book.getBids().get(0);
		Assert.assertEquals(new BigDecimal(14), retrieved.getPrice());
	}

	@Test
	public void testBidRemoval() {
		int size = 100;
		final Bid bid0 = new Bid(0, TEST_SYMBOL, size, new BigDecimal(15), new Date(), false, size);
		size = 0;
		final Bid bid0ZeroQty = new Bid(0, TEST_SYMBOL, size, new BigDecimal(14), new Date(), false, size);

		book.addBid(bid0);
		Assert.assertEquals(1, book.getBids().size());

		book.addBid(bid0ZeroQty);
		Assert.assertEquals(0, book.getBids().size());
	}

	@Test
	public void testAskChanging() {
		final int size = 100;
		final Ask ask0 = new Ask(0, TEST_SYMBOL, size, new BigDecimal(15), new Date(), false, size);
		final Ask ask0Changed = new Ask(0, TEST_SYMBOL, size, new BigDecimal(14), new Date(), false, size);

		book.addAsk(ask0);
		Assert.assertEquals(1, book.getAsks().size());

		book.addAsk(ask0Changed);
		Assert.assertEquals(1, book.getAsks().size());

		final Ask retrieved = book.getAsks().get(0);
		Assert.assertEquals(new BigDecimal(14), retrieved.getPrice());
	}

	@Test
	public void testAskRemoval() {
		int size = 100;
		final Ask ask0 = new Ask(0, TEST_SYMBOL, size, new BigDecimal(15), new Date(), false, size);
		size = 0;
		final Ask ask0ZeroQty = new Ask(0, TEST_SYMBOL, size, new BigDecimal(14), new Date(), false, size);

		book.addAsk(ask0);
		Assert.assertEquals(1, book.getAsks().size());

		book.addAsk(ask0ZeroQty);
		Assert.assertEquals(0, book.getAsks().size());
	}

	@Test
	public void testEqualDatesForAsks() {
		final Date date1 = new Date();
		final int size = 100;
		final Ask ask0 = new Ask(0, TEST_SYMBOL, size, new BigDecimal(15), date1, false, size);
		final Ask ask1 = new Ask(1, TEST_SYMBOL, size, new BigDecimal(15), date1, false, size);

		book.addAsk(ask0);
		Assert.assertEquals(1, book.getAsks().size());

		book.addAsk(ask1);
		Assert.assertEquals(2, book.getAsks().size());
	}

	@Test
	public void testEqualDatesForBids() {
		final Date date1 = new Date();
		final int size = 100;
		final Bid bid0 = new Bid(0, TEST_SYMBOL, size, new BigDecimal(15), date1, false, size);
		final Bid bid1 = new Bid(1, TEST_SYMBOL, size, new BigDecimal(15), date1, false, size);

		book.addBid(bid0);
		Assert.assertEquals(1, book.getBids().size());

		book.addBid(bid1);
		Assert.assertEquals(2, book.getBids().size());
	}

	@Test
	public void testChangingQtyPreservesBidOrder() {
		final Date date1 = new Date();
		int size = 100;
		final Bid bid0 = new Bid(0, TEST_SYMBOL, size, new BigDecimal(15), date1, false, size);
		final Bid bid1 = new Bid(1, TEST_SYMBOL, size, new BigDecimal(15), date1, false, size);

		book.addBid(bid0);
		Assert.assertEquals(0, book.getBids().get(0).getId());

		book.addBid(bid1);
		Assert.assertEquals(0, book.getBids().get(0).getId());

		size = 50;
		final Bid bid0LowerQty = new Bid(0, TEST_SYMBOL, size, new BigDecimal(15), date1, false, size);
		book.addBid(bid0LowerQty);
		Assert.assertEquals(0, book.getBids().get(0).getId());

		size = 100;
		final Bid bid0HigherQty = new Bid(0, TEST_SYMBOL, size, new BigDecimal(15), date1, false, size);
		book.addBid(bid0HigherQty);
		Assert.assertEquals(0, book.getBids().get(0).getId());
	}

	@Test
	public void testRemovingAndAddingBidOrder() {
		final Date date1 = new Date();
		int size = 100;
		final Bid bid0 = new Bid(0, TEST_SYMBOL, size, new BigDecimal(15), date1, false, size);
		final Bid bid1 = new Bid(1, TEST_SYMBOL, size, new BigDecimal(15), date1, false, size);

		book.addBid(bid0);
		Assert.assertEquals(0, book.getBids().get(0).getId());

		book.addBid(bid1);
		Assert.assertEquals(0, book.getBids().get(0).getId());

		size = 0;
		final Bid bid0Remove = new Bid(0, TEST_SYMBOL, size, new BigDecimal(15), date1, false, size);
		book.addBid(bid0Remove);
		Assert.assertEquals(1, book.getBids().get(0).getId());

		// Adding back should put it at the end of the line
		size = 100;
		final Bid bid0AddBack = new Bid(0, TEST_SYMBOL, size, new BigDecimal(15), date1, false, size);
		book.addBid(bid0AddBack);
		Assert.assertEquals(1, book.getBids().get(0).getId());
	}

	@Test
	public void testChangingQtyPreservesAskOrder() {
		final Date date1 = new Date();
		int size = 100;
		final Ask ask0 = new Ask(0, TEST_SYMBOL, size, new BigDecimal(15), date1, false, size);
		final Ask ask1 = new Ask(1, TEST_SYMBOL, size, new BigDecimal(15), date1, false, size);

		book.addAsk(ask0);
		Assert.assertEquals(0, book.getAsks().get(0).getId());

		book.addAsk(ask1);
		Assert.assertEquals(0, book.getAsks().get(0).getId());

		size = 50;
		final Ask ask0LowerQty = new Ask(0, TEST_SYMBOL, size, new BigDecimal(15), date1, false, size);
		book.addAsk(ask0LowerQty);
		Assert.assertEquals(0, book.getAsks().get(0).getId());

		size = 100;
		final Ask ask0HigherQty = new Ask(0, TEST_SYMBOL, size, new BigDecimal(15), date1, false, size);
		book.addAsk(ask0HigherQty);
		Assert.assertEquals(0, book.getAsks().get(0).getId());
	}

	@Test
	public void testUpdatingOrderSetsVolume() {
		final Date date1 = new Date();
		int size = 100;
		final Bid bid0 = new Bid(0, TEST_SYMBOL, size, new BigDecimal(15), date1, false, size);
		final Bid bid1 = new Bid(1, TEST_SYMBOL, size, new BigDecimal(15), date1, false, size);

		book.addBid(bid0);
		book.addBid(bid1);
		Assert.assertEquals(size*2, book.getBidVolume());
		
		// Fill 1
		final Bid updatedBid0 = new Bid(0, TEST_SYMBOL, 13, new BigDecimal(15), date1, false, size);
		book.addBid(updatedBid0);
		Assert.assertEquals(113, book.getBidVolume());
		
		// Fill 2
		final Bid updatedBid1 = new Bid(0, TEST_SYMBOL, 11, new BigDecimal(15), date1, false, size); // Should this last parameter be `size` or 13?
		book.addBid(updatedBid1);
		Assert.assertEquals(111, book.getBidVolume());
	}
	
	@Test
	public void testRemovingAndAddingAskOrder() {
		final Date date1 = new Date();
		int size = 100;
		final Ask ask0 = new Ask(0, TEST_SYMBOL, size, new BigDecimal(15), date1, false, size);
		final Ask ask1 = new Ask(1, TEST_SYMBOL, size, new BigDecimal(15), date1, false, size);

		book.addAsk(ask0);
		Assert.assertEquals(0, book.getAsks().get(0).getId());

		book.addAsk(ask1);
		Assert.assertEquals(0, book.getAsks().get(0).getId());

		size = 0;
		final Ask ask0Remove = new Ask(0, TEST_SYMBOL, size, new BigDecimal(15), date1, false, size);
		book.addAsk(ask0Remove);
		Assert.assertEquals(1, book.getAsks().get(0).getId());

		// Adding back should put it at the end of the line
		size = 100;
		final Ask ask0AddBack = new Ask(0, TEST_SYMBOL, size, new BigDecimal(15), date1, false, size);
		book.addAsk(ask0AddBack);
		Assert.assertEquals(1, book.getAsks().get(0).getId());
	}

	@Test
	public void testBidAskVolume() {
		final Date date1 = new Date();
		final Ask ask0 = new Ask(0, TEST_SYMBOL, 100, new BigDecimal(15), date1, false, 100);
		final Ask ask1 = new Ask(1, TEST_SYMBOL, 200, new BigDecimal(16), date1, false, 200);
		final Ask ask2 = new Ask(2, TEST_SYMBOL, 300, new BigDecimal(17), date1, false, 300);

		final Bid bid0 = new Bid(3, TEST_SYMBOL, 100, new BigDecimal(13), date1, false, 100);
		final Bid bid1 = new Bid(4, TEST_SYMBOL, 200, new BigDecimal(12), date1, false, 200);
		final Bid bid2 = new Bid(5, TEST_SYMBOL, 300, new BigDecimal(11), date1, false, 300);

		book.addAsk(ask0);
		book.addAsk(ask1);
		book.addAsk(ask2);

		book.addBid(bid0);
		book.addBid(bid1);
		book.addBid(bid2);

		Assert.assertEquals(600, book.getAskVolume());
		Assert.assertEquals(600, book.getBidVolume());

		final Bid bid3 = new Bid(3, TEST_SYMBOL, 50, new BigDecimal(13), date1, false, 100);
		book.addBid(bid3);
		Assert.assertEquals(550, book.getBidVolume());

		final Ask ask3 = new Ask(1, TEST_SYMBOL, 200, new BigDecimal(16.10), date1, false, 200);
		book.addAsk(ask3);
		Assert.assertEquals(600, book.getAskVolume());

		final Ask ask4 = new Ask(1, TEST_SYMBOL, 300, new BigDecimal(16.10), date1, false, 200);
		book.addAsk(ask4);
		Assert.assertEquals(700, book.getAskVolume());
	}

	@Test
	public void addingOlderAskShouldNotUpdateLastReceivedAskOrLastReceivedBidAskValues() {
	    final Ask newerAsk = BookEntryFactory.createActualAsk("1", "ZVZZT", 1, BigDecimal.ONE, new Date());
	    book.addAsk(newerAsk);

	    final Bid bid = BookEntryFactory.createActualBid("2", "ZVZZT", 1, BigDecimal.ONE, new Date());
	    book.addBid(bid);

	    final Date olderAskDate = new Date(newerAsk.getTimestamp().getTime() - 1000);
	    book.addAsk(BookEntryFactory.createActualAsk("3", "ZVZZT", 1, BigDecimal.ONE, olderAskDate));

	    Assert.assertEquals(newerAsk, book.getLastAsk());
	    Assert.assertEquals(bid, book.getLastReceivedBookEntry());
	    Assert.assertEquals(bid, book.getLastReceivedNonSimulatedBookEntry());
	}

    @Test
    public void addingOlderBidShouldNotUpdateLastReceivedBidOrLastReceivedBidAskValues() {
        final Bid newerBid = BookEntryFactory.createActualBid("1", "ZVZZT", 1, BigDecimal.ONE, new Date());
        book.addBid(newerBid);

        final Ask ask = BookEntryFactory.createActualAsk("2", "ZVZZT", 1, BigDecimal.ONE, new Date());;
        book.addAsk(ask);

        final Date olderBidDate = new Date(newerBid.getTimestamp().getTime() - 1000);
        book.addBid(BookEntryFactory.createActualBid("3", "ZVZZT", 1, BigDecimal.ONE, olderBidDate));

        Assert.assertEquals(newerBid, book.getLastBid());
        Assert.assertEquals(ask, book.getLastReceivedBookEntry());
        Assert.assertEquals(ask, book.getLastReceivedNonSimulatedBookEntry());
    }
}
