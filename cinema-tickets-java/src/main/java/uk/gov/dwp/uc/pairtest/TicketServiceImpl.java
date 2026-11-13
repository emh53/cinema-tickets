package uk.gov.dwp.uc.pairtest;

import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

import java.util.EnumMap;
import java.util.Map;

public class TicketServiceImpl implements TicketService {

    private static final int CHILD_PRICE = 15;
    private static final int ADULT_PRICE = 25;
    private static final int MAX_TICKETS = 25;

    private final TicketPaymentService ticketPaymentService;
    private final SeatReservationService seatReservationService;

    public TicketServiceImpl(TicketPaymentService ticketPaymentService,
                             SeatReservationService seatReservationService) {
        this.ticketPaymentService = ticketPaymentService;
        this.seatReservationService = seatReservationService;
    }

    /**
     * Should only have private methods other than the one below.
     */

    @Override
    public void purchaseTickets(Long accountId, TicketTypeRequest... ticketTypeRequests) throws InvalidPurchaseException {
        validateAccountId(accountId);

        TicketCount ticketCount = getTicketCounts(ticketTypeRequests);
        validateTicketsRequested(ticketCount);

        makePaymentRequest(accountId, ticketCount);
        allocateSeats(accountId, ticketCount);
    }

    private void validateAccountId(Long accountId) {
        if (accountId == null || accountId <= 0L) {
            throw new InvalidPurchaseException();
        }
    }

    private static TicketCount getTicketCounts(TicketTypeRequest[] ticketTypeRequests) {
        Map<TicketTypeRequest.Type, Integer> totals = new EnumMap<>(TicketTypeRequest.Type.class);
        for (TicketTypeRequest request : ticketTypeRequests) {
            totals.merge(request.type(), request.noOfTickets(), Integer::sum);
        }

        return new TicketCount(totals);
    }

    private void validateTicketsRequested(TicketCount ticketCount) {
        int totalTickets = ticketCount.getAdults() + ticketCount.getChildren() + ticketCount.getInfants();
        if (ticketCount.getAdults() < 1
            || ticketCount.getChildren() < 0
            || ticketCount.getInfants() < 0
            || totalTickets > MAX_TICKETS
            || ticketCount.getAdults() < ticketCount.getInfants()) {
            throw new InvalidPurchaseException();
        }
    }

    private void makePaymentRequest(long accountId, TicketCount ticketCount) {
        int adultTotal = ticketCount.getAdults() * ADULT_PRICE;
        int childTotal = ticketCount.getChildren() * CHILD_PRICE;
        int totalAmountToPay = adultTotal + childTotal;
        ticketPaymentService.makePayment(accountId, totalAmountToPay);
    }

    private void allocateSeats(long accountId, TicketCount ticketCount) {
        int totalSeatsToAllocate = ticketCount.getAdults() + ticketCount.getChildren();
        seatReservationService.reserveSeat(accountId, totalSeatsToAllocate);
    }

    private static class TicketCount {
        private final int adults;
        private final int children;
        private final int infants;

        TicketCount(Map<TicketTypeRequest.Type, Integer> totals) {
            this.adults = totals.getOrDefault(TicketTypeRequest.Type.ADULT, 0);
            this.children = totals.getOrDefault(TicketTypeRequest.Type.CHILD, 0);
            this.infants = totals.getOrDefault(TicketTypeRequest.Type.INFANT, 0);
        }

        int getAdults() {
            return adults;
        }

        int getChildren() {
            return children;
        }

        int getInfants() {
            return infants;
        }
    }
}
