package uk.gov.dwp.uc.pairtest;

import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

public class TicketServiceImpl implements TicketService {

    private static final int CHILD_PRICE = 15;
    private static final int ADULT_PRICE = 25;

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
        validateTicketTypeRequest(ticketTypeRequests);

        makePaymentRequest(accountId, ticketTypeRequests);
        allocateSeats(accountId, ticketTypeRequests);
    }

    private void validateAccountId(Long accountId) {
        if (accountId == null || accountId <= 0L) {
            throw new InvalidPurchaseException();
        }
    }

    private void validateTicketTypeRequest(TicketTypeRequest[] ticketTypeRequests) {
        boolean adultRequested = false;
        int ticketTotal = 0;

        for (TicketTypeRequest request : ticketTypeRequests) {
            ticketTotal += request.getNoOfTickets();
            if (TicketTypeRequest.Type.ADULT.equals(request.getTicketType()) && request.getNoOfTickets() > 0) {
                adultRequested = true;
            }
        }

        if (!adultRequested || ticketTotal > 25) {
            throw new InvalidPurchaseException();
        }
    }

    private void makePaymentRequest(long accountId, TicketTypeRequest[] ticketTypeRequests) {
        int adultTickets = 0;
        int childTickets = 0;

        for (TicketTypeRequest request : ticketTypeRequests) {
            switch (request.getTicketType()) {
                case ADULT -> adultTickets += request.getNoOfTickets();
                case CHILD -> childTickets += request.getNoOfTickets();
                case null, default -> {
                }
            }
        }

        int adultTotal = adultTickets * ADULT_PRICE;
        int childTotal = childTickets * CHILD_PRICE;
        int totalAmountToPay = adultTotal + childTotal;
        ticketPaymentService.makePayment(accountId, totalAmountToPay);
    }

    private void allocateSeats(long accountId, TicketTypeRequest[] ticketTypeRequests) {
        int adultTickets = 0;
        int childTickets = 0;

        for (TicketTypeRequest request : ticketTypeRequests) {
            switch (request.getTicketType()) {
                case ADULT -> adultTickets += request.getNoOfTickets();
                case CHILD -> childTickets += request.getNoOfTickets();
                case null, default -> {
                }
            }
        }

        seatReservationService.reserveSeat(accountId, adultTickets + childTickets);
    }
}
